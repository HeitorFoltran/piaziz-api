package com.azizaid.hub.dto.response;

import java.time.LocalDateTime;

public record AcompanhamentoResumoDTO(
        Long id,
        String numeroCaso,
        String codigoFicha,
        String nome,
        String cpf,
        String encaminhamento,
        String tipoEncaminhamento,
        String status,
        LocalDateTime dataAtualizacao,
        LocalDateTime dataCriacao
) {
}
