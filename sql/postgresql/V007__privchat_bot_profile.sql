-- =============================================
-- module-privchat: Bot Management v1 (PostgreSQL)
-- spec 07-application/im-admin/ADMIN_BOT_SPEC §3
-- =============================================
--
-- 新增表：
--   privchat_bot_profile   bot 自身配置 + menu_schema JSON + 关联 user_id / service_id
--
-- seed：
--   privchat_business_service 中预置 id=9001, name='bot' 作为默认 bot service
--   （与 BotEchoTransferHandler 现有约定一致，admin 创建 bot 时若未指定 service_id 默认 9001）
--
-- 关键约束：
--   - bot_id = application 分配（snowflake/递增），非 server 持有
--   - user_id UNIQUE：一个 PrivChat user 最多挂一个 bot_profile（避免双归属）
--   - service_id 必须能在 privchat_business_service 找到（FK 不硬约束，由 application 校验）
--   - menu_schema TEXT (JSON 字符串)；空 / null 表示无菜单

-- ===================
-- privchat_bot_profile
-- ===================

CREATE TABLE IF NOT EXISTS privchat_bot_profile (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    username VARCHAR(128),
    avatar_url VARCHAR(512),
    description VARCHAR(512),
    status SMALLINT NOT NULL DEFAULT 1,
    menu_schema TEXT,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_bot_profile_user_id
    ON privchat_bot_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_privchat_bot_profile_service_id
    ON privchat_bot_profile(service_id);

-- ===================
-- seed default bot service (id=9001, name='bot')
-- ===================

INSERT INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9001, 'bot', NULL, 1, 'Default bot service for application module', 0, 0)
ON CONFLICT (id) DO NOTHING;
