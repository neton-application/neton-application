-- =============================================
-- V016 — DROP privchat_transfer_idempotency
-- =============================================
--
-- spec 07-application/CHANNEL_TRANSFER_DISPATCH_SPEC v1.2 §7（修订）+
-- PRIVCHAT_APPLICATION_SERVICE_SPEC §7（修订）。
--
-- framework 边界原则：保存"关系和状态"，**不**保存"每次请求的流水"。Transfer
-- dispatch 层的 `(user_id, channel_id, client_request_id)` 幂等本质是网络抖动下
-- 的**短期重复请求保护**——v1 改成**内存 LRU + TTL 5min**，重启清空可接受。
--
-- 强一致业务幂等由业务 handler 自己持久化：
--
-- - 游戏 action_seq → game_action_log
-- - 钱包 idempotency_key → wallet_order / wallet_ledger
-- - bot/menu/get 之类查询 → 不需要持久幂等
--
-- framework 不替业务背"长期幂等账本"的责任。

DROP TABLE IF EXISTS privchat_transfer_idempotency;
