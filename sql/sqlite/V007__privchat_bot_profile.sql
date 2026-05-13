-- =============================================
-- module-privchat: Bot Management v1 (SQLite)
-- spec 07-application/im-admin/ADMIN_BOT_SPEC §3
-- =============================================
--
-- 见 sql/postgresql/V007__privchat_bot_profile.sql 的 context。

CREATE TABLE IF NOT EXISTS privchat_bot_profile (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    username TEXT,
    avatar_url TEXT,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    menu_schema TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_bot_profile_user_id
    ON privchat_bot_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_privchat_bot_profile_service_id
    ON privchat_bot_profile(service_id);

-- seed default bot service
INSERT OR IGNORE INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9001, 'bot', NULL, 1, 'Default bot service for application module', 0, 0);
