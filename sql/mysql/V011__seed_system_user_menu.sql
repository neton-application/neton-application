-- =============================================
-- application V011 — seed system-user admin menu (MySQL)
-- =============================================
-- See sql/postgresql/V011__seed_system_user_menu.sql for context.

INSERT IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (705, '系统用户管理', 'privchat:system-user:view', 2, 700, 'system-user', 'privchat/systemUser/index', 'ant-design:safety-certificate-outlined', 5, 1, 0, 0);

INSERT IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7050, '系统用户操作', 'privchat:system-user:operate', 3, 705, '', NULL, '', 1, 1, 0, 0);
