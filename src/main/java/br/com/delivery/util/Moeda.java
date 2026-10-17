package br.com.delivery.util;

import java.util.Locale;

/**
 * Funções utilitárias para valores em dinheiro.
 * Classe final com construtor privado: não faz sentido criar objetos dela.
 */
public final class Moeda {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private Moeda() {
    }

    /** Arredonda para 2 casas decimais (centavos). */
    public static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** Formata no padrão brasileiro: 1234.5 -> "R$ 1234,50". */
    public static String formatar(double valor) {
        return String.format(BRASIL, "R$ %.2f", valor);
    }
}
