package com.azizaid.hub.controller;

import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
import com.azizaid.hub.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profissionais")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    @GetMapping
    public List<ProfissionalResponseDTO> listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "servicoId", required = false) Long servicoId) {
        return profissionalService.listar(q, servicoId);
    }

    @PostMapping
    public ResponseEntity<ProfissionalResponseDTO> criar(@Valid @RequestBody ProfissionalRequestDTO dto) {
        ProfissionalResponseDTO criado = profissionalService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }
}
