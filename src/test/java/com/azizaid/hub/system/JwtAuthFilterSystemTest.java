package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtAuthFilterSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    private Profissional persistirProfissional(PapelProfissional role) {
        return profissionalRepository.save(Profissional.builder()
                .nome("Profissional Filtro")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("filtro." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(role)
                .build());
    }

    private ResponseEntity<String> listarFichas(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/fichas", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void tokenValido_profissionalAtivo_retorna200() {
        Profissional profissional = persistirProfissional(PapelProfissional.PADRAO);
        String token = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);

        assertThat(listarFichas(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void contaDesativadaDepoisDoToken_mesmoTokenRetorna401() {
        Profissional profissional = persistirProfissional(PapelProfissional.PADRAO);
        String token = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);

        profissional.setAtivo(false);
        profissionalRepository.save(profissional);

        assertThat(listarFichas(token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sessaoRevogadaDepoisDoToken_tokenAntigoRetorna401_tokenNovoRetorna200() throws InterruptedException {
        Profissional profissional = persistirProfissional(PapelProfissional.PADRAO);
        String tokenAntigo = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);

        Thread.sleep(1100);
        profissional.setSessoesRevogadasEm(Instant.now());
        profissionalRepository.save(profissional);

        assertThat(listarFichas(tokenAntigo).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Thread.sleep(1100);
        String tokenNovo = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);
        assertThat(listarFichas(tokenNovo).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void roleRebaixadaNoBanco_tokenAntigoPassaARespeitarNovaRole() {
        Profissional profissional = persistirProfissional(PapelProfissional.PADRAO);
        String token = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), PapelProfissional.PADRAO);

        assertThat(listarFichas(token).getStatusCode()).isEqualTo(HttpStatus.OK);

        profissional.setRole(PapelProfissional.ESTAGIARIO);
        profissionalRepository.save(profissional);

        assertThat(listarFichas(token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenDeIdInexistente_retorna401() {
        String token = jwtService.gerarToken(999_999_999L, "fantasma", PapelProfissional.PADRAO);

        assertThat(listarFichas(token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
