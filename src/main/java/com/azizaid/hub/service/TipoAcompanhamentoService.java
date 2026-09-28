package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoCadastroDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TipoAcompanhamentoService {

    private final TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    public TipoAcompanhamentoService(TipoAcompanhamentoRepository tipoAcompanhamentoRepository) {
        this.tipoAcompanhamentoRepository = tipoAcompanhamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoAcompanhamentoCadastroDTO> listar() {
        Set<Long> emUso = new HashSet<>(tipoAcompanhamentoRepository.idsEmUso());
        return tipoAcompanhamentoRepository.findAll().stream()
                .map(t -> TipoAcompanhamentoCadastroDTO.from(t, emUso.contains(t.getId())))
                .toList();
    }

    @Transactional
    public TipoAcompanhamentoResponseDTO criar(TipoAcompanhamentoRequestDTO dto) {
        if (tipoAcompanhamentoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new IllegalArgumentException("Já existe um tipo de acompanhamento com este nome");
        }
        TipoAcompanhamento tipo = new TipoAcompanhamento();
        tipo.setNome(dto.nome());
        return TipoAcompanhamentoResponseDTO.from(tipoAcompanhamentoRepository.save(tipo));
    }

    @Transactional
    public TipoAcompanhamentoResponseDTO atualizar(Long id, TipoAcompanhamentoRequestDTO dto) {
        TipoAcompanhamento tipo = tipoAcompanhamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Tipo de acompanhamento", id));

        if (!tipo.getNome().equalsIgnoreCase(dto.nome())
                && tipoAcompanhamentoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new IllegalArgumentException("Já existe um tipo de acompanhamento com este nome");
        }

        tipo.setNome(dto.nome());
        return TipoAcompanhamentoResponseDTO.from(tipoAcompanhamentoRepository.save(tipo));
    }

    // Só exclui tipo que não está em nenhum caso. Excluir um tipo em uso apagaria a atribuição dos
    // casos em cascata (FK ON DELETE CASCADE), sem ninguém perceber.
    @Transactional
    public void excluir(Long id) {
        TipoAcompanhamento tipo = tipoAcompanhamentoRepository.buscarParaExclusao(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Tipo de acompanhamento", id));

        long fichas = tipoAcompanhamentoRepository.contarFichas(id);
        if (fichas > 0) {
            throw new IllegalArgumentException("Não é possível excluir: o tipo está atribuído a "
                    + fichas + " caso(s).");
        }
        tipoAcompanhamentoRepository.delete(tipo);
    }
}
