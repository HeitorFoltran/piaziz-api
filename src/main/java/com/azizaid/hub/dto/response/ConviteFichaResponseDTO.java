package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.ConviteFicha;

import java.time.LocalDateTime;

public record ConviteFichaResponseDTO(
        Long id,
        String linkCompleto,
        LocalDateTime dataCriacao,
        LocalDateTime dataExpiracao,
        String status,
        LocalDateTime usadoEm
) {
    public static ConviteFichaResponseDTO comLink(ConviteFicha c, String linkCompleto) {
        return new ConviteFichaResponseDTO(
                c.getId(),
                linkCompleto,
                c.getDataCriacao(),
                c.getDataExpiracao(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getUsadoEm());
    }

    public static ConviteFichaResponseDTO semLink(ConviteFicha c) {
        return new ConviteFichaResponseDTO(
                c.getId(),
                null,
                c.getDataCriacao(),
                c.getDataExpiracao(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getUsadoEm());
    }
}
