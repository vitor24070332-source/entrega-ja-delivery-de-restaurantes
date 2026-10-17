package br.com.delivery;

import br.com.delivery.io.ImportadorCsv;
import br.com.delivery.io.ResultadoImportacao;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.servico.Plataforma;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportadorCsvTest {

    @Test
    @DisplayName("CSV - importa linhas válidas e registra o motivo das inválidas")
    void importaCardapio() throws Exception {
        Plataforma plataforma = new Plataforma();
        plataforma.cadastrarRestaurante("Açaí", "Sobremesa", new Localizacao("x", 0, 0), 10, 0, 0);

        Path arquivo = Files.createTempFile("cardapio", ".csv");
        Files.writeString(arquivo,
                "﻿restaurante_id;nome;categoria;preco;estoque\n"
                        + "1;Açaí 300ml;Sobremesa;12,50;20\n"   // ok (vírgula decimal)
                        + "1;Açaí 700ml;Sobremesa;abc;20\n"     // preço inválido
                        + "9;Suco;Bebida;8.00;10\n"            // restaurante não existe
                        + "1;Açaí 300ml;Sobremesa;13.00;5\n",  // nome repetido no cardápio
                StandardCharsets.UTF_8);

        ResultadoImportacao resultado = new ImportadorCsv().importarCardapio(plataforma, arquivo);

        assertEquals(1, resultado.getImportados());
        assertEquals(3, resultado.getErros().size());
        assertEquals(12.50, plataforma.buscarProduto(1).getPreco(), 0.001);
        Files.deleteIfExists(arquivo);
    }
}
