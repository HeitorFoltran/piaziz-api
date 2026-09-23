package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.Size;

public record FichaPendenteRejeitarRequestDTO(
        @Size(max = 300, message = "motivo deve ter no máximo 300 caracteres")
        String motivo
) {
}
