-- =============================================
-- module-privchat: Channel Transfer dispatch 数据表 (PostgreSQL)
-- spec 07-application/CHANNEL_TRANSFER_DISPATCH_SPEC v1.0 §4
-- =============================================
--
-- 4 张表：
--   privchat_business_service       业务服务注册表（持久声明）
--   privchat_business_room          business room ↔ channel + service 绑定
--   privchat_transfer_idempotency   (user_id, channel_id, client_request_id) 幂等结果
--   privchat_transfer_audit_log     transfer 审计
--
-- 关键约束：
--   - privchat_business_room.channel_id = PK；transfer wire 上来就拿 channel_id 查
--   - business_id 字段承担业务 room 对外 ID，**不**再单独加 room_id 列
--   - 时间字段统一 Unix ms (BIGINT)
--   - 幂等表的 expires_at 配后台清理任务（不在本迁移里）
--   - server **不读**这些表（spec 02-server §0 / §1.4）

-- ===================
-- privchat_business_service
-- ===================

CREATE TABLE IF NOT EXISTS privchat_business_service (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    callback_url VARCHAR(512),
    status SMALLINT NOT NULL DEFAULT 1,
    description VARCHAR(512),
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_business_service_name
    ON privchat_business_service(name);

-- ===================
-- privchat_business_room (channel_id = PK)
-- ===================

CREATE TABLE IF NOT EXISTS privchat_business_room (
    channel_id BIGINT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    business_id BIGINT,
    business_type VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_privchat_business_room_service
    ON privchat_business_room(service_id);
CREATE INDEX IF NOT EXISTS idx_privchat_business_room_business
    ON privchat_business_room(business_type, business_id);

-- ===================
-- privchat_transfer_idempotency
-- ===================
--
-- dedupe key = (user_id, channel_id, client_request_id)；spec dispatch §7.1。
-- 命中时直接返回 response_code / response_message / response_data，不重复执行 handler。
-- 默认 5 min 窗口，application 可按 service_id / route 配置更长（最大 24h）。
-- response_data 是 wire TransferResponse.data 的 base64 字符串副本（spec §10 raw ≤64KB；
-- neton-database KSP 当前不支持 ByteArray? 列映射，base64 是最便宜的存储路径）。

CREATE TABLE IF NOT EXISTS privchat_transfer_idempotency (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    client_request_id VARCHAR(128) NOT NULL,
    service_id BIGINT,
    response_code INT NOT NULL,
    response_message VARCHAR(512),
    response_data TEXT,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privchat_transfer_idempotency_key
    ON privchat_transfer_idempotency(user_id, channel_id, client_request_id);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_idempotency_expires
    ON privchat_transfer_idempotency(expires_at);

-- ===================
-- privchat_transfer_audit_log
-- ===================
--
-- spec dispatch §9。每条记录必带 service_id / service_name / route 三件套，
-- 索引覆盖最常见的两个查询：(service, route, time) 失败率聚合、
-- (user, channel, time) 用户操作链路；trace_id 单独索引用于跨链路追踪。

CREATE TABLE IF NOT EXISTS privchat_transfer_audit_log (
    id BIGSERIAL PRIMARY KEY,
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
);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_service_route_created
    ON privchat_transfer_audit_log(service_id, route, created_at);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_user_channel_created
    ON privchat_transfer_audit_log(user_id, channel_id, created_at);
CREATE INDEX IF NOT EXISTS idx_privchat_transfer_audit_trace
    ON privchat_transfer_audit_log(trace_id);
