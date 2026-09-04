package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.response.AvaliacaoSocioeconomicaResponseDTO;
import com.azizaid.hub.service.AvaliacaoSocioeconomicaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fichas/{fichaId}/avaliacao-socioeconomica")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class AvaliacaoSocioeconomicaController {

    private final AvaliacaoSocioeconomicaService avaliacaoSocioeconomicaService;

    public AvaliacaoSocioeconomicaController(AvaliacaoSocioeconomicaService avaliacaoSocioeconomicaService) {
        this.avaliacaoSocioeconomicaService = avaliacaoSocioeconomicaService;
    }

    @GetMapping
    public AvaliacaoSocioeconomicaResponseDTO buscar(@PathVariable Long fichaId) {
        return avaliacaoSocioeconomicaService.buscarPorFicha(fichaId);
    }

    @PutMapping
    public AvaliacaoSocioeconomicaResponseDTO salvar(
            @PathVariable Long fichaId,
            @Valid @RequestBody AvaliacaoSocioeconomicaRequestDTO dto) {
        return avaliacaoSocioeconomicaService.salvar(fichaId, dto);
    }
}
