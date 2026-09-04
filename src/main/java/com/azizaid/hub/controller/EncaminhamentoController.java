package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;
import com.azizaid.hub.dto.response.EncaminhamentoResponseDTO;
import com.azizaid.hub.service.EncaminhamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas/{fichaId}/encaminhamentos")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class EncaminhamentoController {

    private final EncaminhamentoService encaminhamentoService;

    public EncaminhamentoController(EncaminhamentoService encaminhamentoService) {
        this.encaminhamentoService = encaminhamentoService;
    }

    @GetMapping
    public List<EncaminhamentoResponseDTO> listar(@PathVariable Long fichaId) {
        return encaminhamentoService.listarPorFicha(fichaId);
    }

    @PostMapping
    public ResponseEntity<EncaminhamentoResponseDTO> criar(
            @PathVariable Long fichaId,
            @Valid @RequestBody EncaminhamentoRequestDTO dto) {
        EncaminhamentoResponseDTO criado = encaminhamentoService.criar(fichaId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }
}
