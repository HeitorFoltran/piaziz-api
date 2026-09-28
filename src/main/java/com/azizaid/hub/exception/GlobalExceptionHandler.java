package com.azizaid.hub.exception;

import com.azizaid.hub.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return montar(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        return montar(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, Object>> handleMuitasTentativas(MuitasTentativasException ex) {
        return montar(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        String erros = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatarErro)
                .collect(Collectors.joining("; "));
        return montar(HttpStatus.BAD_REQUEST, erros.isEmpty() ? "Requisição inválida" : erros);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Cobre a AuthorizationDeniedException do @PreAuthorize (subclasse) e a AccessDeniedException
    // lançada direto por services. Sem este handler, o catch-all abaixo as transformaria em 500.
    // Mesma mensagem do accessDeniedHandler do SecurityConfig.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcessoNegado(AccessDeniedException ex) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    // Não repassa ex.getMessage(): a mensagem do Jackson expõe nomes de classe e de campo interno.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName());
    }

    // Bean Validation direto em @RequestParam (ex.: @Min/@Max no page/size da listagem).
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidacaoParametro(HandlerMethodValidationException ex) {
        String erros = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return montar(HttpStatus.BAD_REQUEST, erros.isEmpty() ? "Requisição inválida" : erros);
    }

    // Exceções padrão do Spring MVC (ErrorResponse) já carregam o status: 404, 405, 415 etc.
    // Qualquer outra coisa é 500 genérico, com stack trace no log. A URI não é logada: o caminho
    // de /api/ficha-publica/{token} carrega o token do link público.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode codigo = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(codigo.value());
            if (status != null && status.is4xxClientError()) {
                return montar(status, mensagemCliente(status));
            }
        }
        log.error("Erro inesperado em {}", request.getMethod(), ex);
        // O requestId no corpo leva de quem reportou o erro ao stack trace no log. Só no 500.
        ResponseEntity<Map<String, Object>> resposta = montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        resposta.getBody().put("requestId", MDC.get(RequestIdFilter.MDC_CHAVE));
        return resposta;
    }

    private static String mensagemCliente(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            default -> "Requisição inválida";
        };
    }

    private static String formatarErro(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private static ResponseEntity<Map<String, Object>> montar(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("error", status.getReasonPhrase());
        corpo.put("message", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
