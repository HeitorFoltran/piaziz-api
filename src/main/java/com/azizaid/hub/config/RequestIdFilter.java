package com.azizaid.hub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Primeiro filtro de todos (antes da cadeia do Spring Security): cada requisição ganha um UUID no
// MDC e no header X-Request-Id, e o 500 devolve o mesmo ID no corpo. O ID é sempre gerado aqui,
// nunca aceito do cliente: um valor controlado pelo cliente dentro do log abriria espaço para
// injetar texto nele. Este filtro não loga nada (nem a URI, que no link público carrega o token).
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_CHAVE = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        MDC.put(MDC_CHAVE, id);
        response.setHeader(HEADER, id);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CHAVE);
        }
    }
}
