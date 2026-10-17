package br.com.delivery.io;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.servico.Plataforma;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TECNOLOGIA EXTERNA 1 (parte de importação) - leitura de arquivo CSV.
 *
 * Importa produtos para os cardápios. Formato esperado (separador ";"):
 *
 *   restaurante_id;nome;categoria;preco;estoque
 *   1;Açaí 500ml;Sobremesa;18,90;30
 *
 * Linhas com erro não interrompem a importação: são registradas e puladas.
 */
public class ImportadorCsv {

    private static final String SEPARADOR = ";";
    private static final int COLUNAS_ESPERADAS = 5;

    public ResultadoImportacao importarCardapio(Plataforma plataforma, Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        ResultadoImportacao resultado = new ResultadoImportacao();

        // começa em 1 para pular o cabeçalho
        for (int i = 1; i < linhas.size(); i++) {
            String linha = removerBom(linhas.get(i)).trim();
            if (linha.isEmpty()) {
                continue;
            }
            int numeroLinha = i + 1;
            String[] colunas = linha.split(SEPARADOR, -1);
            if (colunas.length != COLUNAS_ESPERADAS) {
                resultado.registrarErro(numeroLinha, "esperadas " + COLUNAS_ESPERADAS
                        + " colunas, encontradas " + colunas.length + ".");
                continue;
            }
            try {
                int restauranteId = Integer.parseInt(colunas[0].trim());
                String nome = colunas[1].trim();
                String categoria = colunas[2].trim();
                double preco = lerNumero(colunas[3]);
                int estoque = Integer.parseInt(colunas[4].trim());
                plataforma.cadastrarProduto(restauranteId, nome, categoria, preco, estoque);
                resultado.registrarSucesso();
            } catch (NumberFormatException e) {
                resultado.registrarErro(numeroLinha, "número inválido.");
            } catch (RegraNegocioException e) {
                resultado.registrarErro(numeroLinha, e.getMessage());
            }
        }
        return resultado;
    }

    /** Aceita "18,90" (padrão brasileiro) ou "18.90". */
    private double lerNumero(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
    }

    /** Arquivos salvos pelo Excel podem começar com o caractere invisível BOM. */
    private String removerBom(String texto) {
        if (texto.startsWith("﻿")) {
            return texto.substring(1);
        }
        return texto;
    }
}
