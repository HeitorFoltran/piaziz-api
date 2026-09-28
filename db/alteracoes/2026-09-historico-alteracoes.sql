-- Histórico de alterações completo: toda alteração num caso passa a ser gravada, não só as feitas
-- por outro profissional. acao = o que foi feito, detalhe = metadado (ex.: "Ativo -> Arquivado"),
-- nunca valor de campo. Linhas antigas ficam com acao NULL, lida como EDITOU. Sem backfill.
-- Aditivo e idempotente: pode rodar em bancos que já existem (local, Supabase, VPS) sem apagar dados,
-- e antes do merge sem afetar o código atual.
-- Rodar ANTES de mergear o PR que traz este script (auto-deploy do Render a partir de main) e, na VPS,
-- antes do deploy da tag.
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.

ALTER TABLE entity_audit_log
    ADD COLUMN IF NOT EXISTS acao    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS detalhe VARCHAR(120);
