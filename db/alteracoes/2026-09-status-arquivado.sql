-- Status de acompanhamento: ENCERRADO passa a se chamar ARQUIVADO.
-- Idempotente: pode rodar de novo sem efeito.
-- Rodar DEPOIS que o código novo estiver no ar (Render depois do merge; VPS logo depois do
-- deploy da tag). O código novo lê ENCERRADO e ARQUIVADO (StatusFichaConverter), então não
-- quebra antes do script. O código antigo NÃO lê ARQUIVADO: rodar antes do deploy deixaria as
-- fichas arquivadas ilegíveis até o deploy terminar.
-- Enquanto o script não roda, arquivar uma ficha falha no CHECK antigo. Rodar logo após o deploy.
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.
--
-- Não é aditivo: voltar para uma tag anterior exige desfazer antes, com
--   ALTER TABLE ficha DROP CONSTRAINT IF EXISTS ck_ficha_status;
--   UPDATE ficha SET status = 'ENCERRADO' WHERE status = 'ARQUIVADO';
--   ALTER TABLE ficha ADD CONSTRAINT ck_ficha_status CHECK (status IN ('ATIVO', 'PAUSADO', 'ENCERRADO'));

BEGIN;

ALTER TABLE ficha DROP CONSTRAINT IF EXISTS ck_ficha_status;

UPDATE ficha SET status = 'ARQUIVADO' WHERE status = 'ENCERRADO';

ALTER TABLE ficha
    ADD CONSTRAINT ck_ficha_status CHECK (status IN ('ATIVO', 'PAUSADO', 'ARQUIVADO'));

COMMIT;
