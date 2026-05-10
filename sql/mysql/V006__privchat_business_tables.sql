-- =============================================
-- module-privchat: Channel Transfer dispatch 数据表 (MySQL)
-- spec 07-application/CHANNEL_TRANSFER_DISPATCH_SPEC v1.0 §4
-- =============================================
--
-- 见 sql/postgresql/V006__privchat_business_tables.sql 的 context。
-- response_data 用 MEDIUMBLOB（16MB 上限）确保 64KB body + base64 等价头都装得下。

CREATE TABLE IF NOT EXISTS privchat_business_service (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    callback_url VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(512),
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE UNIQUE INDEX idx_privchat_business_service_name
    ON privchat_business_service(name);

CREATE TABLE IF NOT EXISTS privchat_business_room (
    channel_id BIGINT NOT NULL PRIMARY KEY,
    service_id BIGINT NOT NULL,
    business_id BIGINT,
    business_type VARCHAR(64),
    status TINYINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_privchat_business_room_service
    ON privchat_business_room(service_id);
CREATE INDEX idx_privchat_business_room_business
    ON privchat_business_room(business_type, business_id);

CREATE TABLE IF NOT EXISTS privchat_transfer_idempotency (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    service_id BIGINT,
    response_code INT NOT NULL,
    response_message VARCHAR(512),
    response_data MEDIUMTEXT,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE UNIQUE INDEX idx_privchat_transfer_idempotency_key
    ON privchat_transfer_idempotency(user_id, channel_id, client_request_id);
CREATE INDEX idx_privchat_transfer_idempotency_expires
    ON privchat_transfer_idempotency(expires_at);

CREATE TABLE IF NOT EXISTS privchat_transfer_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    internal_request_id VARCHAR(128) NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    trace_id VARCHAR(128),
    channel_id BIGINT NOT NULL,
    room_id BIGINT,
    user_id BIGINT NOT NULL,
    service_id BIGINT,
    service_name VARCHAR(128),
    route VARCHAR(256) NOT NULL,
    code INT,
    message VARCHAR(512),
    elapsed_ms BIGINT,
    created_at BIGINT NOT NULL DEFAULT 0,
    completed_at BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_privchat_transfer_audit_service_route_created
    ON privchat_transfer_audit_log(service_id, route, created_at);
CREATE INDEX idx_privchat_transfer_audit_user_channel_created
    ON privchat_transfer_audit_log(user_id, channel_id, created_at);
CREATE INDEX idx_privchat_transfer_audit_trace
    ON privchat_transfer_audit_log(trace_id);
