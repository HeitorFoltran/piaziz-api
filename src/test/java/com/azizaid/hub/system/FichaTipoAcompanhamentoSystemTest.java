package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.AtribuirTiposAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.model.enums.PapelProfissional;
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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FichaTipoAcompanhamentoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Test
    void putTiposAcompanhamento_atribuiEExpoeNoDetalhe() {
        String token = jwtService.gerarToken(1L, "teste@azizaidhub.local", PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        FichaRequestDTO fichaDto = construirDtoValido("66677788899");
        ResponseEntity<FichaResponseDTO> fichaCriada = restTemplate.postForEntity(
                "/api/fichas", new HttpEntity<>(fichaDto, headers), FichaResponseDTO.class);
        assertThat(fichaCriada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long fichaId = fichaCriada.getBody().id();

        ResponseEntity<TipoAcompanhamentoResponseDTO> tipo1 = restTemplate.postForEntity(
                "/api/tipos-acompanhamento",
                new HttpEntity<>(new TipoAcompanhamentoRequestDTO("Jurídico System"), headers),
                TipoAcompanhamentoResponseDTO.class);
        ResponseEntity<TipoAcompanhamentoResponseDTO> tipo2 = restTemplate.postForEntity(
                "/api/tipos-acompanhamento",
                new HttpEntity<>(new TipoAcompanhamentoRequestDTO("Psicológico System"), headers),
                TipoAcompanhamentoResponseDTO.class);

        AtribuirTiposAcompanhamentoRequestDTO atribuicao = new AtribuirTiposAcompanhamentoRequestDTO(
                List.of(tipo1.getBody().id(), tipo2.getBody().id()));

        ResponseEntity<FichaResponseDTO> atribuida = restTemplate.exchange(
                "/api/fichas/" + fichaId + "/tipos-acompanhamento",
                HttpMethod.PUT,
                new HttpEntity<>(atribuicao, headers),
                FichaResponseDTO.class);
        assertThat(atribuida.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<FichaResponseDTO> detalhe = restTemplate.exchange(
                "/api/fichas/" + fichaId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FichaResponseDTO.class);

        assertThat(detalhe.getBody().tiposAcompanhamento())
                .extracting("nome")
                .containsExactlyInAnyOrder("Jurídico System", "Psicológico System");
    }
}
