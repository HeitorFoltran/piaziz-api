package com.azizaid.hub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Classe separada dos services de retenção de propósito: chamar os métodos @Transactional de
// dentro do próprio service pularia o proxy e a transação não valeria.
@Component
public class RetencaoAgendador {

    private static final Logger log = LoggerFactory.getLogger(RetencaoAgendador.class);

    private final RetencaoFichaPendenteService retencaoFichaPendenteService;
    private final RetencaoLeituraAuditService retencaoLeituraAuditService;
    private final RetencaoIpService retencaoIpService;
    private final RetencaoConviteService retencaoConviteService;

    public RetencaoAgendador(RetencaoFichaPendenteService retencaoFichaPendenteService,
                             RetencaoLeituraAuditService retencaoLeituraAuditService,
                             RetencaoIpService retencaoIpService,
                             RetencaoConviteService retencaoConviteService) {
        this.retencaoFichaPendenteService = retencaoFichaPendenteService;
        this.retencaoLeituraAuditService = retencaoLeituraAuditService;
        this.retencaoIpService = retencaoIpService;
        this.retencaoConviteService = retencaoConviteService;
    }

    // Cada etapa isolada: uma falha é logada e as outras rodam mesmo assim.
    @Scheduled(cron = "${retencao.cron}", zone = "America/Sao_Paulo")
    public void executar() {
        executarEtapa("ficha_pendente", retencaoFichaPendenteService::aplicarRetencao);
        executarEtapa("leitura_audit_log", retencaoLeituraAuditService::aplicarRetencao);
        executarEtapa("IPs", retencaoIpService::limparIps);
        executarEtapa("token_cifrado de convite", retencaoConviteService::limparTokensVencidos);
    }

    private void executarEtapa(String nome, Runnable etapa) {
        try {
            etapa.run();
        } catch (RuntimeException e) {
            log.error("Falha na retenção de {}", nome, e);
        }
    }
}
