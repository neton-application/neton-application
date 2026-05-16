-- =============================================
-- module-privchat: System User v1 (MySQL)
-- spec 07-application/SYSTEM_USER_SPEC §2.2
-- =============================================
--
-- 见 sql/postgresql/V017__privchat_system_user_profile.sql 的 context。

CREATE TABLE IF NOT EXISTS privchat_system_user_profile (
    id BIGINT NOT NULL PRIMARY KEY,
    service_id BIGINT NOT NULL,
    description TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    dm_enabled TINYINT(1) NOT NULL DEFAULT 1,
    group_invitable TINYINT(1) NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_privchat_system_user_profile_service_id
    ON privchat_system_user_profile(service_id);

-- seed assistant service
INSERT IGNORE INTO privchat_business_service (id, name, callback_url, status, description, created_at, updated_at)
VALUES (9002, 'assistant', NULL, 1, 'AI assistant service — system_user.message_received consumer', 0, 0);
