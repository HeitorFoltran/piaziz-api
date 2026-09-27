package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServicoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    private ResponseEntity<String> postar(String nome) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO));
        return restTemplate.postForEntity("/api/servicos",
                new HttpEntity<>(new ServicoRequestDTO(nome), headers), String.class);
    }

    @Test
    void postServico_mesmoNomeComEspacosEOutraCaixa_segundoRetorna400() {
        // Sufixo único: o banco do Testcontainers é compartilhado entre as classes de teste.
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        assertThat(postar("CRAS " + sufixo).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> segunda = postar(" cras " + sufixo + " ");
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(segunda.getBody()).contains("Já existe um serviço com este nome");
    }

    @Test
    void postServico_nomeCom101Caracteres_retorna400() {
        assertThat(postar("a".repeat(101)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
