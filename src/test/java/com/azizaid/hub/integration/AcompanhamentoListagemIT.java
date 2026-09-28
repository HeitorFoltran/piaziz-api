package com.azizaid.hub.integration;

import com.azizaid.hub.dto.request.FiltroAcompanhamentos;
import com.azizaid.hub.dto.request.FiltroAcompanhamentos.CampoData;
import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.PaginaDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.service.AcompanhamentoService;
import com.azizaid.hub.support.PostgresTestContainerConfig;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// A listagem é SQL nativo: só um Postgres de verdade confere filtros, ordem e tipos das colunas.
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        // Sem isto, cada sessão despeja as métricas no log do build.
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=warn"
})
class AcompanhamentoListagemIT extends PostgresTestContainerConfig {

    private static final long PROFISSIONAL_1 = 9001L;
    private static final long PROFISSIONAL_2 = 9002L;

    @Autowired
    AcompanhamentoService acompanhamentoService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    long servico1;
    long servico2;
    long tipo1;
    long tipo2;
    String nomeServico1;
    String nomeTipo1;
    String nomeTipo2;

    // Ordem por data_atualizacao DESC: D, B, A, C, E.
    long fichaA;
    long fichaB;
    long fichaC;
    long fichaD;
    long fichaE;

    @BeforeEach
    void montarCasos() {
        jdbc.execute("TRUNCATE ficha CASCADE");

        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        nomeServico1 = "Serviço 1 " + sufixo;
        servico1 = inserirServico(nomeServico1);
        servico2 = inserirServico("Serviço 2 " + sufixo);
        nomeTipo1 = "A tipo " + sufixo;
        nomeTipo2 = "B tipo " + sufixo;
        tipo1 = inserirTipo(nomeTipo1);
        tipo2 = inserirTipo(nomeTipo2);

        fichaA = inserirFicha("Ana Silva", "11111111111", "ATIVO", PROFISSIONAL_1,
                LocalDateTime.of(2026, 1, 5, 10, 0), LocalDateTime.of(2026, 3, 10, 23, 30));
        vincularTipo(fichaA, tipo1);
        inserirEncaminhamento(fichaA, servico1, LocalDate.of(2026, 3, 1));
        inserirEncaminhamento(fichaA, servico2, LocalDate.of(2026, 3, 5));

        // Encaminhamento sem data: no Postgres, DESC põe nulo primeiro, então o mais recente é o do serviço 1.
        fichaB = inserirFicha("Beatriz Souza", "22222222222", "PAUSADO", PROFISSIONAL_2,
                LocalDateTime.of(2026, 2, 1, 9, 0), LocalDateTime.of(2026, 3, 11, 0, 0));
        vincularTipo(fichaB, tipo2);
        vincularTipo(fichaB, tipo1);
        inserirEncaminhamento(fichaB, servico2, LocalDate.of(2026, 3, 9));
        inserirEncaminhamento(fichaB, servico1, null);

        fichaC = inserirFicha("Clara Lima", "33333333333", "ARQUIVADO", null,
                LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 3, 9, 12, 0));
        inserirEncaminhamento(fichaC, servico1, LocalDate.of(2026, 2, 1));

        fichaD = inserirFicha("Dora Nunes", "44444444444", "ATIVO", PROFISSIONAL_1,
                LocalDateTime.of(2026, 3, 10, 23, 30), LocalDateTime.of(2026, 3, 12, 9, 0));
        vincularTipo(fichaD, tipo2);

