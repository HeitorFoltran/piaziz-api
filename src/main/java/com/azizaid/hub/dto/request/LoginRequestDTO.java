package com.azizaid.hub.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        // Username ou email. @JsonAlias("email"): o front antigo continua funcionando durante o deploy.
        // Limite da coluna auth_audit_log.email_tentado: acima disso o registro da tentativa falharia com 500.
        @JsonAlias("email")
        @NotBlank(message = "identificador é obrigatório")
        @Size(max = 150, message = "identificador deve ter no máximo 150 caracteres")
        String identificador,
        @NotBlank(message = "senha é obrigatória")
        String senha
) {
}
