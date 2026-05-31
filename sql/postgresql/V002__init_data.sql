-- seed data: 仅 INSERT...VALUES (纯 seed), 排除 INSERT...SELECT 数据迁移
-- V001 是 pg_dump 输出,顶部把 search_path 清空了。V002 不带 schema 前缀,
-- 必须先把 search_path 恢复到 public,否则 INSERT 找不到表。
SET search_path = public;

-- from V009__seed_bot_menu.sql
-- =============================================
-- application V009 — seed bot admin menu
-- spec 07-application/im-admin/ADMIN_BOT_SPEC §2
-- =============================================
--
-- 把 module-privchat/sql/postgresql/V003__seed_bot_menu.sql 的内容收纳进
-- application 主线 migration，确保新环境部署时自动加载（application
-- migrator 不扫描 module 仓库的 sql 目录）。
--
-- 「机器人管理」挂在「即时通讯」(parent_id=700) 下，sort=4。
-- 权限点（v1 粗粒度）：
--   privchat:bot:view     列表 / 详情查看
--   privchat:bot:operate  创建 / 编辑 / 启停 / 删除 / 菜单更新
--   privchat:bot:user_open  app 端用户开 bot 会话（仅 PrivChat app 调，**不**展示在后台菜单上）

INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (704, '机器人管理', 'privchat:bot:view', 2, 700, 'bot', 'privchat/bot/index', 'ant-design:robot-outlined', 4, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7040, '机器人操作', 'privchat:bot:operate', 3, 704, '', NULL, '', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- from V011__seed_system_user_menu.sql
-- =============================================
-- application V011 — seed system-user admin menu
-- =============================================
--
-- 「系统用户管理」独立菜单（user_type=1，没有 owner，未来可绑 service_id），
-- 与「机器人管理」(user_type=2，有 owner) 拆开。
--
-- v1 范围：菜单 + 列表骨架（复用 user 列表 + 固定 user_type=1 过滤）。
-- 不新建 privchat_system_user_profile 表；如未来需要绑 service_id，再加。

INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (705, '系统用户管理', 'privchat:system-user:view', 2, 700, 'system-user', 'privchat/systemUser/index', 'ant-design:safety-certificate-outlined', 5, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7050, '系统用户操作', 'privchat:system-user:operate', 3, 705, '', NULL, '', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- from V001__init_data.sql
-- =============================================
-- module-privchat 初始化数据 (PostgreSQL)
-- =============================================
--
-- IM 内核 admin 菜单 v1：仅 2 个主菜单（用户管理 / 群组管理），
-- 详情页 hideInMenu，由前端静态路由承载。系统消息 / 消息审计 /
-- 登录日志 / 影子封禁 v2 再加，避免 v1 铺满。
--
-- 与会员中心边界：
--   会员中心  = 业务面（积分 / 等级 / 标签 / 签到 / 钱包）
--   IM 用户管理 = 运维面（封号 / 踢设备 / 查消息 / 在线状态）
--   两边按 uid 互跳，不要交叉塞字段。
--
-- 权限粒度（v1 粗粒度，按钮鉴权先不拆）：
--   privchat:user:view     用户管理 list/detail/友/群/设备/登录/消息
--   privchat:user:operate  封号 / 踢设备 / bump session
--   privchat:group:view    群组管理 list/detail/成员/群消息
--   privchat:group:operate 解散群

-- =====================
-- 一级目录
-- =====================
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (700, '即时通讯', '', 1, 0, '/privchat', NULL, 'ant-design:message-outlined', 5, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- =====================
-- 二级菜单 (parent_id=700)
-- =====================
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (701, '用户管理', 'privchat:user:view', 2, 700, 'user', 'privchat/user/index', 'ant-design:user-outlined', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (702, '群组管理', 'privchat:group:view', 2, 700, 'group', 'privchat/group/index', 'ant-design:team-outlined', 2, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- =====================
-- 按钮权限（type=3）
-- =====================
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7010, '用户操作', 'privchat:user:operate', 3, 701, '', NULL, '', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7020, '群组操作', 'privchat:group:operate', 3, 702, '', NULL, '', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- from V002__seed_system_message_menu.sql
-- =============================================
-- module-privchat V002 — seed system-message menu
-- =============================================
--
-- v1 拍板：IM 内核 主菜单第三项「系统消息」（精准定向，不是广播）。
-- spec：
--   - 进 IM 消息流，不是 push notification
--   - SYSTEM_USER_ID 固定发送者
--   - 精准定向（用户 ≤ 100 / 群 ≤ 50 / channel ≤ 100）
--   - 不做全服广播 / 条件投递 / 异步任务
--
-- 权限点：privchat:system-message:send 走粗粒度，单一权限即可（v1 不细分）。

INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (703, '系统消息', 'privchat:system-message:send', 2, 700, 'system-message', 'privchat/systemMessage/index', 'ant-design:notification-outlined', 3, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- from V003__seed_bot_menu.sql
-- =============================================
-- module-privchat V003 — seed bot admin menu
-- spec 07-application/im-admin/ADMIN_BOT_SPEC §2
-- =============================================
--
-- 「机器人管理」独立菜单，挂在「即时通讯」(parent_id=700) 下，sort=4。
-- 权限点（v1 粗粒度）：
--   privchat:bot:view     列表 / 详情查看
--   privchat:bot:operate  创建 / 编辑 / 启停 / 删除 / 菜单更新
--   privchat:bot:user_open  app 端用户开 bot 会话（仅供 PrivChat app 调用，**不**展示在后台菜单上）

INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (704, '机器人管理', 'privchat:bot:view', 2, 700, 'bot', 'privchat/bot/index', 'ant-design:robot-outlined', 4, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
-- 按钮权限（type=3）
INSERT INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7040, '机器人操作', 'privchat:bot:operate', 3, 704, '', NULL, '', 1, 1, 0, 0)
ON CONFLICT (id) DO NOTHING;
