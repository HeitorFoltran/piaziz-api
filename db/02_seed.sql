INSERT INTO servico (nome) VALUES
    ('Psicologia'),
    ('Assistência Social'),
    ('Jurídico'),
    ('Saúde');

INSERT INTO profissional (nome, cpf, carteira_profissional, servico_id, email, senha_hash, role) VALUES
    ('Dra. Ana Beatriz',     '000.000.000-99', 'CRP-0001',
        (SELECT id FROM servico WHERE nome = 'Psicologia'),
        'ana.beatriz@azizaidhub.local', '$2a$10$LO4.a.Z0V1KLHfMvdXr3Y.L.Kb3oGVuW0lUpjZaz5K.IJbeDxMybW', 'PADRAO'),
    ('Carlos Mendes',        '000.000.000-88', 'CRESS-0002',
        (SELECT id FROM servico WHERE nome = 'Assistência Social'),
        'carlos.mendes@azizaidhub.local', '$2a$10$LO4.a.Z0V1KLHfMvdXr3Y.L.Kb3oGVuW0lUpjZaz5K.IJbeDxMybW', 'PADRAO'),
    ('Dra. Fernanda Costa',  '000.000.000-77', 'OAB-0003',
        (SELECT id FROM servico WHERE nome = 'Jurídico'),
        'fernanda.costa@azizaidhub.local', '$2a$10$LO4.a.Z0V1KLHfMvdXr3Y.L.Kb3oGVuW0lUpjZaz5K.IJbeDxMybW', 'PADRAO'),
    ('Estagiário Demo',      '000.000.000-66', NULL, NULL,
        'estagiario@azizaidhub.local', '$2a$10$LO4.a.Z0V1KLHfMvdXr3Y.L.Kb3oGVuW0lUpjZaz5K.IJbeDxMybW', 'ESTAGIARIO');

INSERT INTO ficha (codigo_ficha, numero_caso, nome, cpf, idade, telefone,
                   estado_civil, pessoas_dependentes, idade_filhos, nivel_seguranca,
                   tipo_moradia, tipo_moradia_outra_descricao, qtd_moradores, qtd_filhos,
                   onde_moram_filhos, supervisao_filhos,
                   data_criacao, data_atualizacao, status) VALUES
    ('F-0001', '2024/001234', 'Maria Silva',     '123.456.789-01', 34, '(45) 99999-0001',
        'Casada',         2, '7, 12',   2, 'CASA_PROPRIA', NULL, 3, 2, 'COMIGO', 'SIM',
        (CURRENT_DATE - INTERVAL '5 months') + TIME '09:00',
        (CURRENT_DATE - INTERVAL '3 days')   + TIME '09:00', 'ATIVO'),

    ('F-0002', '2024/001567', 'Ana Oliveira',    '987.654.321-15', 29, '(45) 99999-0015',
        'Solteira',       1, '4',       1, 'ALUGADA',      NULL, 2, 1, 'COMIGO', 'NAO',
        (CURRENT_DATE - INTERVAL '4 months') + TIME '09:00',
        (CURRENT_DATE - INTERVAL '6 days')   + TIME '09:00', 'ATIVO'),

    ('F-0003', '2024/001890', 'Juliana Santos',  '456.789.123-23', 41, '(45) 99999-0023',
        'Divorciada',     3, '5, 9, 14', 3, 'CEDIDA',      NULL, 4, 3, 'COMIGO', 'NAO_PRECISA',
        (CURRENT_DATE - INTERVAL '3 months') + TIME '09:00',
        (CURRENT_DATE - INTERVAL '8 days')   + TIME '09:00', 'ATIVO'),

    ('F-0004', '2024/002100', 'Carla Souza',     '321.654.987-44', 37, '(45) 99999-0044',
        'União estável',  2, '2, 8',    2, 'ABRIGO',       NULL, 5, 2, 'COMIGO', 'SIM',
        (CURRENT_DATE - INTERVAL '2 months') + TIME '09:00',
        (CURRENT_DATE - INTERVAL '40 days')  + TIME '09:00', 'ATIVO'),

    ('F-0005', '2024/002345', 'Fernanda Lima',   '654.321.789-78', 26, '(45) 99999-0078',
        'Solteira',       0, '',        0, 'OUTRO',        'Dividindo quarto com amiga', 1, 0, 'NAO_TEM_FILHOS', NULL,
        (CURRENT_DATE - INTERVAL '1 month')  + TIME '09:00',
        (CURRENT_DATE - INTERVAL '12 days')  + TIME '09:00', 'ATIVO'),

    ('F-0006', '2024/002567', 'Beatriz Rocha',   '789.123.456-32', 33, '(45) 99999-0032',
        'Casada',         1, '6',       1, 'ALUGADA',      NULL, 3, 1, 'COMIGO', 'NAO',
        (CURRENT_DATE - INTERVAL '4 days')   + TIME '09:00',
        (CURRENT_DATE - INTERVAL '1 day')    + TIME '09:00', 'ATIVO'),

    ('F-0007', '2024/002789', 'Patrícia Gomes',  '147.258.369-56', 45, '(45) 99999-0056',
        'Viúva',          2, '10, 16',  2, 'CASA_PROPRIA', NULL, 3, 2, 'COMIGO', 'NAO_PRECISA',
        (CURRENT_DATE - INTERVAL '2 days')   + TIME '09:00',
        (CURRENT_DATE - INTERVAL '2 days')   + TIME '09:00', 'ATIVO');