        fichaE = inserirFicha("Eva Rocha", "55555555555", "ATIVO", PROFISSIONAL_2,
                LocalDateTime.of(2026, 3, 11, 0, 0), LocalDateTime.of(2026, 3, 8, 8, 0));
        inserirEncaminhamento(fichaE, servico2, LocalDate.of(2026, 3, 1));
    }

    @AfterEach
    void limparUsuario() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semFiltros_trazTodasOrdenadasPorAtualizacao() {
        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(), 0, 20);

        assertThat(ids(pagina)).containsExactly(fichaD, fichaB, fichaA, fichaC, fichaE);
        assertThat(pagina.pagina()).isZero();
        assertThat(pagina.tamanho()).isEqualTo(20);
        assertThat(pagina.totalItens()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
    }

    @Test
    void itens_trazemColunasServicoMaisRecenteETiposDeCadaFicha() {
        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(), 0, 20);

        AcompanhamentoResumoDTO b = item(pagina, fichaB);
        assertThat(b.nome()).isEqualTo("Beatriz Souza");
        assertThat(b.cpf()).isEqualTo("***.***.***-22");
        assertThat(b.status()).isEqualTo("PAUSADO");
        assertThat(b.encaminhamento()).isEqualTo(nomeServico1);
        assertThat(b.tipoEncaminhamento()).isEqualTo(String.valueOf(servico1));
        assertThat(b.criadoPorId()).isEqualTo(PROFISSIONAL_2);
        assertThat(b.dataAtualizacao()).isEqualTo(LocalDateTime.of(2026, 3, 11, 0, 0));
        assertThat(b.dataCriacao()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 0));
        assertThat(b.numeroCaso()).isEqualTo("CASO-" + fichaB);
        assertThat(b.codigoFicha()).isEqualTo("F-" + fichaB);
        assertThat(b.tiposAcompanhamento()).extracting(TipoAcompanhamentoResponseDTO::nome)
                .containsExactly(nomeTipo1, nomeTipo2);

        assertThat(item(pagina, fichaA).tiposAcompanhamento()).extracting(TipoAcompanhamentoResponseDTO::id)
                .containsExactly(tipo1);
        assertThat(item(pagina, fichaD).tiposAcompanhamento()).extracting(TipoAcompanhamentoResponseDTO::id)
                .containsExactly(tipo2);
        assertThat(item(pagina, fichaC).tiposAcompanhamento()).isEmpty();

        AcompanhamentoResumoDTO d = item(pagina, fichaD);
        assertThat(d.encaminhamento()).isNull();
        assertThat(d.tipoEncaminhamento()).isNull();
        assertThat(item(pagina, fichaC).criadoPorId()).isNull();
    }

    @Test
    void busca_porNomeCodigoCpfOuNumeroDoCaso() {
        assertThat(ids(listar(filtroComBusca("beatriz"), 0, 20))).containsExactly(fichaB);
        assertThat(ids(listar(filtroComBusca("f-" + fichaC), 0, 20))).containsExactly(fichaC);
        assertThat(ids(listar(filtroComBusca("4444"), 0, 20))).containsExactly(fichaD);
        assertThat(ids(listar(filtroComBusca("CASO-" + fichaE), 0, 20))).containsExactly(fichaE);
        assertThat(ids(listar(filtroComBusca("   "), 0, 20))).hasSize(5);
    }

    @Test
    void status_filtraPeloStatusDaFicha() {
        assertThat(ids(listar(filtro(StatusFicha.PAUSADO), 0, 20))).containsExactly(fichaB);
        assertThat(ids(listar(filtro(StatusFicha.ATIVO), 0, 20))).containsExactly(fichaD, fichaA, fichaE);
    }

    @Test
    void status_arquivadoIncluiONomeAntigoEncerrado() {
        jdbc.update("UPDATE ficha SET status = 'ENCERRADO' WHERE id = ?", fichaC);

        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(StatusFicha.ARQUIVADO), 0, 20);

        assertThat(ids(pagina)).containsExactly(fichaC);
        assertThat(pagina.itens().get(0).status()).isEqualTo("ARQUIVADO");
    }

    @Test
    void tipo_filtraFichasComOTipo() {
        assertThat(ids(listar(filtroComTipo(tipo1), 0, 20))).containsExactly(fichaB, fichaA);
        assertThat(ids(listar(filtroComTipo(tipo2), 0, 20))).containsExactly(fichaD, fichaB);
    }

    @Test
    void servico_usaSoOEncaminhamentoMaisRecente() {
        // A tem serviço 1 e depois serviço 2; B tem serviço 2 datado e serviço 1 sem data (nulo vem primeiro).
        assertThat(ids(listar(filtroComServico(servico2), 0, 20))).containsExactly(fichaA, fichaE);
        assertThat(ids(listar(filtroComServico(servico1), 0, 20))).containsExactly(fichaB, fichaC);
    }

    @Test
    void meus_trazSoAsFichasCriadasPeloUsuarioLogado() {
        logarComo(PROFISSIONAL_1);

        FiltroAcompanhamentos meus = new FiltroAcompanhamentos(null, null, null, null, true, null, null, null);

        assertThat(ids(listar(meus, 0, 20))).containsExactly(fichaD, fichaA);
    }

    @Test
    void datasDeAtualizacao_saoInclusivasNasDuasPontas() {
        LocalDate dia10 = LocalDate.of(2026, 3, 10);

        // A às 23:30 do dia 10 entra; B às 00:00 do dia 11 não.
        assertThat(ids(listar(filtroComDatas(CampoData.DATA_ATUALIZACAO, dia10, dia10), 0, 20)))
                .containsExactly(fichaA);
        assertThat(ids(listar(filtroComDatas(null, LocalDate.of(2026, 3, 11), null), 0, 20)))
                .containsExactly(fichaD, fichaB);
        assertThat(ids(listar(filtroComDatas(null, null, LocalDate.of(2026, 3, 9)), 0, 20)))
                .containsExactly(fichaC, fichaE);
    }

    @Test
    void datasDeCriacao_usamAColunaDeCriacao() {
        LocalDate dia10 = LocalDate.of(2026, 3, 10);

        // D foi criada às 23:30 do dia 10; E às 00:00 do dia 11 fica fora.
        assertThat(ids(listar(filtroComDatas(CampoData.DATA_CRIACAO, dia10, dia10), 0, 20)))
                .containsExactly(fichaD);
        assertThat(ids(listar(filtroComDatas(CampoData.DATA_CRIACAO, LocalDate.of(2026, 3, 1), null), 0, 20)))
                .containsExactly(fichaD, fichaC, fichaE);
    }

    @Test
    void filtrosCombinados() {
        FiltroAcompanhamentos ativoComTipo2 =
                new FiltroAcompanhamentos(null, null, StatusFicha.ATIVO, tipo2, false, null, null, null);
        assertThat(ids(listar(ativoComTipo2, 0, 20))).containsExactly(fichaD);

        logarComo(PROFISSIONAL_1);
        FiltroAcompanhamentos meusNoServico2 =
                new FiltroAcompanhamentos(null, servico2, null, null, true, null, null, null);
        assertThat(ids(listar(meusNoServico2, 0, 20))).containsExactly(fichaA);
    }

    @Test
    void paginas_cobremTodasAsFichasSemRepetirNemPular() {
        List<Long> todos = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(), p, 2);
            assertThat(pagina.pagina()).isEqualTo(p);
            assertThat(pagina.tamanho()).isEqualTo(2);
            assertThat(pagina.totalItens()).isEqualTo(5);
            assertThat(pagina.totalPaginas()).isEqualTo(3);
            todos.addAll(ids(pagina));
        }

        assertThat(todos).containsExactly(fichaD, fichaB, fichaA, fichaC, fichaE);
    }

    @Test
    void desempate_peloIdQuandoAAtualizacaoEIgual() {
        jdbc.update("UPDATE ficha SET data_atualizacao = ?", Timestamp.valueOf(LocalDateTime.of(2026, 4, 1, 10, 0)));

        List<Long> todos = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            todos.addAll(ids(listar(filtro(), p, 2)));
        }

        assertThat(todos).containsExactly(fichaE, fichaD, fichaC, fichaB, fichaA);
    }

    @Test
    void paginaAlemDoFim_voltaVaziaComOTotalCerto() {
        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(), 5, 2);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(5);
        assertThat(pagina.totalItens()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    void semResultado_voltaZeroPaginas() {
        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtroComBusca("ninguém com esse nome"), 0, 20);

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.totalItens()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    void umaPaginaCheia_fazNoMaximoTresConsultas() {
        for (int i = 0; i < 10; i++) {
            long ficha = inserirFicha("Extra " + i, "9999999990" + i, "ATIVO", PROFISSIONAL_1,
                    LocalDateTime.of(2026, 5, 1, 10, i), LocalDateTime.of(2026, 5, 2, 10, i));
            vincularTipo(ficha, tipo1);
            vincularTipo(ficha, tipo2);
            inserirEncaminhamento(ficha, servico1, LocalDate.of(2026, 5, 1));
            inserirEncaminhamento(ficha, servico2, LocalDate.of(2026, 5, 2));
        }
        Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertThat(estatisticas.isStatisticsEnabled()).isTrue();
        estatisticas.clear();

        PaginaDTO<AcompanhamentoResumoDTO> pagina = listar(filtro(), 0, 10);

        assertThat(pagina.itens()).hasSize(10);
        assertThat(pagina.itens()).allSatisfy(item -> assertThat(item.tiposAcompanhamento()).hasSize(2));
        // Linhas, COUNT e tipos. Sem o N+1 de antes, não cresce com o tamanho da página.
        assertThat(estatisticas.getPrepareStatementCount()).isBetween(1L, 3L);
    }

    // --- apoio ---

    private PaginaDTO<AcompanhamentoResumoDTO> listar(FiltroAcompanhamentos filtro, int pagina, int tamanho) {
        return acompanhamentoService.listar(filtro, pagina, tamanho);
    }

    private static FiltroAcompanhamentos filtro() {
        return new FiltroAcompanhamentos(null, null, null, null, false, null, null, null);
    }

    private static FiltroAcompanhamentos filtro(StatusFicha status) {
        return new FiltroAcompanhamentos(null, null, status, null, false, null, null, null);
    }

    private static FiltroAcompanhamentos filtroComBusca(String q) {
        return new FiltroAcompanhamentos(q, null, null, null, false, null, null, null);
    }

    private static FiltroAcompanhamentos filtroComTipo(long tipoId) {
        return new FiltroAcompanhamentos(null, null, null, tipoId, false, null, null, null);
    }

    private static FiltroAcompanhamentos filtroComServico(long servicoId) {
        return new FiltroAcompanhamentos(null, servicoId, null, null, false, null, null, null);
    }

    private static FiltroAcompanhamentos filtroComDatas(CampoData campo, LocalDate de, LocalDate ate) {
        return new FiltroAcompanhamentos(null, null, null, null, false, campo, de, ate);
    }

    private static List<Long> ids(PaginaDTO<AcompanhamentoResumoDTO> pagina) {
        return pagina.itens().stream().map(AcompanhamentoResumoDTO::id).toList();
    }

    private static AcompanhamentoResumoDTO item(PaginaDTO<AcompanhamentoResumoDTO> pagina, long id) {
        return pagina.itens().stream().filter(i -> i.id() == id).findFirst().orElseThrow();
    }

    private static void logarComo(long profissionalId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                profissionalId, null, List.of(new SimpleGrantedAuthority("ROLE_PADRAO"))));
    }

    // Direto no banco: o service grava as datas com o relógio e ignora criado_por_id sem usuário logado.
    private long inserirFicha(String nome, String cpf, String status, Long criadoPorId,
                              LocalDateTime criacao, LocalDateTime atualizacao) {
        Long id = jdbc.queryForObject("""
                        INSERT INTO ficha (codigo_ficha, numero_caso, nome, cpf, status, criado_por_id,
                                           data_criacao, data_atualizacao)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                        """, Long.class,
                "TMP-" + UUID.randomUUID().toString().substring(0, 8), "TMP", nome, cpf, status, criadoPorId,
                Timestamp.valueOf(criacao), Timestamp.valueOf(atualizacao));
        jdbc.update("UPDATE ficha SET codigo_ficha = ?, numero_caso = ? WHERE id = ?", "F-" + id, "CASO-" + id, id);
        return id;
    }

    private long inserirServico(String nome) {
        return jdbc.queryForObject("INSERT INTO servico (nome) VALUES (?) RETURNING id", Long.class, nome);
    }

    private long inserirTipo(String nome) {
        return jdbc.queryForObject("INSERT INTO tipo_acompanhamento (nome) VALUES (?) RETURNING id", Long.class, nome);
    }

    private void vincularTipo(long fichaId, long tipoId) {
        jdbc.update("INSERT INTO ficha_tipo_acompanhamento (ficha_id, tipo_acompanhamento_id) VALUES (?, ?)",
                fichaId, tipoId);
    }

    private void inserirEncaminhamento(long fichaId, long servicoId, LocalDate data) {
        jdbc.update("INSERT INTO encaminhamento (ficha_id, servico_id, data_encaminhamento) VALUES (?, ?, ?)",
                fichaId, servicoId, data);
    }
}
