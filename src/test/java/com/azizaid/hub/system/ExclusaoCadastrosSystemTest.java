package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.AtribuirTiposAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.ServicoResponseDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoCadastroDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.repository.ServicoRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static com.azizaid.hub.support.EncaminhamentoTestFactory.construirEncaminhamentoDtoValido;
import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExclusaoCadastrosSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ProfissionalRepository profissionalRepository;

    @Autowired
    ServicoRepository servicoRepository;

    @Autowired
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    HttpHeaders headers;

    @BeforeEach
    void autenticar() {
        headers = new HttpHeaders();
        headers.setBearerAuth(persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO));
    }

    private ResponseEntity<String> excluir(String url) {
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private Long criarFicha(String cpf) {
        ResponseEntity<FichaResponseDTO> ficha = restTemplate.postForEntity(
                "/api/fichas", new HttpEntity<>(construirDtoValido(cpf), headers), FichaResponseDTO.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ficha.getBody().id();
    }

    @Test
    void tipoSemUso_excluiERetorna204() {
        Long id = restTemplate.postForEntity("/api/tipos-acompanhamento",
                new HttpEntity<>(new TipoAcompanhamentoRequestDTO("Tipo Exclusão Livre"), headers),
                TipoAcompanhamentoResponseDTO.class).getBody().id();

        assertThat(excluir("/api/tipos-acompanhamento/" + id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(tipoAcompanhamentoRepository.existsById(id)).isFalse();
    }

    @Test
    void tipoAtribuidoACaso_recusaEMantemAAtribuicao() {
        Long fichaId = criarFicha("41827365080");
        Long tipoId = restTemplate.postForEntity("/api/tipos-acompanhamento",
                new HttpEntity<>(new TipoAcompanhamentoRequestDTO("Tipo Exclusão Em Uso"), headers),
                TipoAcompanhamentoResponseDTO.class).getBody().id();
        restTemplate.exchange("/api/fichas/" + fichaId + "/tipos-acompanhamento", HttpMethod.PUT,
                new HttpEntity<>(new AtribuirTiposAcompanhamentoRequestDTO(List.of(tipoId)), headers),
                FichaResponseDTO.class);

        ResponseEntity<String> resposta = excluir("/api/tipos-acompanhamento/" + tipoId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("atribuído a 1 caso(s)");
        FichaResponseDTO ficha = restTemplate.exchange("/api/fichas/" + fichaId, HttpMethod.GET,
                new HttpEntity<>(headers), FichaResponseDTO.class).getBody();
        assertThat(ficha.tiposAcompanhamento()).extracting("id").containsExactly(tipoId);

        TipoAcompanhamentoCadastroDTO[] lista = restTemplate.exchange("/api/tipos-acompanhamento", HttpMethod.GET,
                new HttpEntity<>(headers), TipoAcompanhamentoCadastroDTO[].class).getBody();
        assertThat(Arrays.stream(lista).filter(t -> t.id().equals(tipoId)).findFirst().orElseThrow().emUso()).isTrue();
    }

    @Test
    void servicoSemUso_excluiERetorna204() {
        Long id = restTemplate.postForEntity("/api/servicos",
                new HttpEntity<>(new ServicoRequestDTO("Serviço Exclusão Livre"), headers),
                ServicoResponseDTO.class).getBody().id();

        assertThat(excluir("/api/servicos/" + id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(servicoRepository.existsById(id)).isFalse();
    }

    @Test
    void servicoComEncaminhamento_recusaEMarcaEmUso() {
        Long fichaId = criarFicha("52938476191");
        Long servicoId = restTemplate.postForEntity("/api/servicos",
                new HttpEntity<>(new ServicoRequestDTO("Serviço Exclusão Em Uso"), headers),
                ServicoResponseDTO.class).getBody().id();
        ResponseEntity<String> encaminhamento = restTemplate.postForEntity("/api/fichas/" + fichaId + "/encaminhamentos",
                new HttpEntity<>(construirEncaminhamentoDtoValido(servicoId), headers), String.class);
        assertThat(encaminhamento.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> resposta = excluir("/api/servicos/" + servicoId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("1 encaminhamento(s)");
        assertThat(servicoRepository.existsById(servicoId)).isTrue();

        ServicoResponseDTO[] lista = restTemplate.exchange("/api/servicos", HttpMethod.GET,
                new HttpEntity<>(headers), ServicoResponseDTO[].class).getBody();
        assertThat(Arrays.stream(lista).filter(s -> s.id().equals(servicoId)).findFirst().orElseThrow().emUso()).isTrue();
    }

    @Test
    void idInexistente_retorna404() {
        assertThat(excluir("/api/servicos/999999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(excluir("/api/tipos-acompanhamento/999999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
