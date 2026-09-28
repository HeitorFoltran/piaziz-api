package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.model.enums.NecessidadeImediata;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.model.enums.SupervisaoFilhos;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.azizaid.hub.support.FichaTestFactory.construirDtoValido;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichaServiceTest {

    @Mock
    FichaRepository fichaRepository;

    @Mock
    EntityAuditService entityAuditService;

    @Mock
    TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    @Mock
    LeituraAuditService leituraAuditService;

    FichaService fichaService;

    @BeforeEach
    void setUp() {
        fichaService = new FichaService(fichaRepository, entityAuditService, tipoAcompanhamentoRepository,
                leituraAuditService);
    }

    @Test
    void criar_comCpfNovo_salvaFicha() {
        when(fichaRepository.existsByCpf("12345678909")).thenReturn(false);
        when(fichaRepository.buscarMaiorSequencialCodigo()).thenReturn(0);
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        FichaRequestDTO dto = construirDtoValido("12345678909");

        fichaService.criar(dto);

        verify(fichaRepository).save(any(Ficha.class));
    }

    @Test
    void criar_comCpfDuplicado_lancaExcecaoENaoSalva() {
        when(fichaRepository.existsByCpf("12345678909")).thenReturn(true);
        FichaRequestDTO dto = construirDtoValido("12345678909");

        assertThrows(IllegalArgumentException.class, () -> fichaService.criar(dto));

        verify(fichaRepository, never()).save(any());
    }

    @Test
    void criar_comCpfInvalido_lancaExcecaoENaoSalva() {
        FichaRequestDTO dto = construirDtoValido("11111111111");

        assertThrows(IllegalArgumentException.class, () -> fichaService.criar(dto));

        verify(fichaRepository, never()).save(any());
    }

    private static FichaRequestDTO comNecessidades(FichaRequestDTO b, Set<NecessidadeImediata> necessidades,
                                                   String outraDescricao) {
        return new FichaRequestDTO(b.numeroCaso(), b.nome(), b.cpf(), b.idade(), b.telefone(), b.estadoCivil(),
                b.pessoasDependentes(), b.idadeFilhos(), b.nivelSeguranca(), b.tipoMoradia(),
                b.tipoMoradiaOutraDescricao(), b.qtdMoradores(), b.qtdFilhos(), b.ondeMoramFilhos(),
                b.supervisaoFilhos(), b.vagasNecessarias(), necessidades, outraDescricao, b.status());
    }

    private Ficha criarESalvar(FichaRequestDTO dto) {
        when(fichaRepository.existsByCpf(dto.cpf())).thenReturn(false);
        when(fichaRepository.buscarMaiorSequencialCodigo()).thenReturn(0);
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));
        fichaService.criar(dto);
        ArgumentCaptor<Ficha> captor = ArgumentCaptor.forClass(Ficha.class);
        verify(fichaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void criar_comOutraDescricaoPreenchida_marcaOutro() {
        // Set.of() é imutável: o serviço não pode acrescentar OUTRO direto nele.
        Ficha salva = criarESalvar(comNecessidades(construirDtoValido("12345678909"), Set.of(), "Documentos"));

        assertEquals(Set.of(NecessidadeImediata.OUTRO), salva.getNecessidadesImediatas());
    }

    @Test
    void criar_comOutraDescricaoEmBranco_naoMarcaOutro() {
        Ficha salva = criarESalvar(comNecessidades(construirDtoValido("12345678909"), Set.of(), "  "));

        assertTrue(salva.getNecessidadesImediatas().isEmpty());
    }

    @Test
    void atualizar_comOutraDescricaoPreenchida_marcaOutroSemPerderAsOutras() {
        Ficha ficha = Ficha.builder().id(1L).cpf("12345678909").build();
        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atualizar(1L, comNecessidades(construirDtoValido("12345678909"),
                Set.of(NecessidadeImediata.APOIO_MORADIA), "Documentos"));

        assertEquals(Set.of(NecessidadeImediata.APOIO_MORADIA, NecessidadeImediata.OUTRO),
                ficha.getNecessidadesImediatas());
    }

    @Test
    void atribuirTiposAcompanhamento_comIdsValidos_atualiza() {
        Ficha ficha = Ficha.builder().id(1L).build();
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        TipoAcompanhamento t2 = TipoAcompanhamento.builder().id(20L).nome("Psicológico").build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(t1, t2));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of(10L, 20L));

        assertEquals(Set.of(t1, t2), ficha.getTiposAcompanhamento());
    }

    @Test
    void atribuirTiposAcompanhamento_comIdInexistente_lancaExcecao() {
        Ficha ficha = Ficha.builder().id(1L).build();
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(10L, 999L))).thenReturn(List.of(t1));

        assertThrows(IllegalArgumentException.class,
                () -> fichaService.atribuirTiposAcompanhamento(1L, List.of(10L, 999L)));

        verify(fichaRepository, never()).save(any());
    }

    @Test
    void atribuirTiposAcompanhamento_comListaVazia_removeTodasAsTags() {
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        Ficha ficha = Ficha.builder().id(1L).tiposAcompanhamento(new HashSet<>(Set.of(t1))).build();

        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of())).thenReturn(List.of());
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of());

        assertTrue(ficha.getTiposAcompanhamento().isEmpty());
    }

    // Ficha no estado que o PUT com construirDtoValido(cpf) deixaria, criada pelo profissional 7.
    private Ficha fichaIgualAoDto(String cpf, StatusFicha status) {
        FichaRequestDTO dto = construirDtoValido(cpf);
        return Ficha.builder().id(1L).criadoPorId(7L).numeroCaso("2026/000001").nome(dto.nome()).cpf(dto.cpf())
                .idade(dto.idade()).telefone(dto.telefone()).estadoCivil(dto.estadoCivil())
                .pessoasDependentes(dto.pessoasDependentes()).nivelSeguranca(dto.nivelSeguranca())
                .qtdMoradores(dto.qtdMoradores()).qtdFilhos(dto.qtdFilhos()).status(status).build();
    }

    private static FichaRequestDTO dtoCom(String cpf, String telefone, SupervisaoFilhos supervisao, StatusFicha status) {
        FichaRequestDTO base = construirDtoValido(cpf);
        return new FichaRequestDTO(base.numeroCaso(), base.nome(), base.cpf(), base.idade(), telefone,
                base.estadoCivil(), base.pessoasDependentes(), base.idadeFilhos(), base.nivelSeguranca(),
                base.tipoMoradia(), base.tipoMoradiaOutraDescricao(), base.qtdMoradores(), base.qtdFilhos(),
                base.ondeMoramFilhos(), supervisao, base.vagasNecessarias(), base.necessidadesImediatas(),
                base.necessidadeOutraDescricao(), status);
    }

    private void stubAtualizar(Ficha ficha) {
        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void atualizar_semMudarNada_naoRegistra() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizar(1L, construirDtoValido("12345678909"));

        verifyNoInteractions(entityAuditService);
    }

    @Test
    void atualizar_mudandoTelefone_registraEditouSemValorDeCampo() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizar(1L, dtoCom("12345678909", "45988887777", null, null));

        verify(entityAuditService).registrar(eq("Ficha"), eq(1L), eq(7L), eq(1L), eq(AcaoAlteracao.EDITOU), isNull());
        verifyNoMoreInteractions(entityAuditService);
    }

    @Test
    void atualizar_mudandoSoSupervisaoFilhos_registraEditou() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizar(1L, dtoCom("12345678909", "11999999999", SupervisaoFilhos.SIM, null));

        verify(entityAuditService).registrar(eq("Ficha"), eq(1L), eq(7L), eq(1L), eq(AcaoAlteracao.EDITOU), isNull());
    }

    @Test
    void atualizar_mudandoSoOStatus_registraSoMudouStatus() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizar(1L, dtoCom("12345678909", "11999999999", null, StatusFicha.ARQUIVADO));

        verify(entityAuditService).registrar("StatusFicha", 1L, 7L, 1L, AcaoAlteracao.MUDOU_STATUS, "Ativo -> Arquivado");
        verifyNoMoreInteractions(entityAuditService);
    }

    @Test
    void atualizarStatus_deAtivoParaArquivado_registraMudouStatus() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizarStatus(1L, StatusFicha.ARQUIVADO);

        verify(entityAuditService).registrar("StatusFicha", 1L, 7L, 1L, AcaoAlteracao.MUDOU_STATUS, "Ativo -> Arquivado");
    }

    @Test
    void atualizarStatus_paraOMesmoStatus_naoRegistra() {
        stubAtualizar(fichaIgualAoDto("12345678909", StatusFicha.ATIVO));

        fichaService.atualizarStatus(1L, StatusFicha.ATIVO);

        verifyNoInteractions(entityAuditService);
    }

    @Test
    void atribuirTiposAcompanhamento_mesmosIdsEmOutraOrdem_naoRegistra() {
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        TipoAcompanhamento t2 = TipoAcompanhamento.builder().id(20L).nome("Psicológico").build();
        Ficha ficha = Ficha.builder().id(1L).tiposAcompanhamento(new HashSet<>(Set.of(t1, t2))).build();
        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(20L, 10L))).thenReturn(List.of(t2, t1));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of(20L, 10L));

        verifyNoInteractions(entityAuditService);
    }

    @Test
    void atribuirTiposAcompanhamento_comUmIdAMais_registraAlterouTipos() {
        TipoAcompanhamento t1 = TipoAcompanhamento.builder().id(10L).nome("Jurídico").build();
        TipoAcompanhamento t2 = TipoAcompanhamento.builder().id(20L).nome("Psicológico").build();
        Ficha ficha = Ficha.builder().id(1L).criadoPorId(7L).tiposAcompanhamento(new HashSet<>(Set.of(t1))).build();
        when(fichaRepository.findById(1L)).thenReturn(Optional.of(ficha));
        when(tipoAcompanhamentoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(t1, t2));
        when(fichaRepository.save(any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        fichaService.atribuirTiposAcompanhamento(1L, List.of(10L, 20L));

        verify(entityAuditService).registrar(eq("TiposAcompanhamento"), eq(1L), eq(7L), eq(1L),
                eq(AcaoAlteracao.ALTEROU_TIPOS), isNull());
    }
}
