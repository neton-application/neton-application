-- =============================================
-- module-privchat: Channel Transfer / Message Dispatch binding rename (MySQL)
-- =============================================
--
-- 见 sql/postgresql/V008__rename_business_room_to_channel.sql 的 context。

RENAME TABLE privchat_business_room TO privchat_business_channel;

ALTER TABLE privchat_business_channel
    CHANGE COLUMN business_id business_ref_id BIGINT NULL;

ALTER TABLE privchat_business_channel
    CHANGE COLUMN business_type business_ref_type VARCHAR(64) NULL;

ALTER TABLE privchat_business_channel
    ADD COLUMN dispatch_transfer_enabled TINYINT NOT NULL DEFAULT 1;

ALTER TABLE privchat_business_channel
    ADD COLUMN dispatch_message_enabled TINYINT NOT NULL DEFAULT 0;

-- MySQL 重命名索引（5.7+ 支持 ALTER TABLE ... RENAME INDEX）
ALTER TABLE privchat_business_channel
    RENAME INDEX idx_privchat_business_room_service TO idx_privchat_business_channel_service;
ALTER TABLE privchat_business_channel
    RENAME INDEX idx_privchat_business_room_business TO idx_privchat_business_channel_business_ref;