INSERT INTO ficha_vaga_necessaria (ficha_id, vaga_necessaria) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'ESCOLA'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), 'CRECHE'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), 'CRECHE'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), 'ESCOLA'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0006'), 'ESCOLA');

INSERT INTO ficha_necessidade_imediata (ficha_id, necessidade_imediata) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'APOIO_PSICOLOGICO'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'INFORMACOES_DIREITOS'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), 'APOIO_TRABALHO'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), 'ATENDIMENTO_ASSISTENCIA_SOCIAL'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0003'), 'INFORMACOES_DIREITOS'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), 'APOIO_MORADIA'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), 'APOIO_TRABALHO'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0005'), 'APOIO_PSICOLOGICO'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0005'), 'APOIO_MORADIA'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0006'), 'ATENDIMENTO_ASSISTENCIA_SOCIAL'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0007'), 'ATENDIMENTO_SAUDE');

INSERT INTO encaminhamento (ficha_id, servico_id, categoria, profissional, data_encaminhamento, data_retorno, descricao) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), (SELECT id FROM servico WHERE nome = 'Jurídico'), 'OUTRO', 'Dra. Fernanda Costa',
        CURRENT_DATE - INTERVAL '3 months',
        CURRENT_DATE - INTERVAL '3 months' + INTERVAL '14 days',
        'Orientação sobre medida protetiva de urgência.'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), (SELECT id FROM servico WHERE nome = 'Assistência Social'), 'ASSISTENCIA_SOCIAL', 'Carlos Mendes',
        CURRENT_DATE - INTERVAL '20 days',
        CURRENT_DATE - INTERVAL '6 days',
        'Verificação de situação habitacional e rede de apoio.'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), (SELECT id FROM servico WHERE nome = 'Psicologia'), 'SAUDE_MENTAL', 'Dra. Ana Beatriz',
        CURRENT_DATE - INTERVAL '5 days',
        CURRENT_DATE + INTERVAL '9 days',
        'Encaminhamento para avaliação psicológica inicial.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), (SELECT id FROM servico WHERE nome = 'Psicologia'), 'SAUDE_MENTAL', 'Dra. Ana Beatriz',
        CURRENT_DATE - INTERVAL '2 months',
        CURRENT_DATE - INTERVAL '2 months' + INTERVAL '14 days',
        'Acompanhamento psicológico semanal.'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), (SELECT id FROM servico WHERE nome = 'Assistência Social'), 'ASSISTENCIA_SOCIAL', 'Carlos Mendes',
        CURRENT_DATE - INTERVAL '10 days',
        NULL,
        'Inclusão em programa de auxílio social. Aguardando retorno do CRAS.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0003'), (SELECT id FROM servico WHERE nome = 'Jurídico'), 'OUTRO', 'Dra. Fernanda Costa',
        CURRENT_DATE - INTERVAL '15 days',
        CURRENT_DATE + INTERVAL '5 days',
        'Acompanhamento de processo de guarda dos filhos.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), (SELECT id FROM servico WHERE nome = 'Saúde'), 'SAUDE_GERAL', 'Equipe UBS Central',
        CURRENT_DATE - INTERVAL '45 days',
        CURRENT_DATE - INTERVAL '30 days',
        'Encaminhamento para acompanhamento de saúde geral.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0005'), (SELECT id FROM servico WHERE nome = 'Psicologia'), 'SAUDE_MENTAL', 'Dra. Ana Beatriz',
        CURRENT_DATE - INTERVAL '12 days',
        CURRENT_DATE + INTERVAL '2 days',
        'Primeira avaliação psicológica.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0006'), (SELECT id FROM servico WHERE nome = 'Assistência Social'), 'ASSISTENCIA_SOCIAL', 'Carlos Mendes',
        CURRENT_DATE - INTERVAL '3 days',
        NULL,
        'Avaliação socioeconômica inicial. Aguardando retorno.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0007'), (SELECT id FROM servico WHERE nome = 'Saúde'), 'SAUDE_MENTAL', 'Equipe UBS Central',
        CURRENT_DATE - INTERVAL '2 days',
        CURRENT_DATE + INTERVAL '12 days',
        'Encaminhamento para atendimento de saúde mental.');

