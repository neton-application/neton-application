-- =============================================
-- V015 — DROP privchat_transfer_audit_log
-- =============================================
--
-- spec 07-application/CHANNEL_TRANSFER_DISPATCH_SPEC v1.2 §9（修订）+
-- PRIVCHAT_APPLICATION_SERVICE_SPEC §7（修订）。
--
-- framework 边界原则：保存"关系和状态"，**不**保存"每次请求的流水"。
--
-- 这张表过去每条 transfer dispatch 落 2 行（start + finish）—— 是 framework 强加的
-- 业务日志，没有命中查询路径。游戏 / 钱包 / bot 这种高频路由一上线就把表打爆。
--
-- 替代：dispatcher 的 start / finish 改成结构化 log.info / log.warn 输出到 stdout，
-- 运维想要审计自接 OTel / log collector；业务侧需要持久审计的，由业务 handler 自己
-- 写业务表（如 game_action_log / wallet_ledger）。

DROP TABLE IF EXISTS privchat_transfer_audit_log;
