package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.FichaPendente;

import java.time.LocalDateTime;

public record FichaPendenteResponseDTO(
        Long id,
        String nome,
        String cpf,
        String telefone,
        Integer idade,
        String situacaoRelatada,
        LocalDateTime dataSubmissao,
        String status,
        Long revisadoPorId,
        LocalDateTime dataRevisao,
        String motivoRejeicao,
        Long fichaId
) {
    public static FichaPendenteResponseDTO from(FichaPendente f) {
        return new FichaPendenteResponseDTO(
                f.getId(),
                f.getNome(),
                f.getCpf(),
                f.getTelefone(),
                f.getIdade(),
                f.getSituacaoRelatada(),
                f.getDataSubmissao(),
                f.getStatus() != null ? f.getStatus().name() : null,
                f.getRevisadoPorId(),
                f.getDataRevisao(),
                f.getMotivoRejeicao(),
                f.getFichaId());
    }
}
