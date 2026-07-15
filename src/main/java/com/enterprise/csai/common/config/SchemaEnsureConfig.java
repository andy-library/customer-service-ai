package com.enterprise.csai.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Idempotent safety net when Flyway history table is missing (legacy DBs).
 * Applies production columns/tables required by 0.2.x.
 */
@Configuration
public class SchemaEnsureConfig {

    private static final Logger log = LoggerFactory.getLogger(SchemaEnsureConfig.class);

    @Bean
    ApplicationRunner schemaEnsureRunner(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute("ALTER TABLE cs_chat_session ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_cs_session_owner ON cs_chat_session (owner_id)");
                jdbc.execute("ALTER TABLE cs_route_log ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128)");
                jdbc.execute("ALTER TABLE cs_route_log ADD COLUMN IF NOT EXISTS degraded BOOLEAN NOT NULL DEFAULT FALSE");
                jdbc.execute("ALTER TABLE cs_route_log ADD COLUMN IF NOT EXISTS handoff BOOLEAN NOT NULL DEFAULT FALSE");
                jdbc.execute("""
                        CREATE TABLE IF NOT EXISTS cs_audit_log (
                            id            UUID PRIMARY KEY,
                            principal_id  VARCHAR(128),
                            event_type    VARCHAR(64)  NOT NULL,
                            session_id    UUID,
                            detail        TEXT,
                            request_id    VARCHAR(128),
                            created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
                        )
                        """);
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_cs_audit_created ON cs_audit_log (created_at)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_cs_audit_principal ON cs_audit_log (principal_id)");
                log.info("schema ensure completed (production columns/tables)");
            } catch (Exception ex) {
                log.warn("schema ensure skipped/failed: {}", ex.getMessage());
            }
        };
    }
}
