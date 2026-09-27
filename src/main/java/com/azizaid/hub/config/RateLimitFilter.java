package com.azizaid.hub.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String MENSAGEM = "Muitas requisições. Tente novamente em alguns minutos.";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ErroResponseWriter erroResponseWriter;
    private final Cache<String, Bucket> loginCache;
    private final Cache<String, Bucket> fichaPublicaStatusCache;
    private final Cache<String, Bucket> fichaPublicaEnvioCache;
    private final Cache<String, Bucket> trocaSenhaCache;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimitProperties properties, ErroResponseWriter erroResponseWriter) {
        this.properties = properties;
        this.erroResponseWriter = erroResponseWriter;
        this.loginCache = construirCache(properties.login().janelaMinutos());
        this.fichaPublicaStatusCache = construirCache(properties.fichaPublicaStatus().janelaMinutos());
        this.fichaPublicaEnvioCache = construirCache(properties.fichaPublicaEnvio().janelaMinutos());
        this.trocaSenhaCache = construirCache(properties.trocaSenha().janelaMinutos());
    }

    private static Cache<String, Bucket> construirCache(int janelaMinutos) {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(janelaMinutos))
                .maximumSize(10_000)
                .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Regra regra = casarRegra(request);
        if (regra == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = regra.cache().get(ip, key -> criarBucket(regra.capacidade(), regra.janelaMinutos()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long segundosParaEsperar = Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L);
            log.warn("Rate limit excedido: regra={} ip={}", regra.nome(), ip);
            response.setHeader("Retry-After", String.valueOf(segundosParaEsperar));
            erroResponseWriter.escrever(response, HttpStatus.TOO_MANY_REQUESTS, MENSAGEM);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static Bucket criarBucket(int capacidade, int janelaMinutos) {
        Bandwidth bandwidth = Bandwidth.classic(capacidade, Refill.greedy(capacidade, Duration.ofMinutes(janelaMinutos)));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private Regra casarRegra(HttpServletRequest request) {
        String metodo = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.POST.matches(metodo) && "/api/auth/login".equals(path)) {
            return new Regra("login", loginCache, properties.login().capacidade(), properties.login().janelaMinutos());
        }
        if (HttpMethod.GET.matches(metodo) && PATH_MATCHER.match("/api/ficha-publica/*/status", path)) {
            return new Regra("ficha-publica-status", fichaPublicaStatusCache,
                    properties.fichaPublicaStatus().capacidade(), properties.fichaPublicaStatus().janelaMinutos());
        }
        // Senha atual errada não conta como falha de login: sem este limite, quem tivesse uma sessão
        // roubada poderia testar senhas à vontade até acertar e trocar a senha da conta.
        if (HttpMethod.PUT.matches(metodo) && "/api/auth/senha".equals(path)) {
            return new Regra("troca-senha", trocaSenhaCache,
                    properties.trocaSenha().capacidade(), properties.trocaSenha().janelaMinutos());
        }
        if (HttpMethod.POST.matches(metodo) && PATH_MATCHER.match("/api/ficha-publica/*", path)) {
            return new Regra("ficha-publica-envio", fichaPublicaEnvioCache,
                    properties.fichaPublicaEnvio().capacidade(), properties.fichaPublicaEnvio().janelaMinutos());
        }
        return null;
    }

    private record Regra(String nome, Cache<String, Bucket> cache, int capacidade, int janelaMinutos) {
    }
}
