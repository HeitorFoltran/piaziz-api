ALTER TABLE entity_audit_log ADD COLUMN IF NOT EXISTS ficha_id BIGINT;

UPDATE entity_audit_log SET ficha_id = entidade_id
 WHERE tipo_entidade = 'Ficha' AND ficha_id IS NULL;

UPDATE entity_audit_log l SET ficha_id = s.ficha_id FROM avaliacao_socioeconomica s
 WHERE l.tipo_entidade = 'AvaliacaoSocioeconomica' AND s.id = l.entidade_id AND l.ficha_id IS NULL;

UPDATE entity_audit_log l SET ficha_id = s.ficha_id FROM historico_atendimento s
 WHERE l.tipo_entidade = 'HistoricoAtendimento' AND s.id = l.entidade_id AND l.ficha_id IS NULL;

UPDATE entity_audit_log l SET ficha_id = s.ficha_id FROM acolhimento_equipe s
 WHERE l.tipo_entidade = 'AcolhimentoEquipe' AND s.id = l.entidade_id AND l.ficha_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_entity_audit_log_ficha ON entity_audit_log (ficha_id, timestamp);
