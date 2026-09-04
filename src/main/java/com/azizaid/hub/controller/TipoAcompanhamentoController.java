package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.TipoAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.service.TipoAcompanhamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-acompanhamento")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class TipoAcompanhamentoController {

    private final TipoAcompanhamentoService tipoAcompanhamentoService;

    public TipoAcompanhamentoController(TipoAcompanhamentoService tipoAcompanhamentoService) {
        this.tipoAcompanhamentoService = tipoAcompanhamentoService;
    }

    @GetMapping
    public List<TipoAcompanhamentoResponseDTO> listar() {
        return tipoAcompanhamentoService.listar();
    }

    @PostMapping
    public ResponseEntity<TipoAcompanhamentoResponseDTO> criar(@Valid @RequestBody TipoAcompanhamentoRequestDTO dto) {
        TipoAcompanhamentoResponseDTO criado = tipoAcompanhamentoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public TipoAcompanhamentoResponseDTO atualizar(
            @PathVariable Long id,
            @Valid @RequestBody TipoAcompanhamentoRequestDTO dto) {
        return tipoAcompanhamentoService.atualizar(id, dto);
    }
}
