-- =============================================
-- module-privchat: System User v1 (SQLite)
-- spec 07-application/SYSTEM_USER_SPEC §2.2
-- =============================================
--
-- 见 sql/postgresql/V017__privchat_system_user_profile.sql 的 context。

CREATE TABLE IF NOT EXISTS privchat_system_user_profile (
    id INTEGER PRIMARY KEY,
    service_id INTEGER NOT NULL,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    dm_enabled INTEGER NOT NULL DEFAULT 1,
    group_invitable INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_privchat_system_user_profile_service_id
    ON privchat_system_user_profile(service_id);

-- seed assistant service
INSERT OR IGNORE INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9002, 'assistant', NULL, 1, 'AI assistant service — system_user.message_received consumer', 0, 0);
