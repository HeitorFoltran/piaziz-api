package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FichaPublicaRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
        String telefone,

        Integer idade,

        @Size(max = 2000, message = "situacaoRelatada deve ter no máximo 2000 caracteres")
        String situacaoRelatada
) {
}
