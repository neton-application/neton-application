-- =============================================
-- module-privchat: System User v1 (PostgreSQL)
-- spec 07-application/SYSTEM_USER_SPEC §2.2
-- =============================================
--
-- 新增表：
--   privchat_system_user_profile  System User 与 service 的 1:1 绑定关系
--                                 + 状态 + DM 入口开关
--
-- seed：
--   privchat_business_service 中预置 id=9002, name='assistant' 作为
--   AI 助手 service（与未来 privchat-application-module-assistant 约定）
--
-- 关键约束：
--   - id = 对应 privchat_users.user_id（一对一）；不由 application 单独分配
--   - service_id 必须能在 privchat_business_service 找到（FK 不硬约束，由 application 校验）
--   - group_invitable 强制 FALSE 且**不**开放 admin UI——v1 永远禁止 System User 入群
--   - 不复用 privchat_bot_profile（owner_user_id 对 System User 没有语义）
--
-- 量级模型：
--   System User 是**全局唯一身份**——Assistant 服务 1000 万用户对应数据库
--   1 条 privchat_users + 1 条本表行 + 1000 万条 privchat_channels；
--   本表行**不**随用户数膨胀。

-- ===================
-- privchat_system_user_profile
-- ===================

CREATE TABLE IF NOT EXISTS privchat_system_user_profile (
    id BIGINT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 1,
    dm_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    group_invitable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_privchat_system_user_profile_service_id
    ON privchat_system_user_profile(service_id);

-- ===================
-- seed assistant service (id=9002, name='assistant')
-- ===================

INSERT INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9002, 'assistant', NULL, 1, 'AI assistant service — system_user.message_received consumer', 0, 0)
ON CONFLICT (id) DO NOTHING;
