package com.enterprise.csai.chat;

import com.enterprise.csai.audit.AuditLogRepository;
import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.domain.policy.GuardrailPolicy;
import com.enterprise.csai.domain.policy.HandoffPolicy;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import com.enterprise.csai.knowledge.KnowledgeSearchResult;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import com.enterprise.csai.modelgateway.ModelGateway;
import com.enterprise.csai.observability.CsaiMetrics;
import com.enterprise.csai.router.IntentType;
import com.enterprise.csai.router.RoutingDecision;
import com.enterprise.csai.router.RoutingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorTest {

    @Mock RoutingService routingService;
    @Mock KnowledgeSearchService knowledgeSearchService;
    @Mock ModelGateway modelGateway;
    @Mock ChatSessionRepository sessionRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock RouteLogRepository routeLogRepository;
    @Mock AuditLogRepository auditLogRepository;

    CsaiProperties properties;
    ChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new CsaiProperties();
        properties.getChat().setHistoryMaxMessages(6);
        properties.getRag().setTopK(5);
        properties.getRouter().setConfidenceThreshold(0.55);
        properties.getRouter().setHandoffOnUnknown(true);
        properties.getGuardrail().setRequireEvidence(false);
        properties.getGuardrail().setBlockInjectionPatterns(true);
        GuardrailPolicy guardrailPolicy = new GuardrailPolicy(properties);
        HandoffPolicy handoffPolicy = new HandoffPolicy(properties);
        CsaiMetrics metrics = new CsaiMetrics(new SimpleMeterRegistry());
        orchestrator = new ChatOrchestrator(
                routingService,
                knowledgeSearchService,
                modelGateway,
                sessionRepository,
                messageRepository,
                routeLogRepository,
                auditLogRepository,
                properties,
                guardrailPolicy,
                handoffPolicy,
                metrics);
    }

    @Test
    void chatReturnsAnswerRouteAndSources() {
        when(routingService.route("如何退款")).thenReturn(new RoutingDecision(
                IntentType.BILLING, 0.9, "billing", "classifier-default", "answer-strong", true));
        when(knowledgeSearchService.searchDetailed(eq("如何退款"), anyInt()))
                .thenReturn(KnowledgeSearchResult.ok(List.of(new KnowledgeChunk(
                        "doc-1", "退款政策", "7 日内未激活可全额退款", 0.88))));
        when(modelGateway.chat(eq("answer-strong"), any()))
                .thenReturn("根据知识库，7 日内未激活可全额退款。");
        when(messageRepository.findRecentBySession(any(), anyInt())).thenReturn(List.of());

        ChatResponse resp = orchestrator.chat(new ChatRequest(null, "如何退款", null));

        assertThat(resp.answer()).contains("退款");
        assertThat(resp.sources()).isNotEmpty();
        assertThat(resp.route().answerModelId()).isEqualTo("answer-strong");
        assertThat(resp.route().intent()).isEqualTo(IntentType.BILLING);
        assertThat(resp.sessionId()).isNotNull();
        assertThat(resp.handoff()).isFalse();
        verify(sessionRepository).insert(any(ChatSessionEntity.class));
        verify(messageRepository, org.mockito.Mockito.times(2)).insert(any(ChatMessageEntity.class));
    }

    @Test
    void lowConfidenceTriggersHandoff() {
        when(routingService.route(any())).thenReturn(new RoutingDecision(
                IntentType.UNKNOWN, 0.2, "unsure", "classifier-default", "answer-fast", true));
        when(messageRepository.findRecentBySession(any(), anyInt())).thenReturn(List.of());

        ChatResponse resp = orchestrator.chat(new ChatRequest(null, "嗯？", null));

        assertThat(resp.handoff()).isTrue();
        assertThat(resp.handoffReason()).isIn("LOW_CONFIDENCE", "UNKNOWN_INTENT");
        assertThat(resp.answer()).contains("转人工");
    }

    @Test
    void overrideAnswerModelIsRespected() {
        when(routingService.route(any())).thenReturn(new RoutingDecision(
                IntentType.CHITCHAT, 0.9, "hi", "classifier-default", "answer-fast", false));
        when(modelGateway.chat(eq("answer-strong"), any())).thenReturn("ok");
        when(messageRepository.findRecentBySession(any(), anyInt())).thenReturn(List.of());

        ChatOptions options = new ChatOptions(false, "answer-strong");
        ChatResponse resp = orchestrator.chat(new ChatRequest(null, "你好", options));

        assertThat(resp.route().answerModelId()).isEqualTo("answer-strong");
        assertThat(resp.route().ragEnabled()).isFalse();
        assertThat(resp.sources()).isEmpty();
        verify(modelGateway).chat(eq("answer-strong"), any());
    }

    @Test
    void reusesExistingSession() {
        UUID sessionId = UUID.randomUUID();
        ChatSessionEntity existing = new ChatSessionEntity();
        existing.setId(sessionId);
        existing.setTitle("old");
        existing.setOwnerId("anonymous");
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(existing));
        when(routingService.route(any())).thenReturn(new RoutingDecision(
                IntentType.PRODUCT, 0.8, "p", "classifier-default", "answer-strong", true));
        when(knowledgeSearchService.searchDetailed(any(), anyInt()))
                .thenReturn(KnowledgeSearchResult.ok(List.of()));
        when(modelGateway.chat(any(), any())).thenReturn("answer");
        when(messageRepository.findRecentBySession(eq(sessionId), anyInt())).thenReturn(List.of());

        ChatResponse resp = orchestrator.chat(new ChatRequest(sessionId, "功能如何用", null));

        assertThat(resp.sessionId()).isEqualTo(sessionId);
        verify(sessionRepository).findById(sessionId);
    }
}
