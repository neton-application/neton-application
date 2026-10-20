-- Acceptance-only fixtures. Never run against an application database.
DO $$ BEGIN
  IF current_database() <> 'mfa_browser' OR inet_server_addr() <> '127.0.0.1'::inet THEN
    RAISE EXCEPTION 'MFA fixtures require the disposable localhost mfa_browser database';
  END IF;
END $$;

-- Public test password: MfaTestOnly-2026! (PBKDF2 with production cost).
UPDATE system_users SET password_hash = 'pbkdf2-sha256$210000$bWZhLWFjY2VwdGFuY2Utb25seQ$ClE0UgjTp7pDObcpYioyev6talz4pxWwic3Yku2I8ac' WHERE id = 1;
INSERT INTO system_users (id, username, password_hash, nickname, status)
VALUES (91001, 'mfa_browser', 'pbkdf2-sha256$210000$bWZhLWFjY2VwdGFuY2Utb25seQ$ClE0UgjTp7pDObcpYioyev6talz4pxWwic3Yku2I8ac', 'MFA验收用户', 1)
ON CONFLICT (id) DO UPDATE SET password_hash = EXCLUDED.password_hash, status = 1, deleted = 0;
INSERT INTO system_user_roles (user_id, role_id, created_at)
SELECT 91001, 1, 0 WHERE NOT EXISTS (SELECT 1 FROM system_user_roles WHERE user_id = 91001 AND role_id = 1);
DELETE FROM system_mfa_challenges WHERE user_id IN (1, 91001);
DELETE FROM system_mfa WHERE user_id IN (1, 91001);
