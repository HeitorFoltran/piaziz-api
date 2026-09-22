package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.FichaPendenteRejeitarRequestDTO;
import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.dto.response.FichaPublicaSubmissaoResponseDTO;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
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

    @Test
    void fluxoCompleto_conviteSubmissaoEAprovacao() {
        Profissional estagiario = criarProfissional(PapelProfissional.ESTAGIARIO, "estagiario.system@azizaidhub.local");
        HttpEntity<Void> criarConviteRequisicao = new HttpEntity<>(headersAutenticados(estagiario));
        ResponseEntity<ConviteFichaResponseDTO> conviteCriado = restTemplate.exchange(
                "/api/convites-ficha", HttpMethod.POST, criarConviteRequisicao, ConviteFichaResponseDTO.class);
        assertThat(conviteCriado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String linkCompleto = conviteCriado.getBody().linkCompleto();
        String token = linkCompleto.substring(linkCompleto.lastIndexOf('/') + 1);

        ResponseEntity<FichaPublicaStatusResponseDTO> statusAntes = restTemplate.getForEntity(
                "/api/ficha-publica/{token}/status", FichaPublicaStatusResponseDTO.class, token);
        assertThat(statusAntes.getBody().valido()).isTrue();

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                "Maria da Silva", "48291365709", "(45) 99999-0000", 28, "Relato de situação de risco");
        ResponseEntity<FichaPublicaSubmissaoResponseDTO> submissao = restTemplate.postForEntity(
                "/api/ficha-publica/{token}", submissaoDto, FichaPublicaSubmissaoResponseDTO.class, token);
        assertThat(submissao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<FichaPublicaStatusResponseDTO> statusDepois = restTemplate.getForEntity(
                "/api/ficha-publica/{token}/status", FichaPublicaStatusResponseDTO.class, token);
        assertThat(statusDepois.getBody().valido()).isFalse();
        assertThat(statusDepois.getBody().motivo()).isEqualTo("usado");

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor.system@azizaidhub.local");
        HttpEntity<Void> listarRequisicao = new HttpEntity<>(headersAutenticados(revisor));
        ResponseEntity<FichaPendenteResponseDTO[]> pendentes = restTemplate.exchange(
                "/api/fichas-pendentes", HttpMethod.GET, listarRequisicao, FichaPendenteResponseDTO[].class);
        FichaPendenteResponseDTO pendente = java.util.Arrays.stream(pendentes.getBody())
                .filter(p -> p.cpf().equals("48291365709"))
                .findFirst()
                .orElseThrow();
        Long pendenteId = pendente.id();
        assertThat(pendente.nome()).isEqualTo("Maria da Silva");

        HttpEntity<Void> aprovarRequisicao = new HttpEntity<>(headersAutenticados(revisor));
        ResponseEntity<FichaPendenteResponseDTO> aprovada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/aprovar", HttpMethod.POST, aprovarRequisicao,
                FichaPendenteResponseDTO.class, pendenteId);
        assertThat(aprovada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprovada.getBody().status()).isEqualTo("APROVADA");
        assertThat(aprovada.getBody().fichaId()).isNotNull();
    }

    @Test
    void submeter_comTokenInvalido_retorna404EPersisteAuditoriaMesmoComRollback() {
        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO("Maria", "52998224725", null, null, null);
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
        ConviteFichaResponseDTO convite = restTemplate.exchange(
                "/api/convites-ficha", HttpMethod.POST, new HttpEntity<Void>(headersAutenticados(estagiario)),
                ConviteFichaResponseDTO.class).getBody();
        String token = convite.linkCompleto().substring(convite.linkCompleto().lastIndexOf('/') + 1);

        FichaPublicaRequestDTO dto = new FichaPublicaRequestDTO("Maria", "11111111111", null, null, null);
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
        HttpEntity<Void> criarConviteRequisicao = new HttpEntity<>(headersAutenticados(estagiario));
        ConviteFichaResponseDTO convite = restTemplate.exchange(
                "/api/convites-ficha", HttpMethod.POST, criarConviteRequisicao, ConviteFichaResponseDTO.class)
                .getBody();
        String token = convite.linkCompleto().substring(convite.linkCompleto().lastIndexOf('/') + 1);

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                "Joana", "11144477735", null, null, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor2.system@azizaidhub.local");
        FichaPendenteResponseDTO[] pendentes = restTemplate.exchange(
                "/api/fichas-pendentes", HttpMethod.GET, new HttpEntity<Void>(headersAutenticados(revisor)),
                FichaPendenteResponseDTO[].class).getBody();
        Long pendenteId = java.util.Arrays.stream(pendentes)
                .filter(p -> p.cpf().equals("11144477735"))
                .findFirst()
                .orElseThrow()
                .id();

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
        ConviteFichaResponseDTO convite = restTemplate.exchange(
                "/api/convites-ficha", HttpMethod.POST, new HttpEntity<Void>(headersAutenticados(estagiario)),
                ConviteFichaResponseDTO.class).getBody();
        String token = convite.linkCompleto().substring(convite.linkCompleto().lastIndexOf('/') + 1);

        FichaPublicaRequestDTO submissaoDto = new FichaPublicaRequestDTO(
                "Carla", "66677788830", null, null, null);
        restTemplate.postForEntity("/api/ficha-publica/{token}", submissaoDto,
                FichaPublicaSubmissaoResponseDTO.class, token);

        Profissional revisor = criarProfissional(PapelProfissional.PADRAO, "revisor3.system@azizaidhub.local");
        FichaPendenteResponseDTO[] pendentes = restTemplate.exchange(
                "/api/fichas-pendentes", HttpMethod.GET, new HttpEntity<Void>(headersAutenticados(revisor)),
                FichaPendenteResponseDTO[].class).getBody();
        Long pendenteId = java.util.Arrays.stream(pendentes)
                .filter(p -> p.cpf().equals("66677788830"))
                .findFirst()
                .orElseThrow()
                .id();

        String motivoMuitoLongo = "x".repeat(301);
        HttpEntity<FichaPendenteRejeitarRequestDTO> rejeitarRequisicao = new HttpEntity<>(
                new FichaPendenteRejeitarRequestDTO(motivoMuitoLongo), headersAutenticados(revisor));
        ResponseEntity<String> rejeitada = restTemplate.exchange(
                "/api/fichas-pendentes/{id}/rejeitar", HttpMethod.POST, rejeitarRequisicao,
                String.class, pendenteId);

        assertThat(rejeitada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
