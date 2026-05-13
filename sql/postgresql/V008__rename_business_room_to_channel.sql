-- =============================================
-- module-privchat: Channel Transfer / Message Dispatch binding rename (PostgreSQL)
-- spec 07-application/PRIVCHAT_APPLICATION_SERVICE_SPEC + CHANNEL_TRANSFER_DISPATCH_SPEC §4.2 + CHANNEL_MESSAGE_EVENT_DISPATCH_SPEC §4.1
-- =============================================
--
-- 把 V006 落地的 `privchat_business_room` 表对齐到 v1.0 spec：
--   privchat_business_room      → privchat_business_channel  （表达 channel↔service 绑定，不是 room）
--   business_id                 → business_ref_id            （业务对象引用 ID，gateway 不解释）
--   business_type               → business_ref_type          （业务对象引用类型 metadata）
-- 新增：
--   dispatch_transfer_enabled SMALLINT DEFAULT 1  （该 channel 是否允许走 /service/privchat/transfer/dispatch）
--   dispatch_message_enabled  SMALLINT DEFAULT 0  （该 channel 是否允许走 /service/privchat/message/dispatch；
--                                                  与 server.application_message_dispatch_enabled 是两层独立开关）
-- 索引同步重命名，保持现有查询无感知。

ALTER TABLE privchat_business_room
    RENAME TO privchat_business_channel;

ALTER TABLE privchat_business_channel
    RENAME COLUMN business_id TO business_ref_id;
ALTER TABLE privchat_business_channel
    RENAME COLUMN business_type TO business_ref_type;

ALTER TABLE privchat_business_channel
    ADD COLUMN IF NOT EXISTS dispatch_transfer_enabled SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE privchat_business_channel
    ADD COLUMN IF NOT EXISTS dispatch_message_enabled SMALLINT NOT NULL DEFAULT 0;

-- 索引重命名（PG 支持原索引名跟旧表绑定，需要显式 rename）
ALTER INDEX IF EXISTS idx_privchat_business_room_service
    RENAME TO idx_privchat_business_channel_service;
ALTER INDEX IF EXISTS idx_privchat_business_room_business
    RENAME TO idx_privchat_business_channel_business_ref;
