package br.com.delivery;

import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.FormaPagamento;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.Pagamento;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.servico.ItemRanking;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.servico.Relatorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Cenário (frete de 11,50 em todos os pedidos, prazo 35 min):
 *   P1 - Restaurante A, 1 x R$ 20 -> total 31,50, entregue em 30 min (no prazo)
 *   P2 - Restaurante B, 2 x R$ 50 -> total 111,50, entregue em 50 min (atrasado)
 *   P3 - Restaurante A, 2 x R$ 20 -> cancelado em preparo, multa de 30% de 40 = 12,00
 */
class RelatoriosTest {

    private static final double DELTA = 0.001;

    private Relatorios relatorios;
    private Pedido p1;
    private Pedido p2;

    @BeforeEach
    void preparar() {
        Plataforma plataforma = new Plataforma();
        plataforma.cadastrarRestaurante("A", "Lanches", new Localizacao("a", 0, 0), 20, 0, 0);
        plataforma.cadastrarRestaurante("B", "Pizza", new Localizacao("b", 0, 0), 20, 0, 0);
        plataforma.cadastrarProduto(1, "Sanduíche", "Lanche", 20.0, 100); // produto 1
        plataforma.cadastrarProduto(2, "Pizza", "Pizza", 50.0, 100);      // produto 2
        Cliente cliente = plataforma.cadastrarCliente("Ana", "9999", new Localizacao("casa", 3, 4));
        plataforma.cadastrarEntregador("Marcos", "Moto");

        p1 = plataforma.criarPedido(cliente.getId(), 1);
        p1.adicionarItem(plataforma.buscarProduto(1), 1);
        entregar(plataforma, p1, 30);

        p2 = plataforma.criarPedido(cliente.getId(), 2);
        p2.adicionarItem(plataforma.buscarProduto(2), 2);
        entregar(plataforma, p2, 50);

        Pedido p3 = plataforma.criarPedido(cliente.getId(), 1);
        p3.adicionarItem(plataforma.buscarProduto(1), 2);
        p3.confirmar(new Pagamento(FormaPagamento.PIX));
        p3.iniciarPreparo();
        p3.cancelar();

        relatorios = new Relatorios(plataforma);
    }

    private void entregar(Plataforma plataforma, Pedido pedido, int minutos) {
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        pedido.iniciarPreparo();
        plataforma.despacharPedido(pedido.getId());
        pedido.registrarEntrega(minutos);
    }

    @Test
    @DisplayName("MÉDIA - ticket médio dos pedidos entregues")
    void ticketMedio() {
        assertEquals(71.50, relatorios.calcularTicketMedio(), DELTA); // (31,50 + 111,50) / 2
    }

    @Test
    @DisplayName("TOTAL - faturamento inclui pedidos entregues e multas de cancelamento")
    void faturamentoTotal() {
        assertEquals(155.00, relatorios.calcularFaturamentoTotal(), DELTA); // 31,50 + 111,50 + 12,00
    }

    @Test
    @DisplayName("MAIOR/MENOR - pedido de maior e de menor valor")
    void maiorEMenor() {
        assertSame(p2, relatorios.buscarPedidoDeMaiorValor());
        assertSame(p1, relatorios.buscarPedidoDeMenorValor());
    }

    @Test
    @DisplayName("RANKING - restaurantes por faturamento e produtos mais vendidos")
    void rankings() {
        ArrayList<ItemRanking> restaurantes = relatorios.rankingRestaurantesPorFaturamento();
        assertEquals("B", restaurantes.get(0).getDescricao());
        assertEquals(111.50, restaurantes.get(0).getValor(), DELTA);
        assertEquals(43.50, restaurantes.get(1).getValor(), DELTA);

        ArrayList<ItemRanking> produtos = relatorios.rankingProdutosMaisVendidos(10);
        assertEquals("Pizza (B)", produtos.get(0).getDescricao());
        assertEquals(2.0, produtos.get(0).getValor(), DELTA);
        assertEquals(1.0, produtos.get(1).getValor(), DELTA); // pedido cancelado não conta
    }

    @Test
    @DisplayName("INDICADORES PRÓPRIOS - pontualidade e cancelamento")
    void indicadoresProprios() {
        assertEquals(50.00, relatorios.calcularTaxaPontualidade(), DELTA);
        assertEquals(33.33, relatorios.calcularTaxaCancelamento(), DELTA);
    }
}
