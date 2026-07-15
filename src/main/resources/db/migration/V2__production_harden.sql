-- Production hardening: session ownership + audit trail + route_log flags

ALTER TABLE cs_chat_session
    ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_cs_session_owner ON cs_chat_session (owner_id);

ALTER TABLE cs_route_log
    ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128);

ALTER TABLE cs_route_log
    ADD COLUMN IF NOT EXISTS degraded BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE cs_route_log
    ADD COLUMN IF NOT EXISTS handoff BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS cs_audit_log (
    id            UUID PRIMARY KEY,
    principal_id  VARCHAR(128),
    event_type    VARCHAR(64)  NOT NULL,
    session_id    UUID,
    detail        TEXT,
    request_id    VARCHAR(128),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cs_audit_created ON cs_audit_log (created_at);
CREATE INDEX IF NOT EXISTS idx_cs_audit_principal ON cs_audit_log (principal_id);
