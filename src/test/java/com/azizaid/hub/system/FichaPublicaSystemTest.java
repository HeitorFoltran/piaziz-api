package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.request.FichaPendenteRejeitarRequestDTO;
import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.dto.response.FichaPublicaSubmissaoResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.FichaPublicaAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaPublicaSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    FichaPublicaAuditLogRepository fichaPublicaAuditLogRepository;

    private Profissional criarProfissional(PapelProfissional role, String email) {
        return profissionalRepository.save(Profissional.builder()
                .nome("Profissional Teste")
                .cpf("11122233396")
                .email(email)
                .senhaHash("hash-irrelevante-pro-teste")
                .role(role)
                .build());
    }

    private HttpHeaders headersAutenticados(Profissional profissional) {
        String token = jwtService.gerarToken(profissional.getId(), profissional.getEmail(), profissional.getRole());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private FichaRequestDTO construirFichaDto(String nome, String cpf) {
        return new FichaRequestDTO(null, nome, cpf, 28, "(45) 99999-0000",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private String criarConviteEExtrairToken(Profissional criador) {
        ConviteFichaResponseDTO convite = restTemplate.exchange(
                "/api/convites-ficha", HttpMethod.POST, new HttpEntity<Void>(headersAutenticados(criador)),
                ConviteFichaResponseDTO.class).getBody();
        return convite.linkCompleto().substring(convite.linkCompleto().lastIndexOf('/') + 1);
    }

    private FichaPendenteResponseDTO buscarPendentePorCpf(Profissional revisor, String cpf) {
        FichaPendenteResponseDTO[] pendentes = restTemplate.exchange(
                "/api/fichas-pendentes", HttpMethod.GET, new HttpEntity<Void>(headersAutenticados(revisor)),
                FichaPendenteResponseDTO[].class).getBody();
        return Arrays.stream(pendentes)
                .filter(p -> p.cpf().equals(cpf))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void fluxoCompleto_conviteSubmissaoEAprovacao() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        ResponseEntity<FichaPublicaStatusResponseDTO> statusAntes = restTemplate.getForEntity(
                "/api/ficha-publica/{token}/status", FichaPublicaStatusResponseDTO.class, token);
        assertThat(statusAntes.getBody().valido()).isTrue();

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                construirFichaDto("Maria da Silva", "48291365709"), null, null, "Relato de situação de risco");
        ResponseEntity<FichaPublicaSubmissaoResponseDTO> submissao = restTemplate.postForEntity(
                "/api/ficha-publica/{token}", submissaoDto, FichaPublicaSubmissaoResponseDTO.class, token);
        assertThat(submissao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<FichaPublicaStatusResponseDTO> statusDepois = restTemplate.getForEntity(
                "/api/ficha-publica/{token}/status", FichaPublicaStatusResponseDTO.class, token);
        assertThat(statusDepois.getBody().valido()).isFalse();
        assertThat(statusDepois.getBody().motivo()).isEqualTo("usado");

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor.system@azizaidhub.local");
        FichaPendenteResponseDTO pendente = buscarPendentePorCpf(revisor, "48291365709");
        assertThat(pendente.nome()).isEqualTo("Maria da Silva");
        assertThat(pendente.ficha().nome()).isEqualTo("Maria da Silva");
        assertThat(pendente.situacaoRelatada()).isEqualTo("Relato de situação de risco");

        ResponseEntity<FichaPendenteResponseDTO> aprovada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/aprovar", HttpMethod.POST,
                new HttpEntity<Void>(headersAutenticados(revisor)), FichaPendenteResponseDTO.class, pendente.id());
        assertThat(aprovada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprovada.getBody().status()).isEqualTo("APROVADA");
        assertThat(aprovada.getBody().fichaId()).isNotNull();
    }

    @Test
    void submeter_comAvaliacaoEHistoricoPreenchidos_aoAprovarPopulaSubRecursosNaFicha() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario4.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        AvaliacaoSocioeconomicaRequestDTO avaliacaoDto = new AvaliacaoSocioeconomicaRequestDTO(
                true, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
        HistoricoAtendimentoRequestDTO historicoDto = new HistoricoAtendimentoRequestDTO(
                true, "posto de saúde central", null, null, null, null, null, null, null);
        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                construirFichaDto("Beatriz Souza", "39053344705"), avaliacaoDto, historicoDto, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor4.system@azizaidhub.local");
        FichaPendenteResponseDTO pendente = buscarPendentePorCpf(revisor, "39053344705");
        assertThat(pendente.avaliacao().temRenda()).isTrue();
        assertThat(pendente.historico().servicoProcuradoQualOnde()).isEqualTo("posto de saúde central");

        FichaPendenteResponseDTO aprovada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/aprovar", HttpMethod.POST,
                new HttpEntity<Void>(headersAutenticados(revisor)), FichaPendenteResponseDTO.class, pendente.id())
                .getBody();

        ResponseEntity<FichaResponseDTO> fichaResposta = restTemplate.exchange(
                "/api/fichas/{id}", HttpMethod.GET, new HttpEntity<Void>(headersAutenticados(revisor)),
                FichaResponseDTO.class, aprovada.fichaId());
        assertThat(fichaResposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fichaResposta.getBody().avaliacaoSocioeconomica()).isNotNull();
        assertThat(fichaResposta.getBody().avaliacaoSocioeconomica().temRenda()).isTrue();
        assertThat(fichaResposta.getBody().historicoAtendimento()).isNotNull();
        assertThat(fichaResposta.getBody().historicoAtendimento().servicoProcuradoQualOnde())
                .isEqualTo("posto de saúde central");
    }

    @Test
    void submeter_comNumeroCasoEStatusNoPayload_saoIgnoradosAoAprovar() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario5.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        FichaRequestDTO fichaDto = new FichaRequestDTO(
                "2024/999999", "Carla Pereira", "16899622084", 30, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                StatusFicha.ENCERRADO);
        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(fichaDto, null, null, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor5.system@azizaidhub.local");
        FichaPendenteResponseDTO pendente = buscarPendentePorCpf(revisor, "16899622084");
        FichaPendenteResponseDTO aprovada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/aprovar", HttpMethod.POST,
                new HttpEntity<Void>(headersAutenticados(revisor)), FichaPendenteResponseDTO.class, pendente.id())
                .getBody();

        FichaResponseDTO fichaCriada = restTemplate.exchange(
                "/api/fichas/{id}", HttpMethod.GET, new HttpEntity<Void>(headersAutenticados(revisor)),
                FichaResponseDTO.class, aprovada.fichaId()).getBody();
        assertThat(fichaCriada.numeroCaso())
                .as("numeroCaso enviado pelo intake público não pode vazar pra Ficha — é controlado pela equipe")
                .isNotEqualTo("2024/999999");
        assertThat(fichaCriada.status())
                .as("status enviado pelo intake público não pode vazar pra Ficha — é controlado pela equipe")
                .isEqualTo("ATIVO");
    }

    @Test
    void submeter_comTokenInvalido_retorna404EPersisteAuditoriaMesmoComRollback() {
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(
                construirFichaDto("Maria", "52998224725"), null, null, null);
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/ficha-publica/{token}", dto, String.class, "token-que-nao-existe");
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        boolean auditoriaPersistida = fichaPublicaAuditLogRepository.findAll().stream()
                .anyMatch(log -> log.getResultado() == ResultadoFichaPublica.TOKEN_INVALIDO);
        assertThat(auditoriaPersistida)
                .as("auditoria de token inválido precisa persistir mesmo com a submissão lançando exceção depois")
                .isTrue();
    }

    @Test
    void submeter_comCpfInvalido_retorna400ENaoConsomeToken() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario3.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO(
                construirFichaDto("Maria", "11111111111"), null, null, null);
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/api/ficha-publica/{token}", dto, String.class, token);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<FichaPublicaStatusResponseDTO> statusAposFalha = restTemplate.getForEntity(
                "/api/ficha-publica/{token}/status", FichaPublicaStatusResponseDTO.class, token);
        assertThat(statusAposFalha.getBody().valido())
                .as("CPF inválido não pode queimar o convite de uso único")
                .isTrue();
    }

    @Test
    void rejeitar_pendentePendente_marcaRejeitada() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario2.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                construirFichaDto("Joana", "11144477735"), null, null, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor2.system@azizaidhub.local");
        Long pendenteId = buscarPendentePorCpf(revisor, "11144477735").id();

        HttpEntity<FichaPendenteRejeitarRequestDTO> rejeitarRequisicao = new HttpEntity<>(
                new FichaPendenteRejeitarRequestDTO("Dados incompletos"), headersAutenticados(revisor));
        ResponseEntity<FichaPendenteResponseDTO> rejeitada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/rejeitar", HttpMethod.POST, rejeitarRequisicao,
                FichaPendenteResponseDTO.class, pendenteId);

        assertThat(rejeitada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejeitada.getBody().status()).isEqualTo("REJEITADA");
        assertThat(rejeitada.getBody().motivoRejeicao()).isEqualTo("Dados incompletos");
    }

    @Test
    void rejeitar_comMotivoMaiorQue300Caracteres_retorna400() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario3rejeitar.system@azizaidhub.local");
        String token = criarConviteEExtrairToken(estagiario);

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                construirFichaDto("Carla", "66677788830"), null, null, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor3.system@azizaidhub.local");
        Long pendenteId = buscarPendentePorCpf(revisor, "66677788830").id();

        String motivoMuitoLongo = "x".repeat(301);
        HttpEntity<FichaPendenteRejeitarRequestDTO> rejeitarRequisicao = new HttpEntity<>(
                new FichaPendenteRejeitarRequestDTO(motivoMuitoLongo), headersAutenticados(revisor));
        ResponseEntity<String> rejeitada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/rejeitar", HttpMethod.POST, rejeitarRequisicao,
                String.class, pendenteId);

        assertThat(rejeitada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
