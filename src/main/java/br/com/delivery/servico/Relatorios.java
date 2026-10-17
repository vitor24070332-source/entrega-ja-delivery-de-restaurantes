package br.com.delivery.servico;

import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.ItemPedido;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.Produto;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.modelo.StatusPedido;
import br.com.delivery.util.Moeda;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Relatórios e indicadores do sistema.
 *
 * Exigidos pelo enunciado:
 *   MÉDIA ......... calcularTicketMedio()
 *   TOTAL ......... calcularFaturamentoTotal()
 *   MAIOR/MENOR ... buscarPedidoDeMaiorValor() / buscarPedidoDeMenorValor()
 *   RANKING ....... rankingRestaurantesPorFaturamento(), rankingProdutosMaisVendidos(), rankingEntregadores()
 *   PRÓPRIO ....... calcularTaxaPontualidade() e calcularTaxaCancelamento()
 */
public class Relatorios {

    private Plataforma plataforma;

    public Relatorios(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    private ArrayList<Pedido> pedidosEntregues() {
        return plataforma.listarPedidosPorStatus(StatusPedido.ENTREGUE);
    }

    /** MÉDIA - valor médio gasto por pedido entregue. */
    public double calcularTicketMedio() {
        ArrayList<Pedido> entregues = pedidosEntregues();
        if (entregues.isEmpty()) {
            return 0.0;
        }
        double soma = 0;
        for (Pedido p : entregues) {
            soma += p.getTotal();
        }
        return Moeda.arredondar(soma / entregues.size());
    }

    /** TOTAL - soma do faturamento de todos os restaurantes. */
    public double calcularFaturamentoTotal() {
        double total = 0;
        for (Restaurante r : plataforma.listarRestaurantes()) {
            total += r.calcularFaturamento();
        }
        return Moeda.arredondar(total);
    }

    public double calcularTotalComissoes() {
        double total = 0;
        for (Entregador e : plataforma.listarEntregadores()) {
            total += e.calcularTotalComissoes();
        }
        return Moeda.arredondar(total);
    }

    /** MAIOR - pedido entregue de maior valor (null se não houver). */
    public Pedido buscarPedidoDeMaiorValor() {
        Pedido maior = null;
        for (Pedido p : pedidosEntregues()) {
            if (maior == null || p.getTotal() > maior.getTotal()) {
                maior = p;
            }
        }
        return maior;
    }

    /** MENOR - pedido entregue de menor valor (null se não houver). */
    public Pedido buscarPedidoDeMenorValor() {
        Pedido menor = null;
        for (Pedido p : pedidosEntregues()) {
            if (menor == null || p.getTotal() < menor.getTotal()) {
                menor = p;
            }
        }
        return menor;
    }

    /** RANKING - restaurantes ordenados pelo faturamento. */
    public ArrayList<ItemRanking> rankingRestaurantesPorFaturamento() {
        ArrayList<ItemRanking> ranking = new ArrayList<>();
        for (Restaurante r : plataforma.listarRestaurantes()) {
            ranking.add(new ItemRanking(r.getNome(), r.calcularFaturamento()));
        }
        ordenarDecrescente(ranking);
        return ranking;
    }

    /** RANKING - produtos mais vendidos (unidades em pedidos entregues). */
    public ArrayList<ItemRanking> rankingProdutosMaisVendidos(int limite) {
        ArrayList<ItemRanking> ranking = new ArrayList<>();
        for (Restaurante r : plataforma.listarRestaurantes()) {
            for (Produto produto : r.getCardapio()) {
                int unidades = 0;
                for (Pedido pedido : r.getPedidos()) {
                    if (pedido.getStatus() == StatusPedido.ENTREGUE) {
                        unidades += pedido.quantidadeNoCarrinho(produto);
                    }
                }
                if (unidades > 0) {
                    ranking.add(new ItemRanking(produto.getNome() + " (" + r.getNome() + ")", unidades));
                }
            }
        }
        ordenarDecrescente(ranking);
        if (ranking.size() > limite) {
            return new ArrayList<>(ranking.subList(0, limite));
        }
        return ranking;
    }

    /** RANKING - entregadores por número de entregas concluídas. */
    public ArrayList<ItemRanking> rankingEntregadores() {
        ArrayList<ItemRanking> ranking = new ArrayList<>();
        for (Entregador e : plataforma.listarEntregadores()) {
            ranking.add(new ItemRanking(e.getNome(), e.quantidadeEntregasConcluidas()));
        }
        ordenarDecrescente(ranking);
        return ranking;
    }

    /**
     * INDICADOR PRÓPRIO - Taxa de pontualidade (%):
     * dos pedidos entregues, quantos chegaram dentro do prazo estimado.
     */
    public double calcularTaxaPontualidade() {
        ArrayList<Pedido> entregues = pedidosEntregues();
        if (entregues.isEmpty()) {
            return 0.0;
        }
        int noPrazo = 0;
        for (Pedido p : entregues) {
            if (p.foiEntregueNoPrazo()) {
                noPrazo++;
            }
        }
        return Moeda.arredondar(100.0 * noPrazo / entregues.size());
    }

    /**
     * INDICADOR PRÓPRIO - Taxa de cancelamento (%):
     * dos pedidos que saíram do carrinho (não estão ABERTO), quantos foram cancelados.
     */
    public double calcularTaxaCancelamento() {
        int finalizados = 0;
        int cancelados = 0;
        for (Pedido p : plataforma.listarPedidos()) {
            if (p.getStatus() != StatusPedido.ABERTO) {
                finalizados++;
                if (p.getStatus() == StatusPedido.CANCELADO) {
                    cancelados++;
                }
            }
        }
        if (finalizados == 0) {
            return 0.0;
        }
        return Moeda.arredondar(100.0 * cancelados / finalizados);
    }

    /** Produtos ativos com estoque menor ou igual ao limite (alerta de reposição). */
    public ArrayList<Produto> listarProdutosComEstoqueBaixo(int limite) {
        ArrayList<Produto> baixos = new ArrayList<>();
        for (Restaurante r : plataforma.listarRestaurantes()) {
            for (Produto p : r.getCardapio()) {
                if (p.isAtivo() && p.getEstoque() <= limite) {
                    baixos.add(p);
                }
            }
        }
        baixos.sort(Comparator.comparingInt(Produto::getEstoque));
        return baixos;
    }

    /** Soma das unidades de produtos vendidos em pedidos entregues. */
    public int calcularTotalItensVendidos() {
        int total = 0;
        for (Pedido p : pedidosEntregues()) {
            for (ItemPedido item : p.getItens()) {
                total += item.getQuantidade();
            }
        }
        return total;
    }

    private void ordenarDecrescente(ArrayList<ItemRanking> ranking) {
        ranking.sort(Comparator.comparingDouble(ItemRanking::getValor).reversed());
    }
}
