package com.azizaid.hub.service;

import com.azizaid.hub.model.FichaPublicaAuditLog;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FichaPublicaAuditLogService {

    private final FichaPublicaAuditLogRepository repository;

    public FichaPublicaAuditLogService(FichaPublicaAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long conviteId, String ip, ResultadoFichaPublica resultado) {
        repository.save(FichaPublicaAuditLog.builder()
                .conviteId(conviteId)
                .ip(ip)
                .resultado(resultado)
                .build());
    }
}
