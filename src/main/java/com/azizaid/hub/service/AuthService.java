package com.azizaid.hub.service;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.exception.CredenciaisInvalidasException;
import com.azizaid.hub.model.AuthAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final String MENSAGEM_GENERICA = "Credenciais inválidas";

    private final ProfissionalRepository profissionalRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ProfissionalRepository profissionalRepository,
                       AuthAuditLogRepository authAuditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.profissionalRepository = profissionalRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String ipAddress) {
        Optional<Profissional> encontrado = profissionalRepository.findByEmail(dto.email());

        if (encontrado.isEmpty()) {
            registrarTentativa(dto.email(), false, ipAddress, "email desconhecido");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        Profissional profissional = encontrado.get();
        if (!passwordEncoder.matches(dto.senha(), profissional.getSenhaHash())) {
            registrarTentativa(dto.email(), false, ipAddress, "senha incorreta");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        registrarTentativa(dto.email(), true, ipAddress, null);

        String token = jwtService.gerarToken(profissional.getId(), profissional.getEmail(), profissional.getRole());
        return new LoginResponseDTO(
                token,
                profissional.getId(),
                profissional.getNome(),
                profissional.getEmail(),
                profissional.getRole().name());
    }

    private void registrarTentativa(String email, boolean sucesso, String ipAddress, String motivoFalha) {
        authAuditLogRepository.save(AuthAuditLog.builder()
                .emailTentado(email)
                .sucesso(sucesso)
                .ipAddress(ipAddress)
                .motivoFalha(motivoFalha)
                .build());
    }
}
