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

// Origem permitida vem de cors.allowed-origins em src/test/resources/application.properties.
// Se o header Origin fosse descartado pelo cliente HTTP, o OPTIONS não seria preflight e voltaria
// 401 (rota autenticada), então os dois testes falhariam em vez de passar pelo motivo errado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    private ResponseEntity<String> preflight(String origem) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origem);
        headers.setAccessControlRequestMethod(HttpMethod.GET);
        return restTemplate.exchange("/api/fichas", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
    }

    @Test
    void preflight_origemPermitida_retorna200ComAllowOrigin() {
        ResponseEntity<String> resposta = preflight("http://localhost:5173");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }

    @Test
    void preflight_origemForaDaLista_retorna403() {
        ResponseEntity<String> resposta = preflight("https://malicioso.exemplo.com");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isNull();
    }
}
