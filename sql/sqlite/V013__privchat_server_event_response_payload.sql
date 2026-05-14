-- =============================================
-- V013 — privchat_server_event_idempotency.response_payload
-- =============================================
--
-- 见 sql/postgresql/V013__privchat_server_event_response_payload.sql 的设计说明。

ALTER TABLE privchat_server_event_idempotency
    ADD COLUMN response_payload TEXT;
