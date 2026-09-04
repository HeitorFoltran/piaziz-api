package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.repository.ServicoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfissionalService(ProfissionalRepository profissionalRepository, ServicoRepository servicoRepository,
                               PasswordEncoder passwordEncoder) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponseDTO> listar(String termo, Long servicoId) {
        String t = (termo == null || termo.isBlank()) ? null : termo.trim();
        return profissionalRepository.buscar(t, servicoId).stream()
                .map(ProfissionalResponseDTO::from)
                .toList();
    }

    @Transactional
    public ProfissionalResponseDTO criar(ProfissionalRequestDTO dto) {
        if (profissionalRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Já existe um profissional com este email");
        }

        Profissional profissional = new Profissional();
        profissional.setNome(dto.nome());
        profissional.setCpf(dto.cpf());
        profissional.setCarteiraProfissional(dto.carteiraProfissional());
        profissional.setEmail(dto.email());
        profissional.setSenhaHash(passwordEncoder.encode(dto.senha()));
        profissional.setRole(resolverRole(dto.role()));

        if (dto.servicoId() != null) {
            Servico servico = servicoRepository.findById(dto.servicoId())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", dto.servicoId()));
            profissional.setServico(servico);
        }

        return ProfissionalResponseDTO.from(profissionalRepository.save(profissional));
    }

    private PapelProfissional resolverRole(String role) {
        if (role == null || role.isBlank()) {
            return PapelProfissional.PADRAO;
        }
        PapelProfissional resolvido;
        try {
            resolvido = PapelProfissional.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("role inválida: " + role);
        }
        if (resolvido == PapelProfissional.DEV) {
            throw new IllegalArgumentException("role DEV não pode ser atribuída por esta rota");
        }
        return resolvido;
    }
}
