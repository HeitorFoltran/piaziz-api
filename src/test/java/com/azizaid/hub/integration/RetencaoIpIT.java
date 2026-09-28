package com.azizaid.hub.integration;

import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.FichaPublicaAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.service.RetencaoIpService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetencaoIpIT extends PostgresTestContainerConfig {

    private static final String IP = "203.0.113.7";

    @Autowired
    RetencaoIpService retencaoIpService;

    @Autowired
    FichaPublicaAuditLogRepository fichaPublicaAuditLogRepository;

    @Autowired
    AuthAuditLogRepository authAuditLogRepository;

    @Autowired
    FichaPendenteRepository fichaPendenteRepository;

    @Autowired
    ConviteFichaRepository conviteFichaRepository;

    @Autowired
    ProfissionalRepository profissionalRepository;

    // Longe da borda de 90 dias de propósito, pelo mesmo motivo do RetencaoFichaPendenteIT.
    @Test
    void limparIps_apagaIpDasLinhasCom91DiasEMantemODasDe89_semApagarAsLinhas() {
        LocalDateTime agora = LocalDateTime.now();
        Long conviteId = persistirConvite();

        Long publicaAntiga = persistirFichaPublica(agora.minusDays(91));
        Long publicaRecente = persistirFichaPublica(agora.minusDays(89));
        Long authAntiga = persistirAuth(agora.minusDays(91));
        Long authRecente = persistirAuth(agora.minusDays(89));
        Long pendenteAntiga = persistirPendente(conviteId, agora.minusDays(91));
        Long pendenteRecente = persistirPendente(conviteId, agora.minusDays(89));

        int limpas = retencaoIpService.limparIps();

        assertThat(limpas).isEqualTo(3);
        assertThat(fichaPublicaAuditLogRepository.findById(publicaAntiga).orElseThrow().getIp()).isNull();
        assertThat(fichaPublicaAuditLogRepository.findById(publicaRecente).orElseThrow().getIp()).isEqualTo(IP);
        assertThat(authAuditLogRepository.findById(authAntiga).orElseThrow().getIpAddress()).isNull();
        assertThat(authAuditLogRepository.findById(authRecente).orElseThrow().getIpAddress()).isEqualTo(IP);
        assertThat(fichaPendenteRepository.findById(pendenteAntiga).orElseThrow().getIpSubmissao()).isNull();
        assertThat(fichaPendenteRepository.findById(pendenteRecente).orElseThrow().getIpSubmissao()).isEqualTo(IP);
    }

    private Long persistirFichaPublica(LocalDateTime timestamp) {
        return fichaPublicaAuditLogRepository.save(FichaPublicaAuditLog.builder()
                .ip(IP)
                .timestamp(timestamp)
                .resultado(ResultadoFichaPublica.TOKEN_INVALIDO)
                .build()).getId();
    }

    private Long persistirAuth(LocalDateTime timestamp) {
        return authAuditLogRepository.save(AuthAuditLog.builder()
                .emailTentado("retencao.ip." + UUID.randomUUID() + "@azizaidhub.local")
                .sucesso(false)
                .timestamp(timestamp)
                .ipAddress(IP)
                .build()).getId();
    }

    private Long persistirPendente(Long conviteId, LocalDateTime dataSubmissao) {
        return fichaPendenteRepository.save(FichaPendente.builder()
                .conviteId(conviteId)
                .nome("Envio Teste")
                .cpf("52998224725")
                .dataSubmissao(dataSubmissao)
                .ipSubmissao(IP)
                .status(StatusFichaPendente.PENDENTE)
                .build()).getId();
    }

    private Long persistirConvite() {
        Long criadorId = profissionalRepository.save(Profissional.builder()
                .nome("Criadora Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("padrao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build()).getId();
        return conviteFichaRepository.save(ConviteFicha.builder()
                .tokenHash(UUID.randomUUID().toString().replace("-", ""))
                .criadoPorId(criadorId)
                .dataExpiracao(LocalDateTime.now().plusDays(7))
                .status(StatusConvite.USADO)
                .build()).getId();
    }
}
