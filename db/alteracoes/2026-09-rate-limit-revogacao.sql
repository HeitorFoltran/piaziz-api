-- Rate limiting + revogação de sessão.
-- Aditivo e idempotente: pode rodar em bancos que já existem (local, Supabase) sem apagar dados.
-- Rodar ANTES de mergear o PR que traz este script (auto-deploy do Render a partir de main).
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.

ALTER TABLE profissional
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS sessoes_revogadas_em TIMESTAMPTZ;

DROP INDEX IF EXISTS idx_auth_audit_log_email;
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_email_timestamp ON auth_audit_log (email_tentado, timestamp);
