package br.com.delivery.io;

import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.StatusPedido;
import br.com.delivery.servico.ItemRanking;
import br.com.delivery.servico.Relatorios;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * TECNOLOGIA EXTERNA 1 (parte de exportação) - geração de arquivos CSV.
 *
 * Os arquivos usam ";" como separador e vírgula decimal, que é o que o Excel
 * em português espera. O BOM no início faz o Excel reconhecer os acentos (UTF-8).
 */
public class ExportadorCsv {

    private static final String SEPARADOR = ";";
    private static final String BOM = "﻿";
    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportarPedidos(List<Pedido> pedidos, Path arquivo) throws IOException {
        criarPasta(arquivo);
        try (BufferedWriter out = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            out.write(BOM);
            out.write(linha("id", "data", "cliente", "restaurante", "status", "itens", "subtotal",
                    "desconto", "origem_desconto", "frete", "total", "pagamento", "entregador",
                    "prazo_min", "tempo_entrega_min", "no_prazo", "nota"));
            for (Pedido p : pedidos) {
                out.write(linha(
                        String.valueOf(p.getId()),
                        p.getDataHoraCriacao().format(FORMATO_DATA),
                        p.getCliente().getNome(),
                        p.getRestaurante().getNome(),
                        p.getStatus().name(),
                        String.valueOf(p.getItens().size()),
                        numero(p.calcularSubtotal()),
                        numero(p.getDesconto()),
                        p.getOrigemDesconto(),
                        numero(p.getFrete()),
                        numero(p.getTotal()),
                        p.getPagamento() == null ? "" : p.getPagamento().getForma().name(),
                        p.getEntregador() == null ? "" : p.getEntregador().getNome(),
                        String.valueOf(p.getPrazoEstimadoMinutos()),
                        String.valueOf(p.getTempoEntregaMinutos()),
                        p.getStatus() == StatusPedido.ENTREGUE ? (p.foiEntregueNoPrazo() ? "SIM" : "NAO") : "",
                        p.getNota() == 0 ? "" : String.valueOf(p.getNota())));
            }
        }
    }

    public void exportarRanking(List<ItemRanking> ranking, String nomeColunaValor, Path arquivo) throws IOException {
        criarPasta(arquivo);
        try (BufferedWriter out = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            out.write(BOM);
            out.write(linha("posicao", "descricao", nomeColunaValor));
            int posicao = 1;
            for (ItemRanking item : ranking) {
                out.write(linha(String.valueOf(posicao), item.getDescricao(), numero(item.getValor())));
                posicao++;
            }
        }
    }

    public void exportarIndicadores(Relatorios relatorios, Path arquivo) throws IOException {
        criarPasta(arquivo);
        try (BufferedWriter out = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            out.write(BOM);
            out.write(linha("indicador", "valor"));
            out.write(linha("Faturamento total (R$)", numero(relatorios.calcularFaturamentoTotal())));
            out.write(linha("Ticket médio (R$)", numero(relatorios.calcularTicketMedio())));
            out.write(linha("Total de comissões (R$)", numero(relatorios.calcularTotalComissoes())));
            out.write(linha("Itens vendidos", String.valueOf(relatorios.calcularTotalItensVendidos())));
            out.write(linha("Taxa de pontualidade (%)", numero(relatorios.calcularTaxaPontualidade())));
            out.write(linha("Taxa de cancelamento (%)", numero(relatorios.calcularTaxaCancelamento())));
        }
    }

    private String linha(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(campos[i]));
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /** Se o texto tiver ";" ou aspas, envolve em aspas (padrão CSV). */
    private String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        if (campo.contains(SEPARADOR) || campo.contains("\"")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    private String numero(double valor) {
        return String.format(BRASIL, "%.2f", valor);
    }

    private void criarPasta(Path arquivo) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
    }
}
