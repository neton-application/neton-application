-- =============================================
-- V013 — privchat_server_event_idempotency.response_payload
-- =============================================
--
-- spec 07-application/SERVER_EVENT_DISPATCH_SPEC v1.0 §5（request-response
-- event 持久化 response_payload）。
--
-- 增加 response_payload TEXT 列存放 request-response event（如
-- `transfer.requested`）的回包字节（base64）。命中幂等时直接回放完整 ack
-- 包含 response_payload，**不**再调底层业务 handler。
--
-- ack-only event（bot.followed / bot.unfollowed / ...）此列保持 NULL。

ALTER TABLE privchat_server_event_idempotency
    ADD COLUMN response_payload TEXT;
