package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "email é obrigatório")
        String email,
        @NotBlank(message = "senha é obrigatória")
        String senha
) {
}
