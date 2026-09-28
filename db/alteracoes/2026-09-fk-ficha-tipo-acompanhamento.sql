-- Garante a FK ficha_tipo_acompanhamento -> ficha, que está no 01_schema.sql desde a criação da
-- tabela mas faltava em pelo menos um banco local. Sem ela, apagar uma ficha deixava vínculos
-- órfãos, e o tipo de acompanhamento aparecia como "em uso" sem estar em nenhum caso.
-- Idempotente: apaga só vínculos de fichas que não existem mais e cria a FK se ela não existir.
-- Pode rodar a qualquer momento, antes ou depois do merge.
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.

BEGIN;

DELETE FROM ficha_tipo_acompanhamento fta
WHERE NOT EXISTS (SELECT 1 FROM ficha f WHERE f.id = fta.ficha_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fta_ficha') THEN
        ALTER TABLE ficha_tipo_acompanhamento
            ADD CONSTRAINT fk_fta_ficha FOREIGN KEY (ficha_id) REFERENCES ficha (id) ON DELETE CASCADE;
    END IF;
END $$;

COMMIT;
