package com.azizaid.hub.util;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static String mascarar(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() < 2) {
            return "***.***.***-**";
        }
        String doisUltimos = digitos.substring(digitos.length() - 2);
        return "***.***.***-" + doisUltimos;
    }
}