INSERT INTO interacao (ficha_id, autor, data_interacao, texto) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'Promotora Silva',
        (CURRENT_DATE - INTERVAL '4 days') + TIME '10:00',
        'Paciente relatou melhora após início do acompanhamento psicológico. Manter encaminhamentos.'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'Assistente Social',
        (CURRENT_DATE - INTERVAL '8 days') + TIME '15:30',
        'Realizada visita domiciliar. Situação habitacional estável.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), 'Promotora Silva',
        (CURRENT_DATE - INTERVAL '6 days') + TIME '09:15',
        'Encaminhada ao CRAS. Acompanhar evolução do benefício.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0003'), 'Promotora Silva',
        (CURRENT_DATE - INTERVAL '8 days') + TIME '14:00',
        'Audiência marcada. Documentação reunida.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), 'Assistente Social',
        (CURRENT_DATE - INTERVAL '40 days') + TIME '11:00',
        'Acolhida em abrigo temporário. Necessita acompanhamento contínuo.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0005'), 'Promotora Silva',
        (CURRENT_DATE - INTERVAL '12 days') + TIME '16:45',
        'Primeiro atendimento realizado. Caso em fase inicial.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0006'), 'Assistente Social',
        (CURRENT_DATE - INTERVAL '1 day') + TIME '10:30',
        'Cadastro realizado. Encaminhada para avaliação.'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0007'), 'Promotora Silva',
        (CURRENT_DATE - INTERVAL '2 days') + TIME '13:00',
        'Ficha aberta. Caso encaminhado para a rede de saúde.');

INSERT INTO avaliacao_socioeconomica (
    ficha_id, tem_renda, valor_renda, pessoas_dependem_renda, origem_renda, trabalho_formal, renda_suficiente,
    trabalhando_atualmente, onde_trabalha, problema_saude_atrapalha_trabalho, problema_saude_qual,
    situacao_familiar_atrapalha_trabalho, situacao_familiar_qual, deseja_trabalhar, periodo_desejado,
    sabe_ler, nivel_escrita, nivel_escolaridade, escolaridade_detalhe, fez_curso_profissionalizante,
    curso_profissionalizante_qual, deseja_auxilio_ceebja, deseja_curso_senai, area_curso_senai,
    tem_rede_apoio, precisa_ajuda_moradia, tem_o_que_comer, acompanhamento_medico,
    precisa_ajuda_tratamento_medico, uso_continuo_medicamento, medicamento_quais, acesso_medicamentos
) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), TRUE, 1800.00, 2, 'Salário CLT em confecção', TRUE, TRUE,
        TRUE, 'Confecção local', FALSE, NULL,
        FALSE, NULL, FALSE, NULL,
        TRUE, 'SIM', 'MEDIO', NULL, FALSE,
        NULL, FALSE, FALSE, NULL,
        TRUE, FALSE, TRUE, TRUE,
        FALSE, FALSE, NULL, TRUE),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), FALSE, NULL, 1, NULL, FALSE, FALSE,
        FALSE, NULL, FALSE, NULL,
        TRUE, 'Não tem com quem deixar a filha', TRUE, 'MEIO_PERIODO',
        TRUE, 'SIM', 'FUNDAMENTAL', NULL, FALSE,
        NULL, TRUE, FALSE, NULL,
        FALSE, TRUE, TRUE, FALSE,
        FALSE, FALSE, NULL, TRUE),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0004'), FALSE, NULL, 2, NULL, FALSE, FALSE,
        FALSE, NULL, FALSE, NULL,
        FALSE, NULL, TRUE, NULL,
        TRUE, 'SIM', 'FUNDAMENTAL', NULL, FALSE,
        NULL, FALSE, TRUE, 'Corte e costura',
        FALSE, TRUE, FALSE, FALSE,
        FALSE, FALSE, NULL, FALSE);

