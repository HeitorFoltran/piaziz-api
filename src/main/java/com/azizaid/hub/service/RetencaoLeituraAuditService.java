package com.azizaid.hub.service;

import com.azizaid.hub.config.RetencaoProperties;
import com.azizaid.hub.repository.LeituraAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RetencaoLeituraAuditService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoLeituraAuditService.class);

    private final LeituraAuditLogRepository leituraAuditLogRepository;
    private final RetencaoProperties properties;

    public RetencaoLeituraAuditService(LeituraAuditLogRepository leituraAuditLogRepository,
                                       RetencaoProperties properties) {
        this.leituraAuditLogRepository = leituraAuditLogRepository;
        this.properties = properties;
    }

    @Transactional
    public int aplicarRetencao() {
        LocalDateTime limite = LocalDateTime.now().minusDays(properties.leituraAudit().dias());
        int apagadas = leituraAuditLogRepository.apagarAntesDe(limite);

        log.info("Retenção leitura_audit_log: {} linha(s) apagada(s)", apagadas);
        return apagadas;
    }
}
