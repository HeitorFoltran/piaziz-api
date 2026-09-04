package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Interacao;

import java.time.LocalDateTime;

public record InteracaoResponseDTO(
        Long id,
        Long fichaId,
        String autor,
        LocalDateTime dataInteracao,
        String texto
) {
    public static InteracaoResponseDTO from(Interacao i) {
        return new InteracaoResponseDTO(
                i.getId(),
                i.getFicha() != null ? i.getFicha().getId() : null,
                i.getAutor(),
                i.getDataInteracao(),
                i.getTexto()
        );
    }
}
