-- Run only after Flyway V1-V8 have been applied by a separate migration role.
-- psql --set=ON_ERROR_STOP=1 -v app_db_password='SECRET' -U MIGRATOR -d reelnest -f ...
-- Choose a real random password. Do not commit the invocation or leak it via shell history.
-- This creates/re-keys a NON-owner runtime login. Never give it DDL rights.
SELECT format('CREATE ROLE reelnest_app LOGIN PASSWORD %L', :'app_db_password')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='reelnest_app') \gexec
SELECT format('ALTER ROLE reelnest_app LOGIN PASSWORD %L', :'app_db_password') \gexec
GRANT CONNECT ON DATABASE reelnest TO reelnest_app;
GRANT USAGE ON SCHEMA public TO reelnest_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO reelnest_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO reelnest_app;
-- Default privileges assume the migrator owns future tables.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO reelnest_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO reelnest_app;
-- Verify the runtime login cannot alter tables, create objects or assume migrator role.
-- Review role grants and test using the account before enabling AWS ECS services.
