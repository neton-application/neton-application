-- =============================================
-- application V004 — system_users add sex / dept_id / remark
-- =============================================
--
-- model.User declares `sex` / `deptId` / `remark` (vue-vben-admin standard
-- user fields, used by the admin web). The KSP-generated UserRowMapper
-- fails on `row.get("sex")` etc. when these columns are missing → admin
-- login returns 500 even though business logic is sound.
--
-- Adding the columns aligns the schema with the model. `IF NOT EXISTS`
-- keeps the migration idempotent for existing databases that have already
-- been patched manually.

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS sex SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dept_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS remark VARCHAR(512);
