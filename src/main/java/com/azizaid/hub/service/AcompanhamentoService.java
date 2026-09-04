package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.model.Encaminhamento;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.util.CpfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AcompanhamentoService {

    private final FichaRepository fichaRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;

    public AcompanhamentoService(FichaRepository fichaRepository,
                                 EncaminhamentoRepository encaminhamentoRepository) {
        this.fichaRepository = fichaRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AcompanhamentoResumoDTO> listar(String termo, Long servicoId) {
        String t = (termo == null || termo.isBlank()) ? null : termo.trim();

        List<Ficha> fichas = fichaRepository.buscar(t);
        List<AcompanhamentoResumoDTO> resultado = new ArrayList<>();

        for (Ficha ficha : fichas) {
            Optional<Encaminhamento> recente =
                    encaminhamentoRepository.findTopByFichaIdOrderByDataEncaminhamentoDescIdDesc(ficha.getId());

            Long servicoIdRecente = recente.map(e -> e.getServico() != null ? e.getServico().getId() : null).orElse(null);
            String servicoNomeRecente = recente.map(e -> e.getServico() != null ? e.getServico().getNome() : null).orElse(null);

            if (servicoId != null && !servicoId.equals(servicoIdRecente)) {
                continue;
            }

            List<TipoAcompanhamentoResponseDTO> tipos = ficha.getTiposAcompanhamento() == null ? List.of()
                    : ficha.getTiposAcompanhamento().stream().map(TipoAcompanhamentoResponseDTO::from).toList();

            resultado.add(new AcompanhamentoResumoDTO(
                    ficha.getId(),
                    ficha.getNumeroCaso(),
                    ficha.getCodigoFicha(),
                    ficha.getNome(),
                    CpfUtils.mascarar(ficha.getCpf()),
                    servicoNomeRecente,
                    servicoIdRecente != null ? String.valueOf(servicoIdRecente) : null,
                    ficha.getStatus() != null ? ficha.getStatus().name() : null,
                    ficha.getDataAtualizacao(),
                    ficha.getDataCriacao(),
                    tipos
            ));
        }

        return resultado;
    }
}