-- =============================================
-- application V003 — infra_files 用户态上传扩展（SQLite）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §4.2
--
-- SQLite 不支持单条 ALTER 多列，逐条加。

ALTER TABLE infra_files ADD COLUMN file_id       VARCHAR(64);
ALTER TABLE infra_files ADD COLUMN owner_uid     BIGINT;
ALTER TABLE infra_files ADD COLUMN business_type VARCHAR(64);
ALTER TABLE infra_files ADD COLUMN sha256        VARCHAR(64);
ALTER TABLE infra_files ADD COLUMN status        SMALLINT NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_infra_files_file_id    ON infra_files(file_id);
CREATE INDEX        IF NOT EXISTS idx_infra_files_owner_uid ON infra_files(owner_uid);
CREATE INDEX        IF NOT EXISTS idx_infra_files_business  ON infra_files(business_type);
