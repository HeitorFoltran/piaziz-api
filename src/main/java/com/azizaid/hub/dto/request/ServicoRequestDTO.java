package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServicoRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
        String nome
) {
}
