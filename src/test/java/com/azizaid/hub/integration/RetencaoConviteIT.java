package com.azizaid.hub.integration;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.service.RetencaoConviteService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetencaoConviteIT extends PostgresTestContainerConfig {

    @Autowired
    RetencaoConviteService retencaoConviteService;

    @Autowired
    ConviteFichaRepository conviteFichaRepository;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Test
    void limparTokensVencidos_apagaTokenDoConviteVencidoEMantemODoConviteNoPrazo() {
        Long criadorId = persistirCriador();
        LocalDateTime agora = LocalDateTime.now();
        Long vencido = persistirConvite(criadorId, agora.minusHours(1));
        Long noPrazo = persistirConvite(criadorId, agora.plusDays(3));

        int limpos = retencaoConviteService.limparTokensVencidos();

        assertThat(limpos).isGreaterThanOrEqualTo(1);
        ConviteFicha conviteVencido = conviteFichaRepository.findById(vencido).orElseThrow();
        assertThat(conviteVencido.getTokenCifrado()).isNull();
        assertThat(conviteVencido.getStatus()).isEqualTo(StatusConvite.ATIVO);
        assertThat(conviteVencido.getTokenHash()).isNotNull();
        assertThat(conviteFichaRepository.findById(noPrazo).orElseThrow().getTokenCifrado()).isEqualTo("cifrado");
    }

    private Long persistirCriador() {
        return profissionalRepository.save(Profissional.builder()
                .nome("Criadora Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("padrao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build()).getId();
    }

    private Long persistirConvite(Long criadorId, LocalDateTime dataExpiracao) {
        return conviteFichaRepository.save(ConviteFicha.builder()
                .tokenHash(UUID.randomUUID().toString().replace("-", ""))
                .tokenCifrado("cifrado")
                .criadoPorId(criadorId)
                .dataExpiracao(dataExpiracao)
                .status(StatusConvite.ATIVO)
                .build()).getId();
    }
}
