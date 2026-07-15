package com.enterprise.csai.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            UUID id,
            String principalId,
            String eventType,
            UUID sessionId,
            String detail,
            String requestId) {
        jdbcTemplate.update("""
                INSERT INTO cs_audit_log (id, principal_id, event_type, session_id, detail, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                id,
                principalId,
                eventType,
                sessionId,
                detail,
                requestId);
    }
}
