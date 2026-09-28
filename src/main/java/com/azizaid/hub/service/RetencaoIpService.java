package com.azizaid.hub.service;

import com.azizaid.hub.config.RetencaoProperties;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Apaga só o IP (UPDATE ... SET ip = NULL); as linhas continuam. Não afeta o limite de login por
// conta, que olha só os últimos minutos, nem o rate limit por IP, que é em memória.
@Service
public class RetencaoIpService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoIpService.class);

    private final FichaPublicaAuditLogRepository fichaPublicaAuditLogRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final FichaPendenteRepository fichaPendenteRepository;
    private final RetencaoProperties properties;

    public RetencaoIpService(FichaPublicaAuditLogRepository fichaPublicaAuditLogRepository,
                             AuthAuditLogRepository authAuditLogRepository,
                             FichaPendenteRepository fichaPendenteRepository,
                             RetencaoProperties properties) {
        this.fichaPublicaAuditLogRepository = fichaPublicaAuditLogRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.fichaPendenteRepository = fichaPendenteRepository;
        this.properties = properties;
    }

    @Transactional
    public int limparIps() {
        LocalDateTime limite = LocalDateTime.now().minusDays(properties.ip().dias());

        int fichaPublica = fichaPublicaAuditLogRepository.limparIpAntesDe(limite);
        int auth = authAuditLogRepository.limparIpAntesDe(limite);
        int fichaPendente = fichaPendenteRepository.limparIpAntesDe(limite);

        // Só números, nunca o IP ou outro dado da linha.
        log.info("Retenção de IPs: {} linha(s) limpa(s) em ficha_publica_audit_log, {} em auth_audit_log, {} em ficha_pendente",
                fichaPublica, auth, fichaPendente);
        return fichaPublica + auth + fichaPendente;
    }
}
