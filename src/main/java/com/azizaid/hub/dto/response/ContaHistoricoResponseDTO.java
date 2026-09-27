package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.enums.AcaoConta;

import java.time.LocalDateTime;

// autorNome é null se o autor não existir mais. detalhe nunca contém senha nem hash.
public record ContaHistoricoResponseDTO(
        Long id,
        AcaoConta acao,
        String detalhe,
        Long autorId,
        String autorNome,
        LocalDateTime timestamp
) {
}
