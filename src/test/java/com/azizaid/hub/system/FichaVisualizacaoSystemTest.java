package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.VisualizacaoFichaResponseDTO;
import com.azizaid.hub.model.LeituraAuditLog;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.LeituraAuditLogRepository;
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

import java.util.List;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaVisualizacaoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    LeituraAuditLogRepository leituraAuditLogRepository;

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

    private ResponseEntity<String> abrir(String token, Long fichaId) {
        return restTemplate.exchange("/api/fichas/{id}", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), String.class, fichaId);
    }

    private List<LeituraAuditLog> leiturasDa(Long fichaId) {
        return leituraAuditLogRepository.findTop200ByFichaIdOrderByTimestampDescIdDesc(fichaId);
    }

    @Test
    void getDetalhe_gravaUmaLinha_eSegundaAberturaLogoEmSeguidaNaoGravaOutra() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        Long fichaId = criarFicha(token, "71428793860");

        assertThat(abrir(token, fichaId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leiturasDa(fichaId)).hasSize(1);

        assertThat(abrir(token, fichaId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leiturasDa(fichaId)).hasSize(1);
    }

    @Test
    void getVisualizacoes_devolveUmaLinhaPorUsuarioComNome_doMaisNovoParaOMaisAntigo() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Ana Leitora");
        String tokenB = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO, "Bruno Leitor");
        Long fichaId = criarFicha(tokenA, "85303526020");

        abrir(tokenA, fichaId);
        abrir(tokenB, fichaId);

        ResponseEntity<VisualizacaoFichaResponseDTO[]> resposta = restTemplate.exchange(
                "/api/fichas/{id}/visualizacoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenA)), VisualizacaoFichaResponseDTO[].class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .extracting(VisualizacaoFichaResponseDTO::profissionalNome)
                .containsExactly("Bruno Leitor", "Ana Leitora");
        assertThat(resposta.getBody()).allSatisfy(v -> assertThat(v.timestamp()).isNotNull());
        // Consultar o log não conta como leitura.
        assertThat(leiturasDa(fichaId)).hasSize(2);
    }

    @Test
    void getLista_naoGravaLeitura() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        Long fichaId = criarFicha(token, "38116245040");

        ResponseEntity<String> resposta = restTemplate.exchange("/api/fichas", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(leiturasDa(fichaId)).isEmpty();
    }

    @Test
    void getDetalhe_comFichaInexistente_devolve404ENaoGrava() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        long totalAntes = leituraAuditLogRepository.count();

        assertThat(abrir(token, 999_999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(leiturasDa(999_999L)).isEmpty();
        assertThat(leituraAuditLogRepository.count()).isEqualTo(totalAntes);
    }

    @Test
    void getVisualizacoes_comFichaInexistente_devolve404() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);

        ResponseEntity<String> resposta = restTemplate.exchange("/api/fichas/{id}/visualizacoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), String.class, 999_999L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getVisualizacoes_comEstagiario_devolve403() {
        String tokenA = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        String tokenEstagiario = persistirComToken(profissionalRepository, jwtService, PapelProfissional.ESTAGIARIO);
        Long fichaId = criarFicha(tokenA, "54677871019");

        ResponseEntity<String> resposta = restTemplate.exchange("/api/fichas/{id}/visualizacoes", HttpMethod.GET,
                new HttpEntity<>(headersPara(tokenEstagiario)), String.class, fichaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
