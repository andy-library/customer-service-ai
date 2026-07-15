CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE cs_document (
    id              UUID PRIMARY KEY,
    title           VARCHAR(512) NOT NULL,
    filename        VARCHAR(512) NOT NULL,
    content_type    VARCHAR(128),
    status          VARCHAR(32)  NOT NULL,
    chunk_count     INT          NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE cs_chat_session (
    id          UUID PRIMARY KEY,
    title       VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cs_chat_message (
    id          UUID PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES cs_chat_session(id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cs_chat_message_session ON cs_chat_message(session_id, created_at);

CREATE TABLE cs_route_log (
    id                    UUID PRIMARY KEY,
    session_id            UUID,
    message_id            UUID,
    user_query            TEXT NOT NULL,
    intent                VARCHAR(64) NOT NULL,
    confidence            DOUBLE PRECISION,
    classifier_model_id   VARCHAR(128),
    answer_model_id       VARCHAR(128),
    rag_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    latency_ms            BIGINT,
    request_id            VARCHAR(128),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
