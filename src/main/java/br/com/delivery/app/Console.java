package br.com.delivery.app;

import java.util.List;
import java.util.Scanner;

/**
 * Centraliza a leitura do teclado e a impressão de mensagens.
 * Assim os menus não repetem código de validação de entrada.
 */
public class Console {

    private final  Scanner scanner;

    public Console(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Lê um texto obrigatório (repete até o usuário digitar algo). */
    public String lerTexto(String rotulo) {
        while (true) {
            System.out.print(rotulo + ": ");
            String texto = scanner.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("  Campo obrigatório.");
        }
    }

    /** Lê um texto que pode ficar vazio (ENTER mantém o valor atual na edição). */
    public String lerTextoOpcional(String rotulo) {
        System.out.print(rotulo + ": ");
        return scanner.nextLine().trim();
    }

    public int lerInteiro(String rotulo) {
        while (true) {
            String texto = lerTexto(rotulo);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("  Digite um número inteiro.");
            }
        }
    }

    /** Aceita vírgula ou ponto como separador decimal. */
    public double lerDecimal(String rotulo) {
        while (true) {
            String texto = lerTexto(rotulo).replace(',', '.');
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("  Digite um número (ex.: 12,50).");
            }
        }
    }

    public boolean confirmar(String pergunta) {
        String resposta = lerTexto(pergunta + " (s/n)").toLowerCase();
        return resposta.startsWith("s");
    }

    public void titulo(String texto) {
        System.out.println();
        System.out.println("========== " + texto + " ==========");
    }

    public void sucesso(String mensagem) {
        System.out.println(">> " + mensagem);
    }

    public void erro(String mensagem) {
        System.out.println("!! " + mensagem);
    }

    public void mensagem(String texto) {
        System.out.println(texto);
    }

    /** Imprime uma lista (um item por linha) ou um aviso se estiver vazia. */
    public void listar(List<?> itens, String mensagemSeVazio) {
        if (itens.isEmpty()) {
            System.out.println("  " + mensagemSeVazio);
            return;
        }
        for (Object item : itens) {
            System.out.println("  " + item);
        }
    }
}
