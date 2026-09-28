package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.model.AcolhimentoEquipe;
import com.azizaid.hub.model.AvaliacaoSocioeconomica;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.HistoricoAtendimento;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.model.enums.TipoViolencia;
import com.azizaid.hub.repository.AcolhimentoEquipeRepository;
import com.azizaid.hub.repository.AvaliacaoSocioeconomicaRepository;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.repository.HistoricoAtendimentoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// As três partes do caso salvas por upsert, com o EntityAuditService de verdade (só o repositório do
// log é mock): confere a regra "só grava quando algo mudou" de ponta a ponta no serviço.
@ExtendWith(MockitoExtension.class)
class PartesDoCasoAuditoriaTest {

    @Mock
    FichaService fichaService;

    @Mock
    EntityAuditLogRepository entityAuditLogRepository;

    @Mock
    AvaliacaoSocioeconomicaRepository avaliacaoRepository;

    @Mock
    AcolhimentoEquipeRepository acolhimentoRepository;

    @Mock
    HistoricoAtendimentoRepository historicoRepository;

    AvaliacaoSocioeconomicaService avaliacaoService;
    AcolhimentoEquipeService acolhimentoService;
    HistoricoAtendimentoService historicoService;

    @BeforeEach
    void setUp() {
        EntityAuditService entityAuditService = new EntityAuditService(entityAuditLogRepository);
        avaliacaoService = new AvaliacaoSocioeconomicaService(avaliacaoRepository, fichaService, entityAuditService);
        acolhimentoService = new AcolhimentoEquipeService(acolhimentoRepository, fichaService, entityAuditService);
        historicoService = new HistoricoAtendimentoService(historicoRepository, fichaService, entityAuditService);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                7L, null, List.of(new SimpleGrantedAuthority("ROLE_PADRAO"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Ficha fichaCriadaEm(LocalDateTime dataCriacao) {
        Ficha ficha = Ficha.builder().id(1L).dataCriacao(dataCriacao).build();
        when(fichaService.buscarEntidade(1L)).thenReturn(ficha);
        return ficha;
    }

    private EntityAuditLog linhaGravada() {
        ArgumentCaptor<EntityAuditLog> captor = ArgumentCaptor.forClass(EntityAuditLog.class);
        verify(entityAuditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static AvaliacaoSocioeconomicaRequestDTO avaliacao(Boolean temRenda, BigDecimal valorRenda) {
        return new AvaliacaoSocioeconomicaRequestDTO(
                temRenda, valorRenda, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private static AvaliacaoSocioeconomica avaliacaoExistente(Ficha ficha, Boolean temRenda, BigDecimal valorRenda) {
        AvaliacaoSocioeconomica a = new AvaliacaoSocioeconomica();
        a.setId(5L);
        a.setFicha(ficha);
        a.setCriadoPorId(3L);
        a.setTemRenda(temRenda);
        a.setValorRenda(valorRenda);
        return a;
    }

    private void saveDevolveComId() {
        when(avaliacaoRepository.save(any(AvaliacaoSocioeconomica.class))).thenAnswer(inv -> {
            AvaliacaoSocioeconomica a = inv.getArgument(0);
            if (a.getId() == null) a.setId(5L);
            return a;
        });
    }

    @Test
    void avaliacao_salvaSemMudarNada_naoGrava() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        // Banco devolve 1500.00; a tela manda 1500. Não é alteração.
        when(avaliacaoRepository.findByFichaId(1L))
                .thenReturn(Optional.of(avaliacaoExistente(ficha, true, new BigDecimal("1500.00"))));
        saveDevolveComId();

        avaliacaoService.salvar(1L, avaliacao(true, new BigDecimal("1500")));

        verifyNoInteractions(entityAuditLogRepository);
    }

    @Test
    void avaliacao_existenteComCampoMudado_gravaEditou() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        when(avaliacaoRepository.findByFichaId(1L))
                .thenReturn(Optional.of(avaliacaoExistente(ficha, true, new BigDecimal("1500.00"))));
        saveDevolveComId();

        avaliacaoService.salvar(1L, avaliacao(false, null));

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
        assertThat(linha.getTipoEntidade()).isEqualTo("AvaliacaoSocioeconomica");
        assertThat(linha.getEntidadeId()).isEqualTo(5L);
        assertThat(linha.getEditorId()).isEqualTo(7L);
        assertThat(linha.getDonoId()).isEqualTo(3L);
        assertThat(linha.getDetalhe()).isNull();
    }

    @Test
    void avaliacao_novaNumaFichaCriadaAgora_naoGrava() {
        fichaCriadaEm(LocalDateTime.now());
        when(avaliacaoRepository.findByFichaId(1L)).thenReturn(Optional.empty());
        saveDevolveComId();

        avaliacaoService.salvar(1L, avaliacao(true, null));

        verifyNoInteractions(entityAuditLogRepository);
    }

    @Test
    void avaliacao_novaNumaFichaCriadaHaUmaHora_gravaPreencheuComOIdNovo() {
        fichaCriadaEm(LocalDateTime.now().minusHours(1));
        when(avaliacaoRepository.findByFichaId(1L)).thenReturn(Optional.empty());
        saveDevolveComId();

        avaliacaoService.salvar(1L, avaliacao(true, null));

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.PREENCHEU);
        assertThat(linha.getEntidadeId()).isEqualTo(5L);
        assertThat(linha.getDonoId()).isEqualTo(7L);
    }

    private static AcolhimentoEquipeRequestDTO acolhimento(Set<TipoViolencia> tipos, String observacoes) {
        return new AcolhimentoEquipeRequestDTO(null, null, null, tipos, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, observacoes, null);
    }

    private static AcolhimentoEquipeRequestDTO acolhimentoSugerindoHabitacao(Boolean sugereHabitacao) {
        return new AcolhimentoEquipeRequestDTO(null, null, null, Set.of(), null, null, null, null, null, null,
                null, null, null, null, true, "UBS", null, null, sugereHabitacao, null, null, null, null, null,
                null, null, null, "obs", null);
    }

    @Test
    void acolhimento_mesmosTiposDeViolenciaEmOutraOrdem_naoGrava() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        TipoViolencia[] tipos = TipoViolencia.values();
        AcolhimentoEquipe existente = new AcolhimentoEquipe();
        existente.setId(8L);
        existente.setFicha(ficha);
        existente.setTiposViolencia(new LinkedHashSet<>(List.of(tipos[0], tipos[1])));
        existente.setObservacoesRelevantes("obs");
        when(acolhimentoRepository.findByFichaId(1L)).thenReturn(Optional.of(existente));
        when(acolhimentoRepository.save(any(AcolhimentoEquipe.class))).thenAnswer(inv -> inv.getArgument(0));

        acolhimentoService.salvar(1L, acolhimento(new LinkedHashSet<>(List.of(tipos[1], tipos[0])), "obs"));

        verifyNoInteractions(entityAuditLogRepository);
    }

    @Test
    void acolhimento_comTipoDeViolenciaAMais_gravaEditou() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        TipoViolencia[] tipos = TipoViolencia.values();
        AcolhimentoEquipe existente = new AcolhimentoEquipe();
        existente.setId(8L);
        existente.setFicha(ficha);
        existente.setTiposViolencia(new LinkedHashSet<>(List.of(tipos[0])));
        when(acolhimentoRepository.findByFichaId(1L)).thenReturn(Optional.of(existente));
        when(acolhimentoRepository.save(any(AcolhimentoEquipe.class))).thenAnswer(inv -> inv.getArgument(0));

        acolhimentoService.salvar(1L, acolhimento(Set.of(tipos[0], tipos[1]), null));

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
        assertThat(linha.getTipoEntidade()).isEqualTo("AcolhimentoEquipe");
    }

    @Test
    void acolhimento_existenteMudandoSoUmaSugestao_gravaEditou() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        AcolhimentoEquipe existente = new AcolhimentoEquipe();
        existente.setId(8L);
        existente.setFicha(ficha);
        existente.setSugereSaudeGeral(true);
        existente.setSugereSaudeGeralQual("UBS");
        existente.setSugereHabitacao(false);
        existente.setObservacoesRelevantes("obs");
        when(acolhimentoRepository.findByFichaId(1L)).thenReturn(Optional.of(existente));
        when(acolhimentoRepository.save(any(AcolhimentoEquipe.class))).thenAnswer(inv -> inv.getArgument(0));

        acolhimentoService.salvar(1L, acolhimentoSugerindoHabitacao(true));

        EntityAuditLog linha = linhaGravada();
        assertThat(linha.getAcao()).isEqualTo(AcaoAlteracao.EDITOU);
        assertThat(linha.getTipoEntidade()).isEqualTo("AcolhimentoEquipe");
    }

    private static HistoricoAtendimentoRequestDTO historico(Boolean jaProcurouServico, String reacaoAgressor) {
        return new HistoricoAtendimentoRequestDTO(jaProcurouServico, null, null, null, null, null, null, null,
                reacaoAgressor);
    }

    @Test
    void historico_salvaSemMudarNada_naoGrava() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        HistoricoAtendimento existente = new HistoricoAtendimento();
        existente.setId(9L);
        existente.setFicha(ficha);
        existente.setJaProcurouServico(true);
        when(historicoRepository.findByFichaId(1L)).thenReturn(Optional.of(existente));
        when(historicoRepository.save(any(HistoricoAtendimento.class))).thenAnswer(inv -> inv.getArgument(0));

        // "" que a tela manda num campo vazio vale o mesmo que o null do banco.
        historicoService.salvar(1L, historico(true, ""));

        verifyNoInteractions(entityAuditLogRepository);
    }

    @Test
    void historico_existenteComCampoMudado_gravaEditou() {
        Ficha ficha = fichaCriadaEm(LocalDateTime.now().minusDays(3));
        HistoricoAtendimento existente = new HistoricoAtendimento();
        existente.setId(9L);
        existente.setFicha(ficha);
        existente.setJaProcurouServico(true);
        when(historicoRepository.findByFichaId(1L)).thenReturn(Optional.of(existente));
        when(historicoRepository.save(any(HistoricoAtendimento.class))).thenAnswer(inv -> inv.getArgument(0));

        historicoService.salvar(1L, historico(false, null));

        assertThat(linhaGravada().getTipoEntidade()).isEqualTo("HistoricoAtendimento");
    }
}
