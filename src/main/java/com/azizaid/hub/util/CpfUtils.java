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

    public static String digitos(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    // 000.000.000-00. Só chamar com um CPF válido (11 dígitos).
    public static String formatar(String cpf) {
        String d = digitos(cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static boolean isValido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroDv = calcularDigitoVerificador(digitos, 9);
        int segundoDv = calcularDigitoVerificador(digitos, 10);

        return primeiroDv == Character.getNumericValue(digitos.charAt(9))
                && segundoDv == Character.getNumericValue(digitos.charAt(10));
    }

    private static int calcularDigitoVerificador(String digitos, int tamanho) {
        int peso = tamanho + 1;
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
