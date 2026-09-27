package com.azizaid.hub.service;

import com.azizaid.hub.model.ContaAuditLog;
import com.azizaid.hub.model.enums.AcaoConta;
import com.azizaid.hub.repository.ContaAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaAuditLogService {

    private static final int TAMANHO_DETALHE = 300;

    private final ContaAuditLogRepository repository;

    public ContaAuditLogService(ContaAuditLogRepository repository) {
        this.repository = repository;
    }

    // Mesma transação da ação (ao contrário do AuthAuditLogService): se a ação falha, não houve
    // nada para registrar. O detalhe nunca pode conter senha ou hash.
    @Transactional
    public void registrar(Long profissionalId, Long autorId, AcaoConta acao, String detalhe) {
        repository.save(ContaAuditLog.builder()
                .profissionalId(profissionalId)
                .autorId(autorId)
                .acao(acao)
                .detalhe(detalhe != null && detalhe.length() > TAMANHO_DETALHE
                        ? detalhe.substring(0, TAMANHO_DETALHE)
                        : detalhe)
                .build());
    }
}
