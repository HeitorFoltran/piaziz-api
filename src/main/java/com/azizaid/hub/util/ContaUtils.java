package com.azizaid.hub.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ContaUtils {

    // O BCrypt ignora o que passa de 72 bytes, e o BCryptPasswordEncoder recente rejeita. O @Size
    // dos DTOs conta caracteres; acentos ocupam mais de um byte, então a checagem final é aqui.
    public static final int MAX_BYTES_SENHA = 72;

    private ContaUtils() {
    }

    // Username e email são sempre gravados e procurados assim.
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return normalizado.isEmpty() ? null : normalizado;
    }

    public static void validarTamanhoSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_SENHA) {
            throw new IllegalArgumentException("senha deve ter no máximo " + MAX_BYTES_SENHA + " bytes");
        }
    }
}
