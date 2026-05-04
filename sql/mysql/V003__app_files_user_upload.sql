-- =============================================
-- application V003 — infra_files 用户态上传扩展（MySQL）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §4.2

ALTER TABLE infra_files
    ADD COLUMN file_id       VARCHAR(64),
    ADD COLUMN owner_uid     BIGINT,
    ADD COLUMN business_type VARCHAR(64),
    ADD COLUMN sha256        VARCHAR(64),
    ADD COLUMN status        SMALLINT NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX uq_infra_files_file_id    ON infra_files(file_id);
CREATE INDEX        idx_infra_files_owner_uid ON infra_files(owner_uid);
CREATE INDEX        idx_infra_files_business  ON infra_files(business_type);
