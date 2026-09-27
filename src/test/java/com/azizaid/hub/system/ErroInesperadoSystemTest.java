package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.service.ServicoService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Classe separada de TratamentoErroSystemTest: o @MockitoBean cria outro contexto Spring.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErroInesperadoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @MockitoBean
    ServicoService servicoService;

    @Test
    void excecaoInesperada_retorna500GenericoSemDetalheInterno() {
        when(servicoService.listar()).thenThrow(new IllegalStateException("detalhe interno do teste"));
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/servicos", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody())
                .contains("\"message\":\"Erro interno\"")
                .doesNotContain("detalhe interno do teste");
    }
}
