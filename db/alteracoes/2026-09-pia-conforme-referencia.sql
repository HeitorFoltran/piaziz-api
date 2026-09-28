-- Lote 7: ficha PIA igual à referência em papel.
-- Encaminhamentos sugeridos no acolhimento (Parte B, item 8): um Sim/Não e um "Qual" por serviço,
-- mais um "Outro encaminhamento" livre. São sugestões da equipe, não encaminhamentos feitos (esses
-- ficam na tabela encaminhamento).
-- "O que eu preciso agora": ficha com "Outro, qual?" preenchido passa a ter OUTRO marcado. Antes do
-- lote 7 o front mandava só a descrição, sem OUTRO no conjunto.
-- Aditivo e idempotente: pode rodar em bancos que já existem (local, Supabase, produção) sem apagar dados.
-- Rodar ANTES do deploy da API (ddl-auto=none: as colunas precisam existir quando o código novo subir).
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.

BEGIN;

ALTER TABLE acolhimento_equipe
    ADD COLUMN IF NOT EXISTS sugere_saude_geral                  BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_saude_geral_qual             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_saude_mental                 BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_saude_mental_qual            VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_habitacao                    BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_habitacao_qual               VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_trabalho_emprego             BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_trabalho_emprego_qual        VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_assistencia_social           BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_assistencia_social_qual      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_assistencia_educacional      BOOLEAN,
    ADD COLUMN IF NOT EXISTS sugere_assistencia_educacional_qual VARCHAR(200),
    ADD COLUMN IF NOT EXISTS sugere_outro                        VARCHAR(300);

INSERT INTO ficha_necessidade_imediata (ficha_id, necessidade_imediata)
SELECT f.id, 'OUTRO' FROM ficha f
WHERE COALESCE(TRIM(f.necessidade_outra_descricao), '') <> ''
  AND NOT EXISTS (SELECT 1 FROM ficha_necessidade_imediata n
                  WHERE n.ficha_id = f.id AND n.necessidade_imediata = 'OUTRO');

COMMIT;
