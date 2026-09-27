package com.azizaid.hub.system;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.dto.response.PontoSerieDTO;
import com.azizaid.hub.dto.response.RelatorioAnaliticoDTO;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.repository.ServicoRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.azizaid.hub.support.EncaminhamentoTestFactory.construirEncaminhamentoDtoValido;
import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static com.azizaid.hub.support.ProfissionalTestFactory.persistirComToken;
import static org.assertj.core.api.Assertions.assertThat;

// As consultas do relatório são nativas (date_trunc) e o tipo Java que o driver devolve para o
// período já mudou entre versões do Hibernate: este teste roda contra Postgres real para pegar isso.
// O banco é compartilhado entre as suítes, então os totais são comparados com uma leitura anterior
// em vez de valores absolutos.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RelatorioAnaliticoSystemTest extends PostgresTestContainerConfig {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtService jwtService;

    @Autowired
    ServicoRepository servicoRepository;

    @Autowired
    ProfissionalRepository profissionalRepository;

    private HttpHeaders headersPara(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<RelatorioAnaliticoDTO> buscarRelatorio(String token, String granularidade) {
        String url = granularidade == null
                ? "/api/dashboard/relatorios"
                : "/api/dashboard/relatorios?granularidade=" + granularidade;
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headersPara(token)),
                RelatorioAnaliticoDTO.class);
    }

    private void criarFichaComEncaminhamentoEAtendimento(String token, String cpf) {
        HttpEntity<FichaRequestDTO> criarFicha = new HttpEntity<>(construirDtoValido(cpf), headersPara(token));
        ResponseEntity<FichaResponseDTO> ficha =
                restTemplate.postForEntity("/api/fichas", criarFicha, FichaResponseDTO.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long fichaId = ficha.getBody().id();

        Servico servico = servicoRepository.save(Servico.builder().nome("Serviço Relatórios Teste").build());
        ResponseEntity<String> encaminhamento = restTemplate.postForEntity(
                "/api/fichas/{id}/encaminhamentos",
                new HttpEntity<>(construirEncaminhamentoDtoValido(servico.getId()), headersPara(token)),
                String.class, fichaId);
        assertThat(encaminhamento.getStatusCode().is2xxSuccessful()).isTrue();

        HistoricoAtendimentoRequestDTO historico = new HistoricoAtendimentoRequestDTO(
                false, null, false, null, false, null, null, null, null);
        ResponseEntity<String> atendimento = restTemplate.exchange(
                "/api/fichas/{id}/historico-atendimento", HttpMethod.PUT,
                new HttpEntity<>(historico, headersPara(token)), String.class, fichaId);
        assertThat(atendimento.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private static List<String> periodos(List<PontoSerieDTO> serie) {
        return serie.stream().map(PontoSerieDTO::periodo).toList();
    }

    @Test
    void get_comDadosCriados_devolveTotaisESeriesNasTresGranularidades() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);
        RelatorioAnaliticoDTO antes = buscarRelatorio(token, "mes").getBody();

        criarFichaComEncaminhamentoEAtendimento(token, "73182946528");

        LocalDate hoje = LocalDate.now();
        String dia = hoje.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String semana = hoje.with(DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String mes = hoje.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // sem parâmetro, o default é "mes"
        String[][] casos = {{null, mes}, {"dia", dia}, {"semana", semana}, {"mes", mes}};
        for (String[] caso : casos) {
            ResponseEntity<RelatorioAnaliticoDTO> resposta = buscarRelatorio(token, caso[0]);
            assertThat(resposta.getStatusCode()).as("granularidade=%s", caso[0]).isEqualTo(HttpStatus.OK);
            RelatorioAnaliticoDTO relatorio = resposta.getBody();

            assertThat(relatorio.totalCadastros()).isEqualTo(antes.totalCadastros() + 1);
            assertThat(relatorio.totalEncaminhamentos()).isEqualTo(antes.totalEncaminhamentos() + 1);
            assertThat(relatorio.tempoMedioCadastroAtendimentoDias()).isNotNull().isGreaterThanOrEqualTo(0.0);
            assertThat(relatorio.percentualCadastrosEncaminhados()).isBetween(0.0, 100.0);
            assertThat(relatorio.taxaConversaoEncaminhamentoAtendimento()).isBetween(0.0, 100.0);

            // o período de hoje aparece nas três séries, que vêm em ordem crescente
            for (List<PontoSerieDTO> serie : List.of(relatorio.progressaoCadastros(),
                    relatorio.progressaoEncaminhamentos(), relatorio.progressaoAtendimentos())) {
                assertThat(periodos(serie)).as("granularidade=%s", caso[0]).contains(caso[1]).isSorted();
            }
        }
    }

    @Test
    void get_comGranularidadeInvalida_retorna400() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.PADRAO);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/dashboard/relatorios?granularidade=ano", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void get_comoEstagiario_retorna403() {
        String token = persistirComToken(profissionalRepository, jwtService, PapelProfissional.ESTAGIARIO);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/dashboard/relatorios", HttpMethod.GET,
                new HttpEntity<>(headersPara(token)), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
