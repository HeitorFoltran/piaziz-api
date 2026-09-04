package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Encaminhamento;

import java.time.LocalDate;

public record EncaminhamentoResponseDTO(
        Long id,
        Long fichaId,
        Long servicoId,
        String servicoNome,
        String categoria,
        String profissional,
        LocalDate dataEncaminhamento,
        LocalDate dataRetorno,
        String descricao
) {
    public static EncaminhamentoResponseDTO from(Encaminhamento e) {
        return new EncaminhamentoResponseDTO(
                e.getId(),
                e.getFicha() != null ? e.getFicha().getId() : null,
                e.getServico() != null ? e.getServico().getId() : null,
                e.getServico() != null ? e.getServico().getNome() : null,
                e.getCategoria() != null ? e.getCategoria().name() : null,
                e.getProfissional(),
                e.getDataEncaminhamento(),
                e.getDataRetorno(),
                e.getDescricao()
        );
    }
}