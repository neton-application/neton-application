-- =============================================
-- neton-application 全部建表语句 (SQLite)
-- 包含: module-system + module-infra + provider
-- =============================================

-- ===================
-- module-system
-- ===================

CREATE TABLE IF NOT EXISTS system_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    nickname TEXT NOT NULL,
    email TEXT,
    mobile TEXT,
    avatar TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_users_username ON system_users(username);

CREATE TABLE IF NOT EXISTS system_roles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_roles_code ON system_roles(code);

CREATE TABLE IF NOT EXISTS system_menus (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    permission TEXT,
    type INTEGER NOT NULL DEFAULT 0,
    parent_id INTEGER NOT NULL DEFAULT 0,
    path TEXT,
    component TEXT,
    icon TEXT,
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS system_user_roles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_system_user_roles_user ON system_user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_system_user_roles_role ON system_user_roles(role_id);

CREATE TABLE IF NOT EXISTS system_role_menus (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id INTEGER NOT NULL,
    menu_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_system_role_menus_role ON system_role_menus(role_id);

CREATE TABLE IF NOT EXISTS system_depts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    parent_id INTEGER NOT NULL DEFAULT 0,
    sort INTEGER NOT NULL DEFAULT 0,
    leader_user_id INTEGER,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS system_posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS system_dict_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_dict_types_type ON system_dict_types(type);

CREATE TABLE IF NOT EXISTS system_dict_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dict_type TEXT NOT NULL,
    label TEXT NOT NULL,
    value TEXT NOT NULL,
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_system_dict_data_type ON system_dict_data(dict_type);

CREATE TABLE IF NOT EXISTS system_notices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS system_login_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    username TEXT,
    user_ip TEXT,
    user_agent TEXT,
    login_result INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_system_login_logs_user ON system_login_logs(user_id);

CREATE TABLE IF NOT EXISTS system_operate_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    module TEXT NOT NULL,
    name TEXT NOT NULL,
    operate_type INTEGER NOT NULL DEFAULT 0,
    request_method TEXT,
    request_url TEXT,
    request_params TEXT,
    response_result TEXT,
    user_ip TEXT,
    duration INTEGER NOT NULL DEFAULT 0,
    result_code INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_system_operate_logs_user ON system_operate_logs(user_id);

-- ===================
-- module-infra
-- ===================

CREATE TABLE IF NOT EXISTS infra_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT NOT NULL DEFAULT '',
    config_key TEXT NOT NULL,
    value TEXT NOT NULL,
    type INTEGER NOT NULL DEFAULT 0,
    name TEXT NOT NULL,
    remark TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_infra_configs_key ON infra_configs(config_key);

CREATE TABLE IF NOT EXISTS infra_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_id INTEGER,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    url TEXT,
    mime_type TEXT,
    size INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS infra_file_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    storage INTEGER NOT NULL DEFAULT 0,
    config TEXT NOT NULL DEFAULT '',
    master INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS infra_api_access_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    user_type INTEGER NOT NULL DEFAULT 0,
    application_name TEXT,
    request_method TEXT,
    request_url TEXT NOT NULL,
    request_params TEXT,
    response_body TEXT,
    user_ip TEXT,
    user_agent TEXT,
    operate_module TEXT,
    operate_name TEXT,
    operate_type INTEGER NOT NULL DEFAULT 0,
    begin_time INTEGER NOT NULL DEFAULT 0,
    end_time INTEGER NOT NULL DEFAULT 0,
    duration INTEGER NOT NULL DEFAULT 0,
    result_code INTEGER NOT NULL DEFAULT 0,
    result_msg TEXT,
    created_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS infra_api_error_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    user_type INTEGER NOT NULL DEFAULT 0,
    application_name TEXT,
    request_method TEXT,
    request_url TEXT NOT NULL,
    request_params TEXT,
    user_ip TEXT,
    user_agent TEXT,
    exception_name TEXT,
    exception_message TEXT,
    exception_stack_trace TEXT,
    process_status INTEGER NOT NULL DEFAULT 0,
    process_user_id INTEGER,
    process_time INTEGER,
    created_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS infra_jobs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    handler_name TEXT NOT NULL,
    handler_param TEXT,
    cron_expression TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    retry_interval INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS infra_job_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    handler_name TEXT NOT NULL,
    handler_param TEXT,
    execute_index INTEGER NOT NULL DEFAULT 1,
    begin_time INTEGER NOT NULL DEFAULT 0,
    end_time INTEGER,
    duration INTEGER,
    status INTEGER NOT NULL DEFAULT 0,
    result_msg TEXT,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_infra_job_logs_job ON infra_job_logs(job_id);

-- ===================
-- provider (messaging + social login + notification)
-- ===================

CREATE TABLE IF NOT EXISTS system_message_channels (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    type TEXT NOT NULL,
    config TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT,
    updated_at TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_message_channels_code ON system_message_channels(code);

CREATE TABLE IF NOT EXISTS system_message_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    content TEXT NOT NULL,
    params TEXT,
    channel_id INTEGER NOT NULL DEFAULT 0,
    type INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT,
    updated_at TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_message_templates_code ON system_message_templates(code);

CREATE TABLE IF NOT EXISTS system_message_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL,
    template_id INTEGER,
    template_code TEXT,
    receiver TEXT NOT NULL,
    content TEXT,
    params TEXT,
    send_status INTEGER NOT NULL DEFAULT 0,
    send_time TEXT,
    error_message TEXT,
    user_id INTEGER,
    user_type INTEGER NOT NULL DEFAULT 0,
    created_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_system_message_logs_channel ON system_message_logs(channel_id);
CREATE INDEX IF NOT EXISTS idx_system_message_logs_receiver ON system_message_logs(receiver);

CREATE TABLE IF NOT EXISTS system_social_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL DEFAULT 0,
    user_type INTEGER NOT NULL DEFAULT 0,
    social_type TEXT NOT NULL,
    open_id TEXT NOT NULL,
    token TEXT,
    raw_token_info TEXT,
    nickname TEXT,
    avatar TEXT,
    raw_user_info TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT,
    updated_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_system_social_users_user ON system_social_users(user_id, user_type);
CREATE INDEX IF NOT EXISTS idx_system_social_users_social ON system_social_users(social_type, open_id);

CREATE TABLE IF NOT EXISTS system_notification_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    type INTEGER NOT NULL DEFAULT 0,
    message_template_id INTEGER NOT NULL DEFAULT 0,
    params TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT,
    updated_at TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_system_notification_templates_code ON system_notification_templates(code);

CREATE TABLE IF NOT EXISTS system_notify_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL DEFAULT 0,
    user_type INTEGER NOT NULL DEFAULT 0,
    template_id INTEGER NOT NULL DEFAULT 0,
    template_code TEXT,
    template_type INTEGER NOT NULL DEFAULT 0,
    template_nickname TEXT,
    template_content TEXT,
    template_params TEXT,
    read_status INTEGER NOT NULL DEFAULT 0,
    read_time TEXT,
    created_at TEXT,
    updated_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_system_notify_messages_user ON system_notify_messages(user_id, user_type);
CREATE INDEX IF NOT EXISTS idx_system_notify_messages_read ON system_notify_messages(user_id, read_status);
