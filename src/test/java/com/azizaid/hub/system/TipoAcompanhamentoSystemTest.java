package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TipoAcompanhamentoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Test
    void postTipoAcompanhamento_duasVezesMesmoNome_segundaRetorna400() {
        String token = jwtService.gerarToken(1L, "teste@azizaidhub.local", PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        TipoAcompanhamentoRequestDTO dto = new TipoAcompanhamentoRequestDTO("Acolhimento");
        HttpEntity<TipoAcompanhamentoRequestDTO> requisicao = new HttpEntity<>(dto, headers);

        ResponseEntity<String> primeira = restTemplate.postForEntity("/api/tipos-acompanhamento", requisicao, String.class);
        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> segunda = restTemplate.postForEntity("/api/tipos-acompanhamento", requisicao, String.class);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
