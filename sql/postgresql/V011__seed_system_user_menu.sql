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
