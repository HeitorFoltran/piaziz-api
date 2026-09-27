package com.azizaid.hub.service;

import com.azizaid.hub.config.RetencaoProperties;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.FichaPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Apaga envios do formulário público já revisados depois do prazo (LGPD). PENDENTE nunca é
// apagada: é um pedido de ajuda que ninguém revisou ainda.
@Service
public class RetencaoFichaPendenteService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoFichaPendenteService.class);

    private final FichaPendenteRepository fichaPendenteRepository;
    private final RetencaoProperties properties;

    public RetencaoFichaPendenteService(FichaPendenteRepository fichaPendenteRepository,
                                        RetencaoProperties properties) {
        this.fichaPendenteRepository = fichaPendenteRepository;
        this.properties = properties;
    }

    @Transactional
    public int aplicarRetencao() {
        LocalDateTime agora = LocalDateTime.now();

        int aprovadas = fichaPendenteRepository.apagarRevisadasAntesDe(
                StatusFichaPendente.APROVADA, agora.minusDays(properties.aprovadaDias()));
        int rejeitadas = fichaPendenteRepository.apagarRevisadasAntesDe(
                StatusFichaPendente.REJEITADA, agora.minusDays(properties.rejeitadaDias()));

        // Só números: nunca id de convite, nome, CPF ou outro dado da linha.
        log.info("Retenção ficha_pendente: {} aprovada(s) e {} rejeitada(s) apagada(s)", aprovadas, rejeitadas);
        return aprovadas + rejeitadas;
    }
}
