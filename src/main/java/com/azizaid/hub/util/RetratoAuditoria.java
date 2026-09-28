package com.azizaid.hub.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

// "Retrato" dos campos que um método grava, tirado antes e depois dos setters, para o histórico só
// registrar quando algo mudou de fato (as telas mandam as quatro partes do caso a cada salvamento).
// Coleção vira Set (ordem não conta), texto em branco vira null ("" e null não são uma alteração) e
// BigDecimal perde os zeros à direita (o banco devolve 1500.00 para o 1500 que a tela mandou).
public final class RetratoAuditoria {

    private RetratoAuditoria() {
    }

    public static List<Object> de(Object... valores) {
        // Stream.toList aceita null, ao contrário de List.of.
        return Arrays.stream(valores).map(RetratoAuditoria::normalizar).toList();
    }

    public static boolean algumPreenchido(List<Object> retrato) {
        return retrato.stream().anyMatch(v -> v != null && !(v instanceof Collection<?> c && c.isEmpty()));
    }

    private static Object normalizar(Object valor) {
        if (valor instanceof String s) {
            return s.isBlank() ? null : s;
        }
        if (valor instanceof BigDecimal b) {
            return b.stripTrailingZeros();
        }
        if (valor instanceof Collection<?> c) {
            return new HashSet<>(c);
        }
        return valor;
    }

    public static boolean mudou(List<Object> antes, List<Object> depois) {
        return !Objects.equals(antes, depois);
    }
}
