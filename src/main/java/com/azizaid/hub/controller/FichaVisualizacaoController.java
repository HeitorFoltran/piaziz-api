package com.azizaid.hub.controller;

import com.azizaid.hub.dto.response.VisualizacaoFichaResponseDTO;
import com.azizaid.hub.service.FichaVisualizacaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas/{fichaId}/visualizacoes")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class FichaVisualizacaoController {

    private final FichaVisualizacaoService fichaVisualizacaoService;

    public FichaVisualizacaoController(FichaVisualizacaoService fichaVisualizacaoService) {
        this.fichaVisualizacaoService = fichaVisualizacaoService;
    }

    @GetMapping
    public List<VisualizacaoFichaResponseDTO> listar(@PathVariable Long fichaId) {
        return fichaVisualizacaoService.listarPorFicha(fichaId);
    }
}
