-- =============================================
-- V012 — privchat server event idempotency
-- =============================================
--
-- 见 sql/postgresql/V012__privchat_server_event_idempotency.sql 的设计说明。

CREATE TABLE IF NOT EXISTS privchat_server_event_idempotency (
    id                  INTEGER      PRIMARY KEY AUTOINCREMENT,
    event_type          TEXT         NOT NULL,
    internal_event_id   TEXT         NOT NULL,
    trace_id            TEXT,
    response_accepted   INTEGER      NOT NULL,
    response_code       INTEGER      NOT NULL,
    response_message    TEXT,
    first_seen_at       INTEGER      NOT NULL,
    last_seen_at        INTEGER      NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_key
    ON privchat_server_event_idempotency(event_type, internal_event_id);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_first_seen
    ON privchat_server_event_idempotency(first_seen_at);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_event_type
    ON privchat_server_event_idempotency(event_type);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_trace
    ON privchat_server_event_idempotency(trace_id);
