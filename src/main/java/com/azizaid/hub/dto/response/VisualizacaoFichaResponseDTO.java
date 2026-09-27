package com.azizaid.hub.dto.response;

import java.time.LocalDateTime;

public record VisualizacaoFichaResponseDTO(
        Long profissionalId,
        String profissionalNome,
        LocalDateTime timestamp
) {
}
