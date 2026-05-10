-- =============================================
-- module-privchat: Channel Transfer dispatch 数据表 (SQLite)
-- spec 07-application/CHANNEL_TRANSFER_DISPATCH_SPEC v1.0 §4
-- =============================================
--
-- 见 sql/postgresql/V006__privchat_business_tables.sql 的 context。

CREATE TABLE IF NOT EXISTS privchat_business_service (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    callback_url TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_business_service_name
    ON privchat_business_service(name);

CREATE TABLE IF NOT EXISTS privchat_business_room (
    channel_id INTEGER PRIMARY KEY,
    service_id INTEGER NOT NULL,
    business_id INTEGER,
    business_type TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_privchat_business_room_service
    ON privchat_business_room(service_id);
CREATE INDEX IF NOT EXISTS idx_privchat_business_room_business
    ON privchat_business_room(business_type, business_id);

CREATE TABLE IF NOT EXISTS privchat_transfer_idempotency (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    channel_id INTEGER NOT NULL,
    client_request_id TEXT NOT NULL,
    service_id INTEGER,
    response_code INTEGER NOT NULL,
    response_message TEXT,
    response_data TEXT,
    expires_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_transfer_idempotency_key
    ON privchat_transfer_idempotency(user_id, channel_id, client_request_id);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_idempotency_expires
    ON privchat_transfer_idempotency(expires_at);

CREATE TABLE IF NOT EXISTS privchat_transfer_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    internal_request_id TEXT NOT NULL,
    client_request_id TEXT NOT NULL,
    trace_id TEXT,
    channel_id INTEGER NOT NULL,
    room_id INTEGER,
    user_id INTEGER NOT NULL,
    service_id INTEGER,
    service_name TEXT,
    route TEXT NOT NULL,
    code INTEGER,
    message TEXT,
    elapsed_ms INTEGER,
    created_at INTEGER NOT NULL DEFAULT 0,
    completed_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_service_route_created
    ON privchat_transfer_audit_log(service_id, route, created_at);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_user_channel_created
    ON privchat_transfer_audit_log(user_id, channel_id, created_at);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_trace
    ON privchat_transfer_audit_log(trace_id);
