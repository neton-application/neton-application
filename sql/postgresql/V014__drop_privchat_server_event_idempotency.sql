-- =============================================
-- V014 — DROP privchat_server_event_idempotency
-- =============================================
--
-- spec 07-application/SERVER_EVENT_DISPATCH_SPEC v1.1 §5（修订）。
--
-- ServerEvent v1 是 at-most-once / best-effort：server 不重试、不持久化重投队列。
-- 在这个语义下"幂等命中回放"分支永远不会被触发——这张表沦为每条 event 一行的
-- 投递日志，浪费 DB 写入。
--
-- 框架边界原则（PRIVCHAT_APPLICATION_SERVICE_SPEC §7 修订）：
-- - framework 保存"关系和状态"
-- - **不**保存"每次请求的流水"
-- - 业务需要审计时由业务 handler 自己写业务表
--
-- 未来如果 ServerEvent 引入 retry / dead-letter queue，再设计专门的
-- delivery_attempt 表（带 TTL GC），不要复用本表。

DROP TABLE IF EXISTS privchat_server_event_idempotency;
