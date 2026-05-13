-- =============================================
-- application V009 — seed bot admin menu (MySQL)
-- =============================================
-- See sql/postgresql/V009__seed_bot_menu.sql for context.

INSERT IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (704, '机器人管理', 'privchat:bot:view', 2, 700, 'bot', 'privchat/bot/index', 'ant-design:robot-outlined', 4, 1, 0, 0);

INSERT IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (7040, '机器人操作', 'privchat:bot:operate', 3, 704, '', NULL, '', 1, 1, 0, 0);
