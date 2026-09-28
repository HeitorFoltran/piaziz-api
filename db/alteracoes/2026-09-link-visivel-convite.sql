-- Link do convite visível na lista: token cifrado (AES-GCM, chave em CONVITE_TOKEN_KEY), só para exibição.
-- A validação do link público continua só pelo token_hash.
-- Aditivo e idempotente: pode rodar em bancos que já existem (local, Supabase, VPS) sem apagar dados.
-- Rodar ANTES de mergear o PR que traz este script (auto-deploy do Render a partir de main) e, na VPS,
-- antes do deploy da tag.
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.
-- Sem backfill: convites que já existem só têm o hash e aparecem na lista sem o link.

ALTER TABLE convite_ficha
    ADD COLUMN IF NOT EXISTS token_cifrado TEXT;
