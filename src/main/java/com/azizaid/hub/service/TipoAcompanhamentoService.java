package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoAcompanhamentoService {

    private final TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    public TipoAcompanhamentoService(TipoAcompanhamentoRepository tipoAcompanhamentoRepository) {
        this.tipoAcompanhamentoRepository = tipoAcompanhamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoAcompanhamentoResponseDTO> listar() {
        return tipoAcompanhamentoRepository.findAll().stream()
                .map(TipoAcompanhamentoResponseDTO::from)
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
}
