package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequestDTO(
        @NotBlank(message = "senhaAtual é obrigatória")
        String senhaAtual,
        @NotBlank(message = "novaSenha é obrigatória")
        @Size(min = 8, max = 72, message = "novaSenha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {
}