INSERT INTO historico_atendimento (
    ficha_id, ja_procurou_servico, servico_procurado_qual_onde, em_fila_espera, fila_espera_qual,
    ja_pediu_ajuda_justica_policia, justica_policia_qual, como_foi_atendimento, resolveu_situacao, reacao_agressor
) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), TRUE, 'CRAS do bairro, há 2 anos', FALSE, NULL,
        TRUE, 'Boletim de ocorrência registrado', 'Atendimento demorado, mas resolutivo.', TRUE,
        'Ficou alterado, mas não voltou a procurá-la.'),
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0002'), FALSE, NULL, FALSE, NULL,
        FALSE, NULL, NULL, NULL, NULL);

INSERT INTO acolhimento_equipe (
    ficha_id, numero_processo_mpu, data_reuniao_acolhimento, servidor_responsavel,
    tipo_violencia_outra_descricao, frequencia_violencia, medidas_protetivas_anteriores, ameacas_relatadas,
    necessidade_atendimento_medico_imediato, acompanhamento_saude_mental_em_curso, acompanhamento_saude_mental_local,
    dependente_sofreu_violencia, dependente_precisa_auxilio_medico, categoria_classificacao,
    observacoes_relevantes, responsavel_acolhimento_juridico
) VALUES
    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0001'), 'MPU-2024/000123', CURRENT_DATE - INTERVAL '5 months', 'Promotora Silva',
        NULL, 'RECORRENTE', TRUE, 'Ameaças de morte relatadas em ocorrências anteriores.',
        FALSE, TRUE, 'CAPS Central',
        FALSE, FALSE, 'CATEGORIA_1',
        'Caso estável, acompanhamento de rotina.', 'Dra. Fernanda Costa'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0003'), 'MPU-2024/000456', CURRENT_DATE - INTERVAL '3 months', 'Promotora Silva',
        NULL, 'RECORRENTE', FALSE, NULL,
        FALSE, FALSE, NULL,
        FALSE, FALSE, 'CATEGORIA_2',
        'Processo de guarda em andamento.', 'Dra. Fernanda Costa'),

    ((SELECT id FROM ficha WHERE codigo_ficha = 'F-0007'), 'MPU-2024/000789', CURRENT_DATE - INTERVAL '2 days', 'Promotora Silva',
        NULL, 'EPISODIO_UNICO', FALSE, NULL,
        TRUE, FALSE, NULL,
        FALSE, FALSE, 'CATEGORIA_3',
        'Caso recém-aberto, aguardando primeira avaliação de saúde mental.', 'Dra. Fernanda Costa');

INSERT INTO acolhimento_tipo_violencia (acolhimento_id, tipo_violencia) VALUES
    ((SELECT id FROM acolhimento_equipe WHERE ficha_id = (SELECT id FROM ficha WHERE codigo_ficha = 'F-0001')), 'FISICA'),
    ((SELECT id FROM acolhimento_equipe WHERE ficha_id = (SELECT id FROM ficha WHERE codigo_ficha = 'F-0001')), 'PSICOLOGICA'),
    ((SELECT id FROM acolhimento_equipe WHERE ficha_id = (SELECT id FROM ficha WHERE codigo_ficha = 'F-0003')), 'PATRIMONIAL'),
    ((SELECT id FROM acolhimento_equipe WHERE ficha_id = (SELECT id FROM ficha WHERE codigo_ficha = 'F-0003')), 'MORAL'),
    ((SELECT id FROM acolhimento_equipe WHERE ficha_id = (SELECT id FROM ficha WHERE codigo_ficha = 'F-0007')), 'PSICOLOGICA');
