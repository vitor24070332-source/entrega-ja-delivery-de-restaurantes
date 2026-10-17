package br.com.delivery.app;

import br.com.delivery.io.ExportadorCsv;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.Produto;
import br.com.delivery.servico.ItemRanking;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.servico.Relatorios;
import br.com.delivery.util.Moeda;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class MenuRelatorios {

    private static final String PASTA_EXPORTACAO = "exportacoes";

    private Plataforma plataforma;
    private Relatorios relatorios;
    private Console console;

    public MenuRelatorios(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.relatorios = new Relatorios(plataforma);
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("RELATÓRIOS E INDICADORES");
            console.mensagem("1 - Painel de indicadores");
            console.mensagem("2 - Ranking de restaurantes por faturamento");
            console.mensagem("3 - Ranking de produtos mais vendidos");
            console.mensagem("4 - Ranking de entregadores");
            console.mensagem("5 - Produtos com estoque baixo");
            console.mensagem("6 - Exportar relatórios para CSV");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            switch (opcao) {
                case 0 -> {
                    return;
                }
                case 1 -> painel();
                case 2 -> imprimirRanking(relatorios.rankingRestaurantesPorFaturamento(), true);
                case 3 -> imprimirRanking(relatorios.rankingProdutosMaisVendidos(10), false);
                case 4 -> imprimirRanking(relatorios.rankingEntregadores(), false);
                case 5 -> estoqueBaixo();
                case 6 -> exportar();
                default -> console.erro("Opção inválida.");
            }
        }
    }

    private void painel() {
        console.mensagem("Faturamento total ........ " + Moeda.formatar(relatorios.calcularFaturamentoTotal()));
        console.mensagem("Ticket médio ............. " + Moeda.formatar(relatorios.calcularTicketMedio()));
        console.mensagem("Comissões de entregadores  " + Moeda.formatar(relatorios.calcularTotalComissoes()));
        console.mensagem("Itens vendidos ........... " + relatorios.calcularTotalItensVendidos());
        console.mensagem("Taxa de pontualidade ..... " + String.format("%.1f%%", relatorios.calcularTaxaPontualidade()));
        console.mensagem("Taxa de cancelamento ..... " + String.format("%.1f%%", relatorios.calcularTaxaCancelamento()));
        Pedido maior = relatorios.buscarPedidoDeMaiorValor();
        Pedido menor = relatorios.buscarPedidoDeMenorValor();
        console.mensagem("Maior pedido ............. " + (maior == null ? "-" : maior));
        console.mensagem("Menor pedido ............. " + (menor == null ? "-" : menor));
    }

    private void imprimirRanking(ArrayList<ItemRanking> ranking, boolean dinheiro) {
        if (ranking.isEmpty()) {
            console.mensagem("  Sem dados ainda.");
            return;
        }
        int posicao = 1;
        for (ItemRanking item : ranking) {
            String valor = dinheiro ? Moeda.formatar(item.getValor()) : String.valueOf((int) item.getValor());
            console.mensagem(String.format("  %2dº  %-45s %s", posicao, item.getDescricao(), valor));
            posicao++;
        }
    }

    private void estoqueBaixo() {
        int limite = console.lerInteiro("Mostrar produtos com estoque até");
        ArrayList<Produto> produtos = relatorios.listarProdutosComEstoqueBaixo(limite);
        console.listar(produtos, "Nenhum produto com estoque baixo.");
    }

    /** TECNOLOGIA EXTERNA - exportação CSV. */
    private void exportar() {
        ExportadorCsv exportador = new ExportadorCsv();
        try {
            exportador.exportarPedidos(plataforma.listarPedidos(), Path.of(PASTA_EXPORTACAO, "pedidos.csv"));
            exportador.exportarRanking(relatorios.rankingRestaurantesPorFaturamento(), "faturamento",
                    Path.of(PASTA_EXPORTACAO, "ranking_restaurantes.csv"));
            exportador.exportarRanking(relatorios.rankingProdutosMaisVendidos(50), "unidades_vendidas",
                    Path.of(PASTA_EXPORTACAO, "ranking_produtos.csv"));
            exportador.exportarIndicadores(relatorios, Path.of(PASTA_EXPORTACAO, "indicadores.csv"));
            console.sucesso("Arquivos gerados na pasta " + Path.of(PASTA_EXPORTACAO).toAbsolutePath());
        } catch (IOException e) {
            console.erro("Falha ao exportar: " + e.getMessage());
        }
    }
}
