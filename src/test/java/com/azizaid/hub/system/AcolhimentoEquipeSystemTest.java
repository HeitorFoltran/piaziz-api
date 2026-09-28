package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.AcolhimentoEquipeResponseDTO;
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

import java.util.Set;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcolhimentoEquipeSystemTest extends PostgresTestContainerConfig {

    private static final String URL = "/api/fichas/{id}/acolhimento-equipe";

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

    private <T> ResponseEntity<T> put(AcolhimentoEquipeRequestDTO dto, String token, Long fichaId, Class<T> tipo) {
        return restTemplate.exchange(URL, HttpMethod.PUT, new HttpEntity<>(dto, headersPara(token)), tipo, fichaId);
    }

    @Test
    void put_comEncaminhamentosSugeridos_getDevolveIgual() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        Long fichaId = criarFicha(token, "45333147833");
        AcolhimentoEquipeRequestDTO dto = new AcolhimentoEquipeRequestDTO(null, null, null, Set.of(), null, null,
                null, null, null, null, null, null, null, null,
                true, "UBS do bairro",
                false, null,
                true, "Aluguel social",
                null, null,
                true, "CRAS Centro",
                false, null,
                "Defensoria Pública",
                null, null);

        ResponseEntity<AcolhimentoEquipeResponseDTO> salvo = put(dto, token, fichaId, AcolhimentoEquipeResponseDTO.class);
        assertThat(salvo.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AcolhimentoEquipeResponseDTO> lido = restTemplate.exchange(URL, HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), AcolhimentoEquipeResponseDTO.class, fichaId);
        AcolhimentoEquipeResponseDTO a = lido.getBody();
        assertThat(a.sugereSaudeGeral()).isTrue();
        assertThat(a.sugereSaudeGeralQual()).isEqualTo("UBS do bairro");
        // false respondido não pode virar null (não respondido).
        assertThat(a.sugereSaudeMental()).isFalse();
        assertThat(a.sugereSaudeMentalQual()).isNull();
        assertThat(a.sugereHabitacao()).isTrue();
        assertThat(a.sugereHabitacaoQual()).isEqualTo("Aluguel social");
        assertThat(a.sugereTrabalhoEmprego()).isNull();
        assertThat(a.sugereTrabalhoEmpregoQual()).isNull();
        assertThat(a.sugereAssistenciaSocial()).isTrue();
        assertThat(a.sugereAssistenciaSocialQual()).isEqualTo("CRAS Centro");
        assertThat(a.sugereAssistenciaEducacional()).isFalse();
        assertThat(a.sugereAssistenciaEducacionalQual()).isNull();
        assertThat(a.sugereOutro()).isEqualTo("Defensoria Pública");
    }

    @Test
    void put_comAmeacasRelatadasMaiorQueAColuna_retorna400EmVezDe500() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        Long fichaId = criarFicha(token, "93288040786");
        AcolhimentoEquipeRequestDTO dto = new AcolhimentoEquipeRequestDTO(null, null, null, Set.of(), null, null,
                null, "a".repeat(1001), null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);

        ResponseEntity<String> resposta = put(dto, token, fichaId, String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("ameacasRelatadas");
    }

    @Test
    void postFicha_comIdadeNegativa_retorna400() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        FichaRequestDTO b = construirDtoValido("23862551156");
        FichaRequestDTO dto = new FichaRequestDTO(b.numeroCaso(), b.nome(), b.cpf(), -1, b.telefone(),
                b.estadoCivil(), b.pessoasDependentes(), b.idadeFilhos(), b.nivelSeguranca(), b.tipoMoradia(),
                b.tipoMoradiaOutraDescricao(), b.qtdMoradores(), b.qtdFilhos(), b.ondeMoramFilhos(),
                b.supervisaoFilhos(), b.vagasNecessarias(), b.necessidadesImediatas(),
                b.necessidadeOutraDescricao(), b.status());

        ResponseEntity<String> resposta = restTemplate.postForEntity("/api/fichas",
                new HttpEntity<>(dto, headersPara(token)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("idade não pode ser negativa");
    }
}
