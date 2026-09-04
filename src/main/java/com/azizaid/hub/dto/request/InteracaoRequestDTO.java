package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InteracaoRequestDTO(
        String autor,
        @NotBlank(message = "texto é obrigatório")
        String texto
) {
}
