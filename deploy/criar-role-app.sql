-- Role usado pela API em produção: só DML, sem DDL.
-- Rodar como o dono do banco (POSTGRES_USER), depois do db/01_schema.sql.
-- Senha: definir em seguida com \password azizaid_app (a mesma do DB_PASSWORD em api.env).
CREATE ROLE azizaid_app LOGIN;
GRANT CONNECT ON DATABASE azizaid_hub TO azizaid_app;
GRANT USAGE ON SCHEMA public TO azizaid_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO azizaid_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO azizaid_app;
-- Tabelas e sequences criadas depois (scripts de db/alteracoes/, rodados pelo mesmo dono) herdam os grants.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO azizaid_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO azizaid_app;
