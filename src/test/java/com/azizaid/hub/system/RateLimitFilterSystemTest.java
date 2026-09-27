package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.request.TrocarSenhaRequestDTO;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "rate-limit.login.capacidade=3",
        "rate-limit.login.janela-minutos=15",
        "rate-limit.troca-senha.capacidade=2"
})
class RateLimitFilterSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @MockitoSpyBean
    ProfissionalRepository profissionalRepository;

    private ResponseEntity<String> tentarLogin() {
        LoginRequestDTO dto = new LoginRequestDTO("inexistente." + UUID.randomUUID() + "@azizaidhub.local", "qualquer");
        return restTemplate.postForEntity("/api/auth/login", dto, String.class);
    }

    @Test
    void quartaRequisicaoLogin_retorna429ComRetryAfterECorpoPadrao() {
        for (int i = 0; i < 3; i++) {
            tentarLogin();
        }

        ResponseEntity<String> resposta = tentarLogin();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getHeaders().getFirst("Retry-After")).isNotBlank();
        assertThat(resposta.getBody()).contains("\"status\":429").contains("\"message\"");
    }

    @Test
    void rotaForaDasRegras_nuncaRetorna429() {
        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> resposta = restTemplate.getForEntity("/actuator/health", String.class);
            assertThat(resposta.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    void regrasIndependentes_estourarLoginNaoAfetaFichaPublicaStatus() {
        for (int i = 0; i < 4; i++) {
            tentarLogin();
        }
        assertThat(tentarLogin().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        ResponseEntity<String> statusResposta =
                restTemplate.getForEntity("/api/ficha-publica/token-qualquer/status", String.class);
        assertThat(statusResposta.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void requisicaoBarradaPeloRateLimit_naoConsultaProfissionalRepository() {
        Profissional profissional = profissionalRepository.save(Profissional.builder()
                .nome("Spy Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("spy." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build());
        String token = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);

        for (int i = 0; i < 3; i++) {
            tentarLogin();
        }
        clearInvocations(profissionalRepository);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequestDTO> requisicao = new HttpEntity<>(
                new LoginRequestDTO("qualquer@azizaidhub.local", "qualquer"), headers);

        ResponseEntity<String> resposta =
                restTemplate.postForEntity("/api/auth/login", requisicao, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(profissionalRepository, never()).findById(any());
    }

    @Test
    void trocaDeSenha_depoisDoLimite_retorna429() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<TrocarSenhaRequestDTO> requisicao =
                new HttpEntity<>(new TrocarSenhaRequestDTO("senha-atual-errada", "nova-senha-123"), headers);

        for (int i = 0; i < 2; i++) {
            ResponseEntity<String> resposta =
                    restTemplate.exchange("/api/auth/senha", HttpMethod.PUT, requisicao, String.class);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> resposta =
                restTemplate.exchange("/api/auth/senha", HttpMethod.PUT, requisicao, String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
