package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.PaginaDTO;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import com.azizaid.hub.support.ProfissionalTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcompanhamentoSystemTest extends PostgresTestContainerConfig {

    private static final ParameterizedTypeReference<PaginaDTO<AcompanhamentoResumoDTO>> PAGINA =
            new ParameterizedTypeReference<>() {
            };

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

    private HttpEntity<Void> comToken(PapelProfissional role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(persistirComToken(profissionalRepository, jwtService, role));
        return new HttpEntity<>(headers);
    }

    private Profissional persistirPadrao() {
        return profissionalRepository.save(Profissional.builder()
                .nome("Criadora Teste")
                .cpf("12345678900")
                .username(ProfissionalTestFactory.usernameUnico())
                .email("padrao." + UUID.randomUUID() + "@azizaidhub.local")
                .senhaHash("hash-irrelevante-pro-teste")
                .role(PapelProfissional.PADRAO)
                .build());
    }

    @Test
    void listar_trazOIdDeQuemCriouCadaCaso() {
        Profissional criadora = persistirPadrao();
        HttpHeaders headers = headersPara(criadora);

        ResponseEntity<FichaResponseDTO> ficha = restTemplate.postForEntity(
                "/api/fichas", new HttpEntity<>(construirDtoValido("61385024771"), headers), FichaResponseDTO.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<PaginaDTO<AcompanhamentoResumoDTO>> lista = restTemplate.exchange(
                "/api/acompanhamentos?q={cpf}", HttpMethod.GET, new HttpEntity<>(headers), PAGINA, "61385024771");

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody().itens()).hasSize(1);
        assertThat(lista.getBody().itens().get(0).id()).isEqualTo(ficha.getBody().id());
        assertThat(lista.getBody().itens().get(0).criadoPorId()).isEqualTo(criadora.getId());
    }

    @Test
    void listar_devolveOFormatoDePagina() {
        HttpHeaders headers = headersPara(persistirPadrao());
        restTemplate.postForEntity("/api/fichas", new HttpEntity<>(construirDtoValido("52998224725"), headers),
                FichaResponseDTO.class);
        restTemplate.postForEntity("/api/fichas", new HttpEntity<>(construirDtoValido("11144477735"), headers),
                FichaResponseDTO.class);

        ResponseEntity<Map> resposta = restTemplate.exchange(
                "/api/acompanhamentos?size=2", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsOnlyKeys("itens", "pagina", "tamanho", "totalItens", "totalPaginas");
        assertThat(resposta.getBody()).containsEntry("pagina", 0).containsEntry("tamanho", 2);
        assertThat((List<?>) resposta.getBody().get("itens")).hasSize(2);
        assertThat(((Number) resposta.getBody().get("totalItens")).longValue()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void listar_comStatusInvalido_retorna400() {
        assertBadRequest("/api/acompanhamentos?status=XYZ", "Parâmetro inválido: status");
    }

    @Test
    void listar_comDeDepoisDeAte_retorna400() {
        assertBadRequest("/api/acompanhamentos?de=2026-03-10&ate=2026-03-01", "de não pode ser depois de ate");
    }

    @Test
    void listar_comSizeForaDoLimite_retorna400() {
        assertBadRequest("/api/acompanhamentos?size=0", "size: deve estar entre 1 e 100");
        assertBadRequest("/api/acompanhamentos?size=101", "size: deve estar entre 1 e 100");
    }

    @Test
    void listar_comPageNegativa_retorna400() {
        assertBadRequest("/api/acompanhamentos?page=-1", "page: deve ser maior ou igual a 0");
    }

    @Test
    void listar_comCampoDataInvalido_retorna400() {
        assertBadRequest("/api/acompanhamentos?campoData=dataNascimento",
                "campoData deve ser dataAtualizacao ou dataCriacao");
    }

    @Test
    void listar_comDataEmFormatoErrado_retorna400() {
        assertBadRequest("/api/acompanhamentos?de=10/03/2026", "Parâmetro inválido: de");
    }

    @Test
    void listar_comoEstagiario_retorna403() {
        ResponseEntity<Map> resposta = restTemplate.exchange(
                "/api/acompanhamentos", HttpMethod.GET, comToken(PapelProfissional.ESTAGIARIO), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void assertBadRequest(String url, String mensagem) {
        ResponseEntity<Map> resposta = restTemplate.exchange(
                url, HttpMethod.GET, comToken(PapelProfissional.PADRAO), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("message", mensagem);
    }
}
