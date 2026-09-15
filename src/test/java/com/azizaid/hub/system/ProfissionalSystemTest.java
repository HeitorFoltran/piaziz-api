package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
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
class ProfissionalSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    private ProfissionalRequestDTO construirDtoValido(String email) {
        return new ProfissionalRequestDTO(
                "Profissional Teste",
                "12345678900",
                null,
                null,
                email,
                "senha123",
                null);
    }

    @Test
    void postProfissional_comRolePadrao_retorna403() {
        String token = jwtService.gerarToken(1L, "padrao@azizaidhub.local", PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<ProfissionalRequestDTO> requisicao =
                new HttpEntity<>(construirDtoValido("novo.padrao@azizaidhub.local"), headers);

        ResponseEntity<String> resposta =
                restTemplate.postForEntity("/api/profissionais", requisicao, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void postProfissional_comRoleDev_retorna201() {
        String token = jwtService.gerarToken(1L, "dev@azizaidhub.local", PapelProfissional.DEV);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<ProfissionalRequestDTO> requisicao =
                new HttpEntity<>(construirDtoValido("novo.dev@azizaidhub.local"), headers);

        ResponseEntity<ProfissionalResponseDTO> resposta =
                restTemplate.postForEntity("/api/profissionais", requisicao, ProfissionalResponseDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().email()).isEqualTo("novo.dev@azizaidhub.local");
    }
}
