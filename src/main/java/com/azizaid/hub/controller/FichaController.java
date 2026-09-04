package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.AtribuirTiposAcompanhamentoRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.service.FichaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping
    public List<FichaResponseDTO> listar(@RequestParam(value = "q", required = false) String q) {
        return fichaService.listar(q);
    }

    @GetMapping("/{id}")
    public FichaResponseDTO detalhar(@PathVariable Long id) {
        return fichaService.detalhar(id);
    }

    @PostMapping
    public ResponseEntity<FichaResponseDTO> criar(@Valid @RequestBody FichaRequestDTO dto) {
        FichaResponseDTO criada = fichaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PatchMapping("/{id}/status")
    public FichaResponseDTO atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusFicha status) {
        return fichaService.atualizarStatus(id, status);
    }

    @PutMapping("/{id}")
    public FichaResponseDTO atualizar(
            @PathVariable Long id,
            @Valid @RequestBody FichaRequestDTO dto) {
        return fichaService.atualizar(id, dto);
    }

    @PutMapping("/{id}/tipos-acompanhamento")
    public FichaResponseDTO atribuirTiposAcompanhamento(
            @PathVariable Long id,
            @RequestBody AtribuirTiposAcompanhamentoRequestDTO dto) {
        return fichaService.atribuirTiposAcompanhamento(id, dto.tipoIds() != null ? dto.tipoIds() : List.of());
    }
}
