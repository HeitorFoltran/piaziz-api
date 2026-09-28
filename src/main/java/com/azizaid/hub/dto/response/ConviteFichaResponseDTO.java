package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.ConviteFicha;

import java.time.LocalDateTime;

public record ConviteFichaResponseDTO(
        Long id,
        Long criadoPorId,
        String criadoPorNome,
        String linkCompleto,
        LocalDateTime dataCriacao,
        LocalDateTime dataExpiracao,
        String status,
        LocalDateTime usadoEm
) {
    public static ConviteFichaResponseDTO comLink(ConviteFicha c, String linkCompleto, String criadoPorNome) {
        return new ConviteFichaResponseDTO(
                c.getId(),
                c.getCriadoPorId(),
                criadoPorNome,
                linkCompleto,
                c.getDataCriacao(),
                c.getDataExpiracao(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getUsadoEm());
    }

    public static ConviteFichaResponseDTO semLink(ConviteFicha c, String criadoPorNome) {
        return comLink(c, null, criadoPorNome);
    }
}
