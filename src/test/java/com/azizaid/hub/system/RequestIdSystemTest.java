package com.azizaid.hub.system;

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
class RequestIdSystemTest extends PostgresTestContainerConfig {

    private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    TestRestTemplate restTemplate;

    private String requestIdDe(ResponseEntity<?> resposta) {
        return resposta.getHeaders().getFirst("X-Request-Id");
    }

    @Test
    void respostaComSucessoEComErro_temHeaderXRequestIdComFormatoDeUuid() {
        ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> naoAutenticado = restTemplate.getForEntity("/api/fichas", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestIdDe(health)).matches(UUID_REGEX);
        assertThat(naoAutenticado.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(requestIdDe(naoAutenticado)).matches(UUID_REGEX);
    }

    @Test
    void duasRequisicoes_temIdsDiferentes() {
        String primeiro = requestIdDe(restTemplate.getForEntity("/actuator/health", String.class));
        String segundo = requestIdDe(restTemplate.getForEntity("/actuator/health", String.class));

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    void idMandadoPeloCliente_eIgnorado() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", "abc");

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/actuator/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(requestIdDe(resposta)).matches(UUID_REGEX).isNotEqualTo("abc");
        assertThat(resposta.getHeaders().get("X-Request-Id")).hasSize(1);
    }
}
