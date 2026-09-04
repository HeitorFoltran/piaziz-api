package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.dto.response.ServicoResponseDTO;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

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

    @Transactional
    public ServicoResponseDTO criar(ServicoRequestDTO dto) {
        Servico servico = new Servico();
        servico.setNome(dto.nome());
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado: " + id));
        servico.setNome(dto.nome());
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }
}
