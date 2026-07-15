package com.enterprise.csai.chat;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import com.enterprise.csai.modelgateway.ModelGateway;
import com.enterprise.csai.router.RoutingDecision;
import com.enterprise.csai.router.RoutingService;
import com.microservice.framework.web.context.RequestIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);

    private static final String SYSTEM_BASE = """
            You are an enterprise customer-service assistant.
            Prefer answers grounded in the provided knowledge snippets.
            If knowledge is required but snippets are empty or insufficient, clearly say
            that the knowledge base does not contain enough evidence. Do not invent
            internal policy numbers, prices, or ticket IDs that are not present in the snippets.
            Answer in the same language as the user question when possible.
            """;

    private final RoutingService routingService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ModelGateway modelGateway;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RouteLogRepository routeLogRepository;
    private final CsaiProperties properties;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ChatOrchestrator(
            RoutingService routingService,
            KnowledgeSearchService knowledgeSearchService,
            ModelGateway modelGateway,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            RouteLogRepository routeLogRepository,
            CsaiProperties properties) {
        this.routingService = routingService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.modelGateway = modelGateway;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.routeLogRepository = routeLogRepository;
        this.properties = properties;
    }

    public ChatResponse chat(ChatRequest request) {
        long started = System.currentTimeMillis();
        ChatSessionEntity session = loadOrCreateSession(request.sessionId(), request.message());
        List<ChatMessageEntity> history = messageRepository.findRecentBySession(
                session.getId(), properties.getChat().getHistoryMaxMessages());

        RoutingDecision decision = routingService.route(request.message());
        String answerModelId = resolveAnswerModel(decision, request.options());
        boolean ragEnabled = isRagEnabled(decision, request.options());

        List<KnowledgeChunk> chunks = ragEnabled
                ? knowledgeSearchService.search(request.message(), properties.getRag().getTopK())
                : List.of();
        List<SourceDto> sources = toSources(chunks);

        List<Message> messages = buildMessages(history, chunks, request.message(), ragEnabled);
        String answer = modelGateway.chat(answerModelId, messages);

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
                RequestIdContext.get());

        log.info("chat done sessionId={} intent={} model={} rag={} sources={} latencyMs={}",
                session.getId(), finalDecision.intent(), answerModelId, ragEnabled, sources.size(), latency);

        return new ChatResponse(session.getId(), answer, finalDecision, sources);
    }

    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        streamExecutor.execute(() -> {
            try {
                long started = System.currentTimeMillis();
                ChatSessionEntity session = loadOrCreateSession(request.sessionId(), request.message());
                List<ChatMessageEntity> history = messageRepository.findRecentBySession(
                        session.getId(), properties.getChat().getHistoryMaxMessages());

                RoutingDecision decision = routingService.route(request.message());
                String answerModelId = resolveAnswerModel(decision, request.options());
                boolean ragEnabled = isRagEnabled(decision, request.options());
                List<KnowledgeChunk> chunks = ragEnabled
                        ? knowledgeSearchService.search(request.message(), properties.getRag().getTopK())
                        : List.of();
                List<SourceDto> sources = toSources(chunks);
                List<Message> messages = buildMessages(history, chunks, request.message(), ragEnabled);

                StringBuilder full = new StringBuilder();
                Flux<String> flux = modelGateway.stream(answerModelId, messages);
                flux.doOnNext(token -> {
                            full.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("delta").data(token));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .doOnError(err -> emitter.completeWithError(err))
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
                                        RequestIdContext.get());
                                ChatResponse meta = new ChatResponse(
                                        session.getId(), answer, finalDecision, sources);
                                emitter.send(SseEmitter.event().name("meta").data(meta));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        })
                        .subscribe();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private ChatSessionEntity loadOrCreateSession(UUID sessionId, String firstMessage) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId).orElseGet(() -> createSession(firstMessage));
        }
        return createSession(firstMessage);
    }

    private ChatSessionEntity createSession(String firstMessage) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(UUID.randomUUID());
        String title = firstMessage == null ? "chat" : firstMessage.strip();
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }
        session.setTitle(title);
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
        messages.add(new SystemMessage(SYSTEM_BASE + "\n" + formatKnowledge(chunks, ragEnabled)));
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
