package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
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

import java.util.UUID;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcompanhamentoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    private HttpHeaders headersPara(Profissional profissional) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.gerarToken(profissional.getId(), profissional.getUsername(), profissional.getRole()));
        return headers;
    }

    @Test
    void listar_trazOIdDeQuemCriouCadaCaso() {
        Profissional criadora = profissionalRepository.save(Profissional.builder()
                .nome("Criadora Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("padrao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build());
        HttpHeaders headers = headersPara(criadora);

        ResponseEntity<FichaResponseDTO> ficha = restTemplate.postForEntity(
                "/api/fichas", new HttpEntity<>(construirDtoValido("61385024771"), headers), FichaResponseDTO.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AcompanhamentoResumoDTO[]> lista = restTemplate.exchange(
                "/api/acompanhamentos?q={cpf}", HttpMethod.GET, new HttpEntity<>(headers),
                AcompanhamentoResumoDTO[].class, "61385024771");

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).hasSize(1);
        assertThat(lista.getBody()[0].id()).isEqualTo(ficha.getBody().id());
        assertThat(lista.getBody()[0].criadoPorId()).isEqualTo(criadora.getId());
    }
}
