package com.azizaid.hub.service;

import com.azizaid.hub.repository.ConviteFichaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Apaga só o token_cifrado dos convites vencidos; a linha, o status e o token_hash continuam.
// Cobre o convite que vence sem ninguém abrir o link (ele fica ATIVO no banco até alguém tentar
// usar), para um backup antigo não guardar links que ainda dariam para ler.
@Service
public class RetencaoConviteService {

    private static final Logger log = LoggerFactory.getLogger(RetencaoConviteService.class);

    private final ConviteFichaRepository conviteFichaRepository;

    public RetencaoConviteService(ConviteFichaRepository conviteFichaRepository) {
        this.conviteFichaRepository = conviteFichaRepository;
    }

    @Transactional
    public int limparTokensVencidos() {
        int limpos = conviteFichaRepository.limparTokenCifradoVencidoAntesDe(LocalDateTime.now());

        log.info("Retenção de convites: token_cifrado apagado em {} convite(s) vencido(s)", limpos);
        return limpos;
    }
}
