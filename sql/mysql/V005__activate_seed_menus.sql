-- =============================================
-- application V005 — activate V002 seed menus
-- =============================================
--
-- See sql/postgresql/V005__activate_seed_menus.sql for context.

UPDATE system_menus SET status = 1 WHERE status = 0;
