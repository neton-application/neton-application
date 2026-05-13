-- =============================================
-- application V010 — align privchat_bot_profile schema with PrivChat user (SQLite)
-- =============================================
-- See sql/postgresql/V010__bot_profile_align_with_user.sql for context.
-- SQLite 走"建新表 + 拷贝 + drop 旧" 模式（多版本 DROP/RENAME COLUMN 兼容不稳定）。

CREATE TABLE privchat_bot_profile_new (
    id INTEGER PRIMARY KEY,
    owner_user_id INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    menu_schema TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

-- 之前没成功的 INSERT，bot_profile 表里其实没行；SELECT 出 0 行也 OK。
INSERT INTO privchat_bot_profile_new (
    id, owner_user_id, service_id, description, status, menu_schema, created_at, updated_at
)
SELECT
    id, user_id, service_id, description, status, menu_schema, created_at, updated_at
FROM privchat_bot_profile;

DROP TABLE privchat_bot_profile;
ALTER TABLE privchat_bot_profile_new RENAME TO privchat_bot_profile;

CREATE INDEX IF NOT EXISTS idx_privchat_bot_profile_owner_user_id
    ON privchat_bot_profile(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_privchat_bot_profile_service_id
    ON privchat_bot_profile(service_id);
