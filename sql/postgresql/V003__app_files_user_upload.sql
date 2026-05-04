-- =============================================
-- application V003 — infra_files 用户态上传扩展（PostgreSQL）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §4.2
--
-- 把 infra_files 从"纯 admin 元数据登记表"扩成"应用唯一用户态文件入口"
-- （/app-api/infra/file/upload）的存储记录。
--
-- 新增列：
--   file_id        — 对外公开 ID（`file_<random>`），客户端引用 fileId 而非自增 id
--   owner_uid      — 上传者 uid；下载/引用时校验归属
--   business_type  — member_avatar / group_avatar / feedback_attachment / kyc_document / generic_image
--   sha256         — 内容指纹，用作 storage_key 的一部分（{businessType}/{ownerUid}/{sha256}.{ext}）
--   status         — 1=active, 0=orphaned/deleted
--
-- 不动现有列（id / config_id / name / path / url / mime_type / size / created_at）。
-- admin 端 /admin-api/infra/file/* 路径继续可用，纯元数据登记。

ALTER TABLE infra_files
    ADD COLUMN IF NOT EXISTS file_id       VARCHAR(64),
    ADD COLUMN IF NOT EXISTS owner_uid     BIGINT,
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sha256        VARCHAR(64),
    ADD COLUMN IF NOT EXISTS status        SMALLINT NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_infra_files_file_id      ON infra_files(file_id);
CREATE INDEX        IF NOT EXISTS idx_infra_files_owner_uid   ON infra_files(owner_uid);
CREATE INDEX        IF NOT EXISTS idx_infra_files_business    ON infra_files(business_type);
