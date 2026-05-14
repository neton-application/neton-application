-- =============================================
-- V012 — privchat server event idempotency
-- =============================================
--
-- 见 sql/postgresql/V012__privchat_server_event_idempotency.sql 的设计说明。

CREATE TABLE IF NOT EXISTS privchat_server_event_idempotency (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type          VARCHAR(128) NOT NULL,
    internal_event_id   VARCHAR(128) NOT NULL,
    trace_id            VARCHAR(128),
    response_accepted   TINYINT(1)   NOT NULL,
    response_code       INT          NOT NULL,
    response_message    VARCHAR(512),
    first_seen_at       BIGINT       NOT NULL,
    last_seen_at        BIGINT       NOT NULL,
    UNIQUE KEY idx_privchat_server_event_idempotency_key (event_type, internal_event_id),
    KEY idx_privchat_server_event_idempotency_first_seen (first_seen_at),
    KEY idx_privchat_server_event_idempotency_event_type (event_type),
    KEY idx_privchat_server_event_idempotency_trace (trace_id)
);
