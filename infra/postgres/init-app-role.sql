-- Ch.12 §2: a dedicated, non-superuser application role. Row-Level
-- Security's FORCE clause exists specifically to make RLS apply even to a
-- table's owning role — but it is powerless against a superuser, which
-- bypasses row security unconditionally regardless of FORCE. The
-- POSTGRES_USER bootstrap role this image creates (elemes) IS a
-- superuser, so every service must connect as this role instead, not the
-- bootstrap one, or the RLS policies in each service's migrations provide
-- zero actual protection.
--
-- CREATE (not just CONNECT) is required on the database because Postgres
-- 15+ no longer grants schema-creation rights to non-owner roles by
-- default — each service's Flyway `create-schemas: true` needs it.
create role elemes_app with login password 'elemes_app_local_dev';
grant create, connect on database elemes to elemes_app;
