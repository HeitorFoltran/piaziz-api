package com.azizaid.hub.service;

import com.azizaid.hub.model.LeituraAuditLog;
import com.azizaid.hub.repository.LeituraAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LeituraAuditService {

    // O React Query refaz a busca do detalhe toda vez que a aba volta ao foco. Sem esta janela,
    // um caso aberto numa aba geraria dezenas de linhas por hora.
    static final int JANELA_DEDUPLICACAO_MINUTOS = 10;

    private final LeituraAuditLogRepository leituraAuditLogRepository;

    public LeituraAuditService(LeituraAuditLogRepository leituraAuditLogRepository) {
        this.leituraAuditLogRepository = leituraAuditLogRepository;
    }

    // Transação própria: quem chama está numa transação read-only, e um INSERT nela falha no Postgres.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long fichaId, Long profissionalId) {
        LocalDateTime desde = LocalDateTime.now().minusMinutes(JANELA_DEDUPLICACAO_MINUTOS);
        if (leituraAuditLogRepository.existsByProfissionalIdAndFichaIdAndTimestampAfter(profissionalId, fichaId, desde)) {
            return;
        }
        leituraAuditLogRepository.save(LeituraAuditLog.builder()
                .fichaId(fichaId)
                .profissionalId(profissionalId)
                .build());
    }
}
