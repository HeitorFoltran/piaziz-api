package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        // limite da coluna auth_audit_log.email_tentado: acima disso o registro da tentativa falharia com 500
        @NotBlank(message = "email é obrigatório")
        @Size(max = 150, message = "email deve ter no máximo 150 caracteres")
        String email,
        @NotBlank(message = "senha é obrigatória")
        String senha
) {
}
