-- Relatórios analíticos: data de criação do histórico de atendimento.
-- Aditivo e idempotente: pode rodar em bancos que já existem sem apagar dados.
-- Linhas antigas ficam NULL; a tela de relatórios mostra estado vazio para elas.
-- Rodar ANTES de subir o código que traz este script.
-- Nunca rodar db/01_schema.sql para aplicar esta mudança: ele começa com DROP TABLE ... CASCADE.

ALTER TABLE historico_atendimento ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP;
