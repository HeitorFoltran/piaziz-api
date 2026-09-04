package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.response.AcolhimentoEquipeResponseDTO;
import com.azizaid.hub.service.AcolhimentoEquipeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fichas/{fichaId}/acolhimento-equipe")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class AcolhimentoEquipeController {

    private final AcolhimentoEquipeService acolhimentoEquipeService;

    public AcolhimentoEquipeController(AcolhimentoEquipeService acolhimentoEquipeService) {
        this.acolhimentoEquipeService = acolhimentoEquipeService;
    }

    @GetMapping
    public AcolhimentoEquipeResponseDTO buscar(@PathVariable Long fichaId) {
        return acolhimentoEquipeService.buscarPorFicha(fichaId);
    }

    @PutMapping
    public AcolhimentoEquipeResponseDTO salvar(
            @PathVariable Long fichaId,
            @Valid @RequestBody AcolhimentoEquipeRequestDTO dto) {
        return acolhimentoEquipeService.salvar(fichaId, dto);
    }
}
