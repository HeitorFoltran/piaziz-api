package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
