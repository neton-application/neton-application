-- =============================================
-- application V004 — system_users add sex / dept_id / remark
-- =============================================
--
-- See sql/postgresql/V004__system_users_add_fields.sql for context.
-- SQLite supports neither IF NOT EXISTS nor multi-column ADD; split into
-- separate statements.

ALTER TABLE system_users ADD COLUMN sex INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_users ADD COLUMN dept_id INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_users ADD COLUMN remark VARCHAR(512);
