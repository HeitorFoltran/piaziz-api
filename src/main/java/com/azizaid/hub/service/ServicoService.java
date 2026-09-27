package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.dto.response.ServicoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private static final String MENSAGEM_NOME_DUPLICADO = "Já existe um serviço com este nome";

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> listar() {
        return servicoRepository.findAll().stream()
                .map(ServicoResponseDTO::from)
                .toList();
    }

    // Sem unique index no banco, de propósito: cadastro de serviço é raro e feito por poucas pessoas,
    // então a checagem aqui basta. A index entra junto com o Flyway, se um dia for preciso.
    @Transactional
    public ServicoResponseDTO criar(ServicoRequestDTO dto) {
        String nome = dto.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException(MENSAGEM_NOME_DUPLICADO);
        }
        Servico servico = new Servico();
        servico.setNome(nome);
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));

        String nome = dto.nome().trim();
        if (!servico.getNome().equalsIgnoreCase(nome) && servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException(MENSAGEM_NOME_DUPLICADO);
        }

        servico.setNome(nome);
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }
}
