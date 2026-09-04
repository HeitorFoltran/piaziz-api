package com.azizaid.hub.dto.request;

import java.util.List;

public record AtribuirTiposAcompanhamentoRequestDTO(
        List<Long> tipoIds
) {
}
