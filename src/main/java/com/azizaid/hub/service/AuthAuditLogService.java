package com.azizaid.hub.service;

import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditLogService {

    private final AuthAuditLogRepository repository;

    public AuthAuditLogService(AuthAuditLogRepository repository) {
        this.repository = repository;
    }

    // Transação própria: AuthService.login lança exceção logo depois de registrar uma falha, e o
    // rollback da transação dele não pode levar a linha junto (é dela que sai o bloqueio por conta).
    // profissionalId: a conta a que o identificador corresponde, ou null se não corresponde a nenhuma.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String identificador, Long profissionalId, boolean sucesso, String ipAddress,
                          String motivoFalha) {
        repository.save(AuthAuditLog.builder()
                .emailTentado(identificador)
                .profissionalId(profissionalId)
                .sucesso(sucesso)
                .ipAddress(ipAddress)
                .motivoFalha(motivoFalha)
                .build());
    }
}
