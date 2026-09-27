package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Edição de conta: os mesmos campos da criação, menos a senha, mais ativo. Validações de
// username/email/CPF só valem para o campo que mudou (ProfissionalService.editar).
public record ProfissionalEdicaoRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,
        @NotBlank(message = "cpf é obrigatório")
        @Size(max = 14, message = "cpf deve ter no máximo 14 caracteres")
        String cpf,
        @Size(max = 40, message = "carteiraProfissional deve ter no máximo 40 caracteres")
        String carteiraProfissional,
        Long servicoId,
        @NotBlank(message = "username é obrigatório")
        String username,
        @Email(message = "email inválido")
        @Size(max = 150, message = "email deve ter no máximo 150 caracteres")
        String email,
        @NotBlank(message = "role é obrigatória")
        String role,
        @NotNull(message = "podeGerenciarProfissionais é obrigatório")
        Boolean podeGerenciarProfissionais,
        @NotNull(message = "ativo é obrigatório")
        Boolean ativo
) {
}
