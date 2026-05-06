-- =============================================
-- application V005 — activate V002 seed menus
-- =============================================
--
-- V002__init_data.sql seeded 106 rows into system_menus with status=0
-- (停用). PermissionLogic.getPermissionInfo only returns menus with
-- status=1, so the admin web sidebar was empty for super_admin even
-- though seed data was present.
--
-- This migration activates every menu still sitting at status=0. New
-- environments install V002 then immediately apply V005, so they end
-- up with the same active set as upgraded ones.

UPDATE system_menus SET status = 1 WHERE status = 0;
