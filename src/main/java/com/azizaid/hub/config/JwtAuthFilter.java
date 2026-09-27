package com.azizaid.hub.config;

import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String MENSAGEM_TROCA_OBRIGATORIA = "Troca de senha obrigatória";

    private final JwtService jwtService;
    private final ProfissionalRepository profissionalRepository;
    private final ErroResponseWriter erroResponseWriter;

    public JwtAuthFilter(JwtService jwtService, ProfissionalRepository profissionalRepository,
                         ErroResponseWriter erroResponseWriter) {
        this.jwtService = jwtService;
        this.profissionalRepository = profissionalRepository;
        this.erroResponseWriter = erroResponseWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length());
            try {
                Claims claims = jwtService.validarEExtrairClaims(token);
                Long profissionalId = jwtService.extrairProfissionalId(claims);

                Optional<Profissional> encontrado = profissionalRepository.findById(profissionalId);
                if (encontrado.isEmpty() || !encontrado.get().isAtivo()
                        || sessaoRevogada(claims.getIssuedAt(), encontrado.get().getSessoesRevogadasEm())) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                PapelProfissional role = encontrado.get().getRole();

                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var auth = new UsernamePasswordAuthenticationToken(profissionalId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Barreira no servidor: o front redireciona antes, mas não dá para confiar só nele.
                if (encontrado.get().isDeveTrocarSenha() && !liberadaDuranteTrocaDeSenha(request)) {
                    erroResponseWriter.escrever(response, HttpStatus.FORBIDDEN, MENSAGEM_TROCA_OBRIGATORIA);
                    return;
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean liberadaDuranteTrocaDeSenha(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        String metodo = request.getMethod();
        return ("PUT".equals(metodo) && "/api/auth/senha".equals(caminho))
                || ("GET".equals(metodo) && "/api/auth/me".equals(caminho));
    }

    private static boolean sessaoRevogada(Date emitidoEm, Instant revogadasEm) {
        return revogadasEm != null
                && emitidoEm.toInstant().isBefore(revogadasEm.truncatedTo(ChronoUnit.SECONDS));
    }
}
