package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.HistoricoAtendimentoResponseDTO;
import com.azizaid.hub.service.HistoricoAtendimentoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fichas/{fichaId}/historico-atendimento")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class HistoricoAtendimentoController {

    private final HistoricoAtendimentoService historicoAtendimentoService;

    public HistoricoAtendimentoController(HistoricoAtendimentoService historicoAtendimentoService) {
        this.historicoAtendimentoService = historicoAtendimentoService;
    }

    @GetMapping
    public HistoricoAtendimentoResponseDTO buscar(@PathVariable Long fichaId) {
        return historicoAtendimentoService.buscarPorFicha(fichaId);
    }

    @PutMapping
    public HistoricoAtendimentoResponseDTO salvar(
            @PathVariable Long fichaId,
            @Valid @RequestBody HistoricoAtendimentoRequestDTO dto) {
        return historicoAtendimentoService.salvar(fichaId, dto);
    }
}
