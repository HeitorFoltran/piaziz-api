package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.response.AlteracaoFichaResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
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
import org.springframework.http.ResponseEntity;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaAlteracaoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    private HttpHeaders headersPara(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Long criarFicha(String token, String cpf) {
        HttpEntity<FichaRequestDTO> requisicao = new HttpEntity<>(construirDtoValido(cpf), headersPara(token));
        ResponseEntity<FichaResponseDTO> resposta =
                restTemplate.postForEntity("/api/fichas", requisicao, FichaResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private AvaliacaoSocioeconomicaRequestDTO avaliacaoVazia() {
        return new AvaliacaoSocioeconomicaRequestDTO(
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    void get_comEdicaoCrossUserNaFicha_devolveLinhaComNomesCorretos() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenB = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Bruno Editor");

        Long fichaId = criarFicha(tokenA, "66048764707");

        HttpEntity<FichaRequestDTO> atualizacao =
                new HttpEntity<>(construirDtoValido("66048764707"), headersPara(tokenB));
        restTemplate.exchange("/api/fichas/{id}", HttpMethod.PUT, atualizacao, FichaResponseDTO.class, fichaId);

        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), AlteracaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
        AlteracaoFichaResponseDTO linha = resposta.getBody()[0];
        assertThat(linha.tipoEntidade()).isEqualTo("Ficha");
        assertThat(linha.editorNome()).isEqualTo("Bruno Editor");
        assertThat(linha.donoNome()).isEqualTo("Ana Criadora");
    }

    @Test
    void get_comEdicaoPeloProprioCriador_naoGeraLinha() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        Long fichaId = criarFicha(tokenA, "29141777638");

        HttpEntity<FichaRequestDTO> atualizacao =
                new HttpEntity<>(construirDtoValido("29141777638"), headersPara(tokenA));
        restTemplate.exchange("/api/fichas/{id}", HttpMethod.PUT, atualizacao, FichaResponseDTO.class, fichaId);

        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), AlteracaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    void get_comEdicaoCrossUserEmSubRecurso_apareceNaFichaComTipoEntidadeCorreto() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenB = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Bruno Editor");

        Long fichaId = criarFicha(tokenA, "01152449303");

        HttpEntity<AvaliacaoSocioeconomicaRequestDTO> criarAvaliacao =
                new HttpEntity<>(avaliacaoVazia(), headersPara(tokenA));
        restTemplate.exchange("/api/fichas/{id}/avaliacao-socioeconomica", HttpMethod.PUT, criarAvaliacao,
                Object.class, fichaId);

        HttpEntity<AvaliacaoSocioeconomicaRequestDTO> editarAvaliacao =
                new HttpEntity<>(avaliacaoVazia(), headersPara(tokenB));
        restTemplate.exchange("/api/fichas/{id}/avaliacao-socioeconomica", HttpMethod.PUT, editarAvaliacao,
                Object.class, fichaId);

        ResponseEntity<AlteracaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), AlteracaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody()[0].tipoEntidade()).isEqualTo("AvaliacaoSocioeconomica");
        assertThat(resposta.getBody()[0].editorNome()).isEqualTo("Bruno Editor");
    }

    @Test
    void get_comEstagiario_devolve403() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");
        String tokenEstagiario = persistirComToken(profissionalRepository, jwtService, PapelProfissional.ESTAGIARIO, "Estagiario Teste");

        Long fichaId = criarFicha(tokenA, "39825979194");

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenEstagiario)), String.class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void get_comFichaInexistente_devolve404() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Criadora");

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/fichas/{id}/alteracoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), String.class, 999999L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
