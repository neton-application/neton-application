-- =============================================
-- module-privchat: Channel Transfer / Message Dispatch binding rename (SQLite)
-- =============================================
--
-- 见 sql/postgresql/V008__rename_business_room_to_channel.sql 的 context。
-- SQLite RENAME COLUMN 支持依赖版本（3.25+），稳妥起见用建新表 + 拷贝 + 删旧表的
-- 标准 SQLite 迁移模式。

CREATE TABLE IF NOT EXISTS privchat_business_channel (
    channel_id INTEGER PRIMARY KEY,
    service_id INTEGER NOT NULL,
    business_ref_id INTEGER,
    business_ref_type TEXT,
    dispatch_transfer_enabled INTEGER NOT NULL DEFAULT 1,
    dispatch_message_enabled INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

INSERT INTO privchat_business_channel (
    channel_id,
    service_id,
    business_ref_id,
    business_ref_type,
    dispatch_transfer_enabled,
    dispatch_message_enabled,
    status,
    created_at,
    updated_at
)
SELECT
    channel_id,
    service_id,
    business_id,
    business_type,
    1,
    0,
    status,
    created_at,
    updated_at
FROM privchat_business_room;

DROP TABLE privchat_business_room;

CREATE INDEX IF NOT EXISTS idx_privchat_business_channel_service
    ON privchat_business_channel(service_id);
CREATE INDEX IF NOT EXISTS idx_privchat_business_channel_business_ref
    ON privchat_business_channel(business_ref_type, business_ref_id);
