-- =============================================
-- V012 — privchat server event idempotency
-- =============================================
--
-- spec 07-application/SERVER_EVENT_DISPATCH_SPEC v1.0 §4 / §5。
--
-- Server Event 是 privchat-server 定义的通用 server→下游事件标准（不绑 channel）；
-- 所有 server 主动 emit 的事件（bot.followed / channel.message_created /
-- user.created / ...）都通过 `POST /service/privchat/server-event/dispatch` 进入
-- 下游，按 (event_type, internal_event_id) 幂等。
--
-- dedupe key = (event_type, internal_event_id)：按事件类型分桶治理（清理 / 统计 /
-- 局部重放）；命中回放首次 ack。

CREATE TABLE IF NOT EXISTS privchat_server_event_idempotency (
    id                  BIGSERIAL    PRIMARY KEY,
    event_type          VARCHAR(128) NOT NULL,
    internal_event_id   VARCHAR(128) NOT NULL,
    trace_id            VARCHAR(128),
    response_accepted   BOOLEAN      NOT NULL,
    response_code       INT          NOT NULL,
    response_message    VARCHAR(512),
    first_seen_at       BIGINT       NOT NULL,
    last_seen_at        BIGINT       NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_key
    ON privchat_server_event_idempotency(event_type, internal_event_id);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_first_seen
    ON privchat_server_event_idempotency(first_seen_at);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_event_type
    ON privchat_server_event_idempotency(event_type);

CREATE INDEX IF NOT EXISTS idx_privchat_server_event_idempotency_trace
    ON privchat_server_event_idempotency(trace_id);
