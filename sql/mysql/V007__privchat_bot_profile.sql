-- =============================================
-- module-privchat: Bot Management v1 (MySQL)
-- spec 07-application/im-admin/ADMIN_BOT_SPEC §3
-- =============================================
--
-- 见 sql/postgresql/V007__privchat_bot_profile.sql 的 context。

CREATE TABLE IF NOT EXISTS privchat_bot_profile (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    username VARCHAR(128),
    avatar_url VARCHAR(512),
    description VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 1,
    menu_schema MEDIUMTEXT,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE UNIQUE INDEX idx_privchat_bot_profile_user_id
    ON privchat_bot_profile(user_id);
CREATE INDEX idx_privchat_bot_profile_service_id
    ON privchat_bot_profile(service_id);

-- seed default bot service
INSERT IGNORE INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9001, 'bot', NULL, 1, 'Default bot service for application module', 0, 0);
