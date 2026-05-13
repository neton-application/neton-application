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
