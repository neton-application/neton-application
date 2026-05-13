-- =============================================
-- application V010 — align privchat_bot_profile schema with PrivChat user (PostgreSQL)
-- =============================================
--
-- 改动语义（结合 ADMIN_BOT_SPEC v1 + 这一轮 spec 修正）：
--   - bot 自己就是一个 PrivChat user（user_type ∈ {1=System, 2=Bot}），
--     所以 `privchat_bot_profile.id` 直接 = bot 的 PrivChat user_id，不再 application 自分配。
--   - 原 `user_id` 字段语义偏移：实际是"所属用户/owner"。重命名为 `owner_user_id`，
--     并取消 UNIQUE（一个用户可以拥有多个 bot）。
--   - name / username / avatar_url 三个字段从 bot_profile 移除——它们由 PrivChat user 表权威，
--     头像走 member 模块独立管理。bot_profile 只存 application 自己的扩展字段
--     （service_id / description / status / menu_schema）。
--
-- 表里 V007 之后没成功的数据写入（之前 INSERT 报 23502），所以 ALTER 安全。

ALTER TABLE privchat_bot_profile DROP COLUMN IF EXISTS name;
ALTER TABLE privchat_bot_profile DROP COLUMN IF EXISTS username;
ALTER TABLE privchat_bot_profile DROP COLUMN IF EXISTS avatar_url;

-- 之前 V007 把 user_id 建成了 UNIQUE；现在它是 owner_user_id，去掉唯一约束。
DROP INDEX IF EXISTS idx_privchat_bot_profile_user_id;

ALTER TABLE privchat_bot_profile RENAME COLUMN user_id TO owner_user_id;

CREATE INDEX IF NOT EXISTS idx_privchat_bot_profile_owner_user_id
    ON privchat_bot_profile(owner_user_id);
