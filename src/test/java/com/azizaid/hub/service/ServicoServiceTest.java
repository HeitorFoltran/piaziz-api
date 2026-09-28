package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    ServicoRepository servicoRepository;

    ServicoService servicoService;

    @BeforeEach
    void setUp() {
        servicoService = new ServicoService(servicoRepository);
    }

    @Test
    void atualizar_comIdExistente_atualizaNome() {
        Servico servico = Servico.builder().id(1L).nome("Antigo").build();
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(servico)).thenReturn(servico);

        var resposta = servicoService.atualizar(1L, new ServicoRequestDTO("Novo"));

        assertThat(resposta.nome()).isEqualTo("Novo");
    }

    @Test
    void atualizar_comIdInexistente_lancaRecursoNaoEncontrado() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> servicoService.atualizar(99L, new ServicoRequestDTO("Novo")));
    }

    @Test
    void criar_comNomeJaExistente_lancaExcecao() {
        when(servicoRepository.existsByNomeIgnoreCase("CRAS")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicoService.criar(new ServicoRequestDTO("CRAS")));

        assertThat(ex.getMessage()).isEqualTo("Já existe um serviço com este nome");
        verify(servicoRepository, never()).save(any());
    }

    @Test
    void criar_aplicaTrimAntesDeChecarESalvar() {
        when(servicoRepository.existsByNomeIgnoreCase("CREAS")).thenReturn(false);
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = servicoService.criar(new ServicoRequestDTO("  CREAS  "));

        ArgumentCaptor<Servico> captor = ArgumentCaptor.forClass(Servico.class);
        verify(servicoRepository).save(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("CREAS");
        assertThat(resposta.nome()).isEqualTo("CREAS");
    }

    @Test
    void atualizar_paraNomeDeOutroServico_lancaExcecao() {
        Servico servico = Servico.builder().id(1L).nome("CRAS").build();
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByNomeIgnoreCase("CREAS")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.atualizar(1L, new ServicoRequestDTO(" CREAS ")));
        verify(servicoRepository, never()).save(any());
    }

    @Test
    void atualizar_proprioNomeSoMudandoMaiusculas_passa() {
        Servico servico = Servico.builder().id(1L).nome("cras").build();
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(servico)).thenReturn(servico);

        var resposta = servicoService.atualizar(1L, new ServicoRequestDTO("CRAS"));

        assertThat(resposta.nome()).isEqualTo("CRAS");
        verify(servicoRepository, never()).existsByNomeIgnoreCase(any());
    }

    @Test
    void excluir_semUso_apaga() {
        Servico servico = Servico.builder().id(1L).nome("Sem uso").build();
        when(servicoRepository.buscarParaExclusao(1L)).thenReturn(Optional.of(servico));

        servicoService.excluir(1L);

        verify(servicoRepository).delete(servico);
    }

    @Test
    void excluir_comEncaminhamentoOuProfissional_lancaExcecaoENaoApaga() {
        Servico servico = Servico.builder().id(1L).nome("Em uso").build();
        when(servicoRepository.buscarParaExclusao(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.contarEncaminhamentos(1L)).thenReturn(3L);
        when(servicoRepository.contarProfissionais(1L)).thenReturn(1L);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> servicoService.excluir(1L));

        assertThat(erro.getMessage()).contains("3 encaminhamento(s) e 1 profissional(is)");
        verify(servicoRepository, never()).delete(any());
    }

    @Test
    void excluir_inexistente_lancaNaoEncontrado() {
        when(servicoRepository.buscarParaExclusao(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servicoService.excluir(9L));
    }

    @Test
    void listar_marcaQuemEstaEmUso() {
        when(servicoRepository.findAll()).thenReturn(List.of(
                Servico.builder().id(1L).nome("Usado").build(),
                Servico.builder().id(2L).nome("Livre").build()));
        when(servicoRepository.idsEmUso()).thenReturn(List.of(1L));

        assertThat(servicoService.listar()).extracting("emUso").containsExactly(true, false);
    }
}
