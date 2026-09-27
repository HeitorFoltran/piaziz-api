package com.azizaid.hub.integration;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.service.RetencaoFichaPendenteService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetencaoFichaPendenteIT extends PostgresTestContainerConfig {

    @Autowired
    RetencaoFichaPendenteService retencaoFichaPendenteService;

    @Autowired
    FichaPendenteRepository fichaPendenteRepository;

    @Autowired
    ConviteFichaRepository conviteFichaRepository;

    @Autowired
    ProfissionalRepository profissionalRepository;

    // Longe da borda de 30/90 dias de propósito: o now() do teste e o do service diferem por
    // milissegundos, e testar a borda exata deixaria o teste instável.
    @Test
    void aplicarRetencao_apagaRevisadasForaDoPrazoEMantemORestante() {
        LocalDateTime agora = LocalDateTime.now();
        Long revisorId = persistirProfissional();

        Long aprovadaAntiga = persistir(StatusFichaPendente.APROVADA, agora.minusDays(40), agora.minusDays(31), revisorId);
        Long aprovadaRecente = persistir(StatusFichaPendente.APROVADA, agora.minusDays(35), agora.minusDays(29), revisorId);
        Long rejeitadaAntiga = persistir(StatusFichaPendente.REJEITADA, agora.minusDays(100), agora.minusDays(91), revisorId);
        Long rejeitadaRecente = persistir(StatusFichaPendente.REJEITADA, agora.minusDays(95), agora.minusDays(89), revisorId);
        Long pendenteAntiga = persistir(StatusFichaPendente.PENDENTE, agora.minusDays(400), null, null);

        int apagadas = retencaoFichaPendenteService.aplicarRetencao();

        assertThat(apagadas).isEqualTo(2);
        assertThat(fichaPendenteRepository.existsById(aprovadaAntiga)).isFalse();
        assertThat(fichaPendenteRepository.existsById(rejeitadaAntiga)).isFalse();
        assertThat(fichaPendenteRepository.existsById(aprovadaRecente)).isTrue();
        assertThat(fichaPendenteRepository.existsById(rejeitadaRecente)).isTrue();
        assertThat(fichaPendenteRepository.existsById(pendenteAntiga)).isTrue();
    }

    private Long persistirProfissional() {
        return profissionalRepository.save(Profissional.builder()
                .nome("Revisora Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("padrao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build()).getId();
    }

    private Long persistir(StatusFichaPendente status, LocalDateTime dataSubmissao,
                           LocalDateTime dataRevisao, Long revisadoPorId) {
        ConviteFicha convite = conviteFichaRepository.save(ConviteFicha.builder()
                .tokenHash(UUID.randomUUID().toString().replace("-", ""))
                .criadoPorId(revisadoPorId != null ? revisadoPorId : persistirProfissional())
                .dataExpiracao(dataSubmissao.plusDays(7))
                .status(StatusConvite.USADO)
                .usadoEm(dataSubmissao)
                .build());

        return fichaPendenteRepository.save(FichaPendente.builder()
                .conviteId(convite.getId())
                .nome("Envio Teste")
                .cpf("52998224725")
                .dataSubmissao(dataSubmissao)
                .status(status)
                .revisadoPorId(revisadoPorId)
                .dataRevisao(dataRevisao)
                .build()).getId();
    }
}
