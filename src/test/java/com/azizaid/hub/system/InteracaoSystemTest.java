package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.InteracaoRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.InteracaoResponseDTO;
import com.azizaid.hub.model.Profissional;
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

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InteracaoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Test
    void postInteracao_usaNomeDoProfissionalAutenticadoComoAutor() {
        Profissional profissional = profissionalRepository.save(Profissional.builder()
                .nome("Ana Beatriz")
                .cpf("11122233396")
                .email("ana.beatriz.system@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build());

        String token = jwtService.gerarToken(profissional.getId(), profissional.getEmail(), PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        FichaRequestDTO fichaDto = construirDtoValido("52998224725");
        HttpEntity<FichaRequestDTO> criarFichaRequisicao = new HttpEntity<>(fichaDto, headers);
        ResponseEntity<FichaResponseDTO> fichaCriada =
                restTemplate.postForEntity("/api/fichas", criarFichaRequisicao, FichaResponseDTO.class);
        assertThat(fichaCriada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long fichaId = fichaCriada.getBody().id();

        InteracaoRequestDTO interacaoDto = new InteracaoRequestDTO("Nota de acompanhamento");
        HttpEntity<InteracaoRequestDTO> criarInteracaoRequisicao = new HttpEntity<>(interacaoDto, headers);
        ResponseEntity<InteracaoResponseDTO> interacaoCriada = restTemplate.postForEntity(
                "/api/fichas/{id}/interacoes", criarInteracaoRequisicao, InteracaoResponseDTO.class, fichaId);

        assertThat(interacaoCriada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(interacaoCriada.getBody().autor()).isEqualTo("Ana Beatriz");
    }
}
