package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfissionalRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        String nome,
        @NotBlank(message = "cpf é obrigatório")
        String cpf,
        String carteiraProfissional,
        Long servicoId,
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "senha é obrigatória")
        @Size(min = 6, message = "senha deve ter ao menos 6 caracteres")
        String senha,

        String role
) {
}
