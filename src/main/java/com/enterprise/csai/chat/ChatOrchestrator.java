package com.enterprise.csai.chat;

import com.enterprise.csai.audit.AuditLogRepository;
import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.enterprise.csai.domain.policy.GuardrailPolicy;
import com.enterprise.csai.domain.policy.HandoffPolicy;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import com.enterprise.csai.knowledge.KnowledgeSearchResult;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import com.enterprise.csai.modelgateway.ModelGateway;
import com.enterprise.csai.observability.CsaiMetrics;
import com.enterprise.csai.router.RoutingDecision;
import com.enterprise.csai.router.RoutingService;
import com.enterprise.csai.security.ApiKeyPrincipal;
import com.enterprise.csai.security.CsaiPrincipalHolder;
import com.microservice.framework.web.context.RequestIdContext;
import com.microservice.framework.web.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);

    private final RoutingService routingService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ModelGateway modelGateway;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RouteLogRepository routeLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final CsaiProperties properties;
    private final GuardrailPolicy guardrailPolicy;
    private final HandoffPolicy handoffPolicy;
    private final CsaiMetrics metrics;
    private final ExecutorService streamExecutor;

    public ChatOrchestrator(
            RoutingService routingService,
            KnowledgeSearchService knowledgeSearchService,
            ModelGateway modelGateway,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            RouteLogRepository routeLogRepository,
            AuditLogRepository auditLogRepository,
            CsaiProperties properties,
            GuardrailPolicy guardrailPolicy,
            HandoffPolicy handoffPolicy,
            CsaiMetrics metrics) {
        this.routingService = routingService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.modelGateway = modelGateway;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.routeLogRepository = routeLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.properties = properties;
        this.guardrailPolicy = guardrailPolicy;
        this.handoffPolicy = handoffPolicy;
        this.metrics = metrics;
        int pool = Math.max(2, properties.getResilience().getStreamPoolSize());
        int queue = Math.max(10, properties.getResilience().getStreamQueueSize());
        this.streamExecutor = new ThreadPoolExecutor(
                Math.min(4, pool),
                pool,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queue),
                r -> {
                    Thread t = new Thread(r, "csai-chat-stream");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        long started = System.currentTimeMillis();
        ApiKeyPrincipal principal = CsaiPrincipalHolder.get();
        String ownerId = principal.id();
        List<String> degradedReasons = new ArrayList<>();
        boolean degraded = false;
        boolean handoff = false;
        String handoffReason = null;
        String status = "ok";

        try {
            if (request.message() == null || request.message().isBlank()) {
                throw new BusinessException(CsaiErrorCodes.INPUT_REJECTED, "message is blank");
            }
            if (guardrailPolicy.looksLikeInjection(request.message())) {
                ChatSessionEntity session = loadOrCreateSession(request.sessionId(), request.message(), ownerId);
                RoutingDecision route = new RoutingDecision(
                        com.enterprise.csai.router.IntentType.UNKNOWN,
                        0.0,
                        "injection_blocked",
                        properties.getRouter().getClassifierModelId(),
                        properties.getRouter().getDefaultAnswerModelId(),
                        false);
                String answer = guardrailPolicy.injectionBlockedAnswer();
                persistTurn(session, request.message(), answer, route, ownerId, false, false, started);
                metrics.recordChat("UNKNOWN", "blocked", false, false, System.currentTimeMillis() - started);
                return new ChatResponse(
                        session.getId(), answer, route, List.of(), false, List.of(), false, null);
            }

            ChatSessionEntity session = loadOrCreateSession(request.sessionId(), request.message(), ownerId);
            List<ChatMessageEntity> history = messageRepository.findRecentBySession(
                    session.getId(), properties.getChat().getHistoryMaxMessages());

            RoutingDecision decision = routingService.route(request.message());
            if ("classifier_failed".equals(decision.reason())) {
                degraded = true;
                degradedReasons.add("CLASSIFIER_FAILED");
            }

            HandoffPolicy.HandoffDecision hd = handoffPolicy.evaluate(decision, request.message());
            if (hd.handoff()) {
                handoff = true;
                handoffReason = hd.reason();
                String answer = guardrailPolicy.handoffAnswer(hd.reason());
                RoutingDecision finalDecision = new RoutingDecision(
                        decision.intent(),
                        decision.confidence(),
                        decision.reason(),
                        decision.classifierModelId(),
                        decision.answerModelId(),
                        false);
                persistTurn(session, request.message(), answer, finalDecision, ownerId, degraded, true, started);
                audit(ownerId, "HANDOFF", session.getId(), handoffReason);
                metrics.recordChat(decision.intent().name(), "handoff", degraded, true,
                        System.currentTimeMillis() - started);
                return new ChatResponse(
                        session.getId(), answer, finalDecision, List.of(),
                        degraded, List.copyOf(degradedReasons), true, handoffReason);
            }

            String answerModelId = resolveAnswerModel(decision, request.options());
            boolean ragEnabled = isRagEnabled(decision, request.options());

            List<KnowledgeChunk> chunks = List.of();
            if (ragEnabled) {
                KnowledgeSearchResult kr = knowledgeSearchService.searchDetailed(
                        request.message(), properties.getRag().getTopK());
                chunks = kr.chunks();
                if (kr.degraded()) {
                    degraded = true;
                    degradedReasons.addAll(kr.degradedReasons());
                }
            }
            List<SourceDto> sources = toSources(chunks);

            String answer;
            if (guardrailPolicy.shouldForceEvidenceDisclaimer(ragEnabled, chunks)
                    && properties.getGuardrail().isRequireEvidence()) {
                answer = guardrailPolicy.evidenceInsufficientAnswer();
                degraded = true;
                degradedReasons.add("NO_EVIDENCE");
            } else {
                List<Message> messages = buildMessages(history, chunks, request.message(), ragEnabled);
                Duration timeout = Duration.ofMillis(properties.getResilience().getAnswerTimeoutMs());
                if (modelGateway instanceof com.enterprise.csai.domain.port.ModelPort port) {
                    answer = port.chat(answerModelId, messages, timeout);
                } else {
                    answer = modelGateway.chat(answerModelId, messages);
                }
            }

            RoutingDecision finalDecision = new RoutingDecision(
                    decision.intent(),
                    decision.confidence(),
                    decision.reason(),
                    decision.classifierModelId(),
                    answerModelId,
                    ragEnabled);

            long latency = System.currentTimeMillis() - started;
            UUID userMessageId = saveMessage(session.getId(), "USER", request.message());
            saveMessage(session.getId(), "ASSISTANT", answer);
            sessionRepository.touch(session.getId());
            routeLogRepository.insert(
                    UUID.randomUUID(),
                    session.getId(),
                    userMessageId,
                    request.message(),
                    finalDecision,
                    latency,
                    RequestIdContext.get(),
                    ownerId,
                    degraded,
                    handoff);

            log.info(
                    "chat done sessionId={} owner={} intent={} model={} rag={} sources={} degraded={} handoff={} latencyMs={}",
                    session.getId(), ownerId, finalDecision.intent(), answerModelId, ragEnabled,
                    sources.size(), degraded, handoff, latency);
            metrics.recordChat(finalDecision.intent().name(), status, degraded, handoff, latency);

            return new ChatResponse(
                    session.getId(),
                    answer,
                    finalDecision,
                    sources,
                    degraded,
                    List.copyOf(degradedReasons),
                    handoff,
                    handoffReason);
        } catch (BusinessException ex) {
            metrics.recordChat("unknown", "error", degraded, handoff, System.currentTimeMillis() - started);
            throw ex;
        } catch (Exception ex) {
            metrics.recordChat("unknown", "error", degraded, handoff, System.currentTimeMillis() - started);
            throw new BusinessException(CsaiErrorCodes.CHAT_FAILED, "chat failed: " + ex.getMessage(), ex);
        }
    }

    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        ApiKeyPrincipal principal = CsaiPrincipalHolder.get();
        String ownerId = principal.id();
        streamExecutor.execute(() -> {
            try {
                long started = System.currentTimeMillis();
                sendStatus(emitter, "received", "已收到消息，开始处理…");
                if (guardrailPolicy.looksLikeInjection(request.message())) {
                    String answer = guardrailPolicy.injectionBlockedAnswer();
                    emitter.send(SseEmitter.event().name("delta").data(answer));
                    sendErrorMeta(emitter, null, answer);
                    emitter.complete();
                    return;
                }
                ChatSessionEntity session = loadOrCreateSession(request.sessionId(), request.message(), ownerId);
                sendStatus(emitter, "session", "会话 " + session.getId());
                List<ChatMessageEntity> history = messageRepository.findRecentBySession(
                        session.getId(), properties.getChat().getHistoryMaxMessages());

                sendStatus(emitter, "classifying", "正在识别意图（本地模型可能需要数十秒）…");
                RoutingDecision decision = routingService.route(request.message());
                sendStatus(emitter, "classified",
                        "意图 " + decision.intent() + " · 置信度 " + String.format("%.2f", decision.confidence()));

                HandoffPolicy.HandoffDecision hd = handoffPolicy.evaluate(decision, request.message());
                if (hd.handoff()) {
                    sendStatus(emitter, "handoff", "触发转人工：" + hd.reason());
                    String answer = guardrailPolicy.handoffAnswer(hd.reason());
                    emitter.send(SseEmitter.event().name("delta").data(answer));
                    RoutingDecision finalDecision = new RoutingDecision(
                            decision.intent(), decision.confidence(), decision.reason(),
                            decision.classifierModelId(), decision.answerModelId(), false);
                    persistTurn(session, request.message(), answer, finalDecision, ownerId, false, true, started);
                    ChatResponse meta = new ChatResponse(
                            session.getId(), answer, finalDecision, List.of(),
                            false, List.of(), true, hd.reason());
                    emitter.send(SseEmitter.event().name("meta").data(meta, MediaType.APPLICATION_JSON));
                    emitter.complete();
                    return;
                }

                String answerModelId = resolveAnswerModel(decision, request.options());
                boolean ragEnabled = isRagEnabled(decision, request.options());
                List<String> degradedReasons = new ArrayList<>();
                boolean degraded = false;
                List<KnowledgeChunk> chunks = List.of();
                if (ragEnabled) {
                    sendStatus(emitter, "retrieving", "正在检索知识库…");
                    KnowledgeSearchResult kr = knowledgeSearchService.searchDetailed(
                            request.message(), properties.getRag().getTopK());
                    chunks = kr.chunks();
                    if (kr.degraded()) {
                        degraded = true;
                        degradedReasons.addAll(kr.degradedReasons());
                    }
                    sendStatus(emitter, "retrieved",
                            "知识命中 " + chunks.size() + " 条"
                                    + (degraded ? "（已降级检索）" : ""));
                } else {
                    sendStatus(emitter, "skip_rag", "本轮不检索知识库");
                }
                List<SourceDto> sources = toSources(chunks);

                if (guardrailPolicy.shouldForceEvidenceDisclaimer(ragEnabled, chunks)) {
                    String answer = guardrailPolicy.evidenceInsufficientAnswer();
                    emitter.send(SseEmitter.event().name("delta").data(answer));
                    RoutingDecision finalDecision = new RoutingDecision(
                            decision.intent(), decision.confidence(), decision.reason(),
                            decision.classifierModelId(), answerModelId, ragEnabled);
                    persistTurn(session, request.message(), answer, finalDecision, ownerId, true, false, started);
                    ChatResponse meta = new ChatResponse(
                            session.getId(), answer, finalDecision, sources,
                            true, List.of("NO_EVIDENCE"), false, null);
                    emitter.send(SseEmitter.event().name("meta").data(meta, MediaType.APPLICATION_JSON));
                    emitter.complete();
                    return;
                }

                sendStatus(emitter, "generating", "模型生成中（流式输出）… model=" + answerModelId);
                List<Message> messages = buildMessages(history, chunks, request.message(), ragEnabled);
                StringBuilder full = new StringBuilder();
                final boolean degradedFinal = degraded;
                final List<String> reasonsFinal = List.copyOf(degradedReasons);
                Flux<String> flux = modelGateway.stream(answerModelId, messages);
                flux.doOnNext(token -> {
                            full.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("delta").data(token));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .doOnError(err -> {
                            try {
                                sendStatus(emitter, "error", err.getMessage() == null
                                        ? "stream failed" : err.getMessage());
                            } catch (Exception ignored) {
                                // ignore
                            }
                            emitter.completeWithError(err);
                        })
                        .doOnComplete(() -> {
                            try {
                                String answer = full.toString();
                                UUID userMessageId = saveMessage(session.getId(), "USER", request.message());
                                saveMessage(session.getId(), "ASSISTANT", answer);
                                sessionRepository.touch(session.getId());
                                RoutingDecision finalDecision = new RoutingDecision(
                                        decision.intent(),
                                        decision.confidence(),
                                        decision.reason(),
                                        decision.classifierModelId(),
                                        answerModelId,
                                        ragEnabled);
                                long latency = System.currentTimeMillis() - started;
                                routeLogRepository.insert(
                                        UUID.randomUUID(),
                                        session.getId(),
                                        userMessageId,
                                        request.message(),
                                        finalDecision,
                                        latency,
                                        RequestIdContext.get(),
                                        ownerId,
                                        degradedFinal,
                                        false);
                                ChatResponse meta = new ChatResponse(
                                        session.getId(), answer, finalDecision, sources,
                                        degradedFinal, reasonsFinal, false, null);
                                sendStatus(emitter, "done", "完成 · " + latency + " ms");
                                emitter.send(SseEmitter.event().name("meta").data(meta, MediaType.APPLICATION_JSON));
                                emitter.complete();
                                metrics.recordChat(finalDecision.intent().name(), "ok",
                                        degradedFinal, false, latency);
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        })
                        .subscribe();
            } catch (Exception ex) {
                try {
                    sendStatus(emitter, "error", ex.getMessage() == null ? "failed" : ex.getMessage());
                } catch (Exception ignored) {
                    // ignore
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private static void sendStatus(SseEmitter emitter, String phase, String message) throws IOException {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("phase", phase);
        body.put("message", message);
        emitter.send(SseEmitter.event().name("status").data(body, MediaType.APPLICATION_JSON));
    }

    private static void sendErrorMeta(SseEmitter emitter, UUID sessionId, String answer) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessionId", sessionId);
        body.put("answer", answer);
        body.put("error", true);
        emitter.send(SseEmitter.event().name("meta").data(body, MediaType.APPLICATION_JSON));
    }

    private void persistTurn(
            ChatSessionEntity session,
            String userMessage,
            String answer,
            RoutingDecision decision,
            String ownerId,
            boolean degraded,
            boolean handoff,
            long started) {
        UUID userMessageId = saveMessage(session.getId(), "USER", userMessage);
        saveMessage(session.getId(), "ASSISTANT", answer);
        sessionRepository.touch(session.getId());
        routeLogRepository.insert(
                UUID.randomUUID(),
                session.getId(),
                userMessageId,
                userMessage,
                decision,
                System.currentTimeMillis() - started,
                RequestIdContext.get(),
                ownerId,
                degraded,
                handoff);
    }

    private void audit(String principalId, String eventType, UUID sessionId, String detail) {
        try {
            auditLogRepository.insert(
                    UUID.randomUUID(),
                    principalId,
                    eventType,
                    sessionId,
                    detail,
                    RequestIdContext.get());
        } catch (Exception ex) {
            log.warn("audit insert failed: {}", ex.getMessage());
        }
    }

    private ChatSessionEntity loadOrCreateSession(UUID sessionId, String firstMessage, String ownerId) {
        if (sessionId != null) {
            ChatSessionEntity existing = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(
                            CsaiErrorCodes.SESSION_NOT_FOUND, "session not found: " + sessionId));
            if (existing.getOwnerId() != null
                    && !existing.getOwnerId().isBlank()
                    && ownerId != null
                    && !"anonymous".equals(ownerId)
                    && !"system".equals(ownerId)
                    && !existing.getOwnerId().equals(ownerId)) {
                throw new BusinessException(
                        CsaiErrorCodes.SESSION_FORBIDDEN,
                        "session belongs to another principal");
            }
            return existing;
        }
        return createSession(firstMessage, ownerId);
    }

    private ChatSessionEntity createSession(String firstMessage, String ownerId) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(UUID.randomUUID());
        String title = firstMessage == null ? "chat" : firstMessage.strip();
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }
        session.setTitle(title);
        session.setOwnerId(ownerId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.insert(session);
        return session;
    }

    private UUID saveMessage(UUID sessionId, String role, String content) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setSessionId(sessionId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        messageRepository.insert(entity);
        return entity.getId();
    }

    private String resolveAnswerModel(RoutingDecision decision, ChatOptions options) {
        if (options != null && options.overrideAnswerModelId() != null
                && !options.overrideAnswerModelId().isBlank()) {
            return options.overrideAnswerModelId();
        }
        return decision.answerModelId();
    }

    private boolean isRagEnabled(RoutingDecision decision, ChatOptions options) {
        if (options != null && Boolean.TRUE.equals(options.forceRag())) {
            return true;
        }
        if (options != null && Boolean.FALSE.equals(options.forceRag())) {
            return false;
        }
        return decision.ragEnabled();
    }

    private List<Message> buildMessages(
            List<ChatMessageEntity> history,
            List<KnowledgeChunk> chunks,
            String userMessage,
            boolean ragEnabled) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(guardrailPolicy.systemPrompt() + "\n" + formatKnowledge(chunks, ragEnabled)));
        for (ChatMessageEntity msg : history) {
            if ("USER".equalsIgnoreCase(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("ASSISTANT".equalsIgnoreCase(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    private String formatKnowledge(List<KnowledgeChunk> chunks, boolean ragEnabled) {
        if (!ragEnabled) {
            return "Knowledge retrieval is disabled for this turn.";
        }
        if (chunks == null || chunks.isEmpty()) {
            return "Knowledge snippets: (none). If the question requires enterprise facts, say evidence is insufficient.";
        }
        StringBuilder sb = new StringBuilder("Knowledge snippets:\n");
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk c = chunks.get(i);
            sb.append("[").append(i + 1).append("] title=")
                    .append(c.title())
                    .append(" documentId=")
                    .append(c.documentId())
                    .append("\n")
                    .append(c.content())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private List<SourceDto> toSources(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .map(c -> new SourceDto(
                        c.documentId(),
                        c.title(),
                        truncate(c.content(), 240),
                        c.score()))
                .toList();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.strip();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
