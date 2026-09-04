package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;
import com.azizaid.hub.dto.response.EncaminhamentoResponseDTO;
import com.azizaid.hub.model.Encaminhamento;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EncaminhamentoService {

    private final EncaminhamentoRepository encaminhamentoRepository;
    private final ServicoRepository servicoRepository;
    private final FichaService fichaService;

    public EncaminhamentoService(EncaminhamentoRepository encaminhamentoRepository,
                                 ServicoRepository servicoRepository,
                                 FichaService fichaService) {
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.servicoRepository = servicoRepository;
        this.fichaService = fichaService;
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoResponseDTO> listarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        return encaminhamentoRepository
                .findByFichaIdOrderByDataEncaminhamentoDescIdDesc(fichaId)
                .stream()
                .map(EncaminhamentoResponseDTO::from)
                .toList();
    }

    @Transactional
    public EncaminhamentoResponseDTO criar(Long fichaId, EncaminhamentoRequestDTO dto) {
        Ficha ficha = fichaService.buscarEntidade(fichaId);

        Servico servico = servicoRepository.findById(dto.servicoId())
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado: " + dto.servicoId()));

        Encaminhamento encaminhamento = new Encaminhamento();
        encaminhamento.setFicha(ficha);
        encaminhamento.setServico(servico);
        encaminhamento.setCategoria(dto.categoria());
        encaminhamento.setProfissional(dto.profissional());
        encaminhamento.setDataEncaminhamento(dto.dataEncaminhamento());
        encaminhamento.setDataRetorno(dto.dataRetorno());
        encaminhamento.setDescricao(dto.descricao());

        Encaminhamento salvo = encaminhamentoRepository.save(encaminhamento);

        ficha.setDataAtualizacao(java.time.LocalDateTime.now());

        return EncaminhamentoResponseDTO.from(salvo);
    }
}