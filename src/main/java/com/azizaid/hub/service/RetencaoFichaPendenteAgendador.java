package com.azizaid.hub.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Classe separada do RetencaoFichaPendenteService de propósito: chamar aplicarRetencao() de
// dentro do próprio service pularia o proxy e o @Transactional não valeria.
@Component
public class RetencaoFichaPendenteAgendador {

    private final RetencaoFichaPendenteService retencaoFichaPendenteService;

    public RetencaoFichaPendenteAgendador(RetencaoFichaPendenteService retencaoFichaPendenteService) {
        this.retencaoFichaPendenteService = retencaoFichaPendenteService;
    }

    @Scheduled(cron = "${retencao.ficha-pendente.cron}", zone = "America/Sao_Paulo")
    public void executar() {
        retencaoFichaPendenteService.aplicarRetencao();
    }
}
