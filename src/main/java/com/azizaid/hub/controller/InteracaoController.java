package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.InteracaoRequestDTO;
import com.azizaid.hub.dto.response.InteracaoResponseDTO;
import com.azizaid.hub.service.InteracaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas/{fichaId}/interacoes")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class InteracaoController {

    private final InteracaoService interacaoService;

    public InteracaoController(InteracaoService interacaoService) {
        this.interacaoService = interacaoService;
    }

    @GetMapping
    public List<InteracaoResponseDTO> listar(@PathVariable Long fichaId) {
        return interacaoService.listarPorFicha(fichaId);
    }

    @PostMapping
    public ResponseEntity<InteracaoResponseDTO> criar(
            @PathVariable Long fichaId,
            @Valid @RequestBody InteracaoRequestDTO dto) {
        InteracaoResponseDTO criada = interacaoService.criar(fichaId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }
}
