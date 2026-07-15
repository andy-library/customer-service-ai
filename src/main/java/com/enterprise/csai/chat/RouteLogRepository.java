package com.enterprise.csai.chat;

import com.enterprise.csai.router.RoutingDecision;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RouteLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public RouteLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            UUID id,
            UUID sessionId,
            UUID messageId,
            String userQuery,
            RoutingDecision decision,
            long latencyMs,
            String requestId) {
        insert(id, sessionId, messageId, userQuery, decision, latencyMs, requestId, null, false, false);
    }

    public void insert(
            UUID id,
            UUID sessionId,
            UUID messageId,
            String userQuery,
            RoutingDecision decision,
            long latencyMs,
            String requestId,
            String ownerId,
            boolean degraded,
            boolean handoff) {
        jdbcTemplate.update("""
                INSERT INTO cs_route_log
                (id, session_id, message_id, user_query, intent, confidence,
                 classifier_model_id, answer_model_id, rag_enabled, latency_ms, request_id,
                 owner_id, degraded, handoff, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                id,
                sessionId,
                messageId,
                userQuery,
                decision.intent().name(),
                decision.confidence(),
                decision.classifierModelId(),
                decision.answerModelId(),
                decision.ragEnabled(),
                latencyMs,
                requestId,
                ownerId,
                degraded,
                handoff);
    }
}
