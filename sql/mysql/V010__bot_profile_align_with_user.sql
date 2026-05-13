-- =============================================
-- application V010 — align privchat_bot_profile schema with PrivChat user (MySQL)
-- =============================================
-- See sql/postgresql/V010__bot_profile_align_with_user.sql for context.

ALTER TABLE privchat_bot_profile DROP COLUMN name;
ALTER TABLE privchat_bot_profile DROP COLUMN username;
ALTER TABLE privchat_bot_profile DROP COLUMN avatar_url;

ALTER TABLE privchat_bot_profile DROP INDEX idx_privchat_bot_profile_user_id;

ALTER TABLE privchat_bot_profile CHANGE COLUMN user_id owner_user_id BIGINT NOT NULL;

CREATE INDEX idx_privchat_bot_profile_owner_user_id
    ON privchat_bot_profile(owner_user_id);
