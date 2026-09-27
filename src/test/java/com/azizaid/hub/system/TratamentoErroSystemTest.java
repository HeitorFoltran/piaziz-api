package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.model.enums.PapelProfissional;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

// Antes de 2026-09-27, tudo que não era exceção do projeto caía no /error sem permitAll e
// voltava 401 "Não autenticado" no lugar do status real.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TratamentoErroSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Test
    void login_comJsonMalformado_retorna400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resposta = restTemplate.postForEntity(
                "/api/auth/login", new HttpEntity<>("{", headers), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("message", "Corpo da requisição inválido");
    }

    @Test
    void rotaInexistente_comTokenValido_retorna404() {
        ResponseEntity<Map> resposta = restTemplate.exchange(
                "/api/rota-que-nao-existe", HttpMethod.GET, comToken(), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Recurso não encontrado");
    }

    @Test
    void metodoNaoSuportado_comTokenValido_retorna405() {
        ResponseEntity<Map> resposta = restTemplate.exchange(
                "/api/servicos", HttpMethod.DELETE, comToken(), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resposta.getBody()).containsEntry("message", "Método não permitido");
    }

    private HttpEntity<Void> comToken() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
