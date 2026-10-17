package br.com.delivery.io;

import java.util.ArrayList;

/** Resumo de uma importação: quantas linhas entraram e quais deram erro. */
public class ResultadoImportacao {

    private int importados;
    private ArrayList<String> erros = new ArrayList<>();

    public void registrarSucesso() {
        importados++;
    }

    public void registrarErro(int linha, String motivo) {
        erros.add("Linha " + linha + ": " + motivo);
    }

    public int getImportados() {
        return importados;
    }

    public ArrayList<String> getErros() {
        return new ArrayList<>(erros);
    }
}
