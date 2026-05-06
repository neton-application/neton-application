-- =============================================
-- application V004 — system_users add sex / dept_id / remark
-- =============================================
--
-- See sql/postgresql/V004__system_users_add_fields.sql for context.
-- MySQL <8.0 does not support ADD COLUMN IF NOT EXISTS; running the
-- migration twice will error. New databases run cleanly.

ALTER TABLE system_users
    ADD COLUMN sex TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN dept_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN remark VARCHAR(512);
