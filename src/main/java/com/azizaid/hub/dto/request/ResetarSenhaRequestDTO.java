package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetarSenhaRequestDTO(
        @NotBlank(message = "senhaProvisoria é obrigatória")
        @Size(min = 8, max = 72, message = "senhaProvisoria deve ter entre 8 e 72 caracteres")
        String senhaProvisoria
) {
}
