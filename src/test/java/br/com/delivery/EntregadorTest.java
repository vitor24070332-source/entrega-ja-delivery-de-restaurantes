package br.com.delivery;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.FormaPagamento;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.Pagamento;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.StatusEntregador;
import br.com.delivery.servico.Plataforma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RN08 (disponibilidade e distribuição de entregadores) e RN09 (prazo e comissão).
 * Mesmo cenário do PedidoTest: 5 km de distância, frete 11,50, prazo 35 min.
 */
class EntregadorTest {

    private static final double DELTA = 0.001;

    private Plataforma plataforma;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        plataforma = new Plataforma();
        plataforma.cadastrarRestaurante("Pizzaria", "Pizza", new Localizacao("Centro", 0, 0), 20, 0, 100);
        plataforma.cadastrarProduto(1, "Pizza", "Pizza", 50.0, 100);
        cliente = plataforma.cadastrarCliente("Ana", "9999", new Localizacao("Casa", 3, 4));
    }

    private Pedido pedidoEmPreparo(int quantidadePizzas) {
        Pedido pedido = plataforma.criarPedido(cliente.getId(), 1);
        pedido.adicionarItem(plataforma.buscarProduto(1), quantidadePizzas);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        pedido.iniciarPreparo();
        return pedido;
    }

    @Test
    @DisplayName("RN08 - entregador ocupado não pode assumir outro pedido; volta a ficar livre ao entregar")
    void umaEntregaPorVez() {
        Entregador e = plataforma.cadastrarEntregador("Marcos", "Moto");
        Pedido p1 = pedidoEmPreparo(1);
        Pedido p2 = pedidoEmPreparo(1);

        p1.despachar(e);
        assertEquals(StatusEntregador.EM_ENTREGA, e.getStatus());
        assertThrows(RegraNegocioException.class, () -> p2.despachar(e));

        p1.registrarEntrega(30);
        assertEquals(StatusEntregador.DISPONIVEL, e.getStatus());
        p2.despachar(e);
    }

    @Test
    @DisplayName("RN08 - a plataforma escolhe o entregador disponível com menos entregas")
    void distribuicaoJusta() {
        Entregador marcos = plataforma.cadastrarEntregador("Marcos", "Moto");
        Entregador juliana = plataforma.cadastrarEntregador("Juliana", "Moto");

        Pedido p1 = pedidoEmPreparo(1);
        assertSame(marcos, plataforma.despacharPedido(p1.getId()));
        p1.registrarEntrega(30);

        Pedido p2 = pedidoEmPreparo(1);
        assertSame(juliana, plataforma.despacharPedido(p2.getId())); // Marcos já tem 1 entrega
    }

    @Test
    @DisplayName("RN08 - sem entregador disponível o pedido não sai")
    void semEntregadorDisponivel() {
        Pedido p1 = pedidoEmPreparo(1);
        assertThrows(RegraNegocioException.class, () -> plataforma.despacharPedido(p1.getId()));
    }

    @Test
    @DisplayName("RN08 - entregador não pode ser desativado no meio de uma entrega")
    void naoDesativaEmEntrega() {
        Entregador e = plataforma.cadastrarEntregador("Marcos", "Moto");
        pedidoEmPreparo(1).despachar(e);
        assertThrows(RegraNegocioException.class, e::desativar);
    }

    @Test
    @DisplayName("RN09 - prazo = preparo + 3 min/km e indica atraso")
    void prazoEAtraso() {
        plataforma.cadastrarEntregador("Marcos", "Moto");
        Pedido pedido = pedidoEmPreparo(1);
        assertEquals(35, pedido.getPrazoEstimadoMinutos());

        plataforma.despacharPedido(pedido.getId());
        pedido.registrarEntrega(50);
        assertEquals(15, pedido.getMinutosDeAtraso());
        assertTrue(!pedido.foiEntregueNoPrazo());
    }

    @Test
    @DisplayName("RN09 - comissão = 80% do frete + bônus de R$ 2 se entregou no prazo")
    void comissaoComBonus() {
        Entregador e = plataforma.cadastrarEntregador("Marcos", "Moto");
        Pedido noPrazo = pedidoEmPreparo(1);
        plataforma.despacharPedido(noPrazo.getId());
        noPrazo.registrarEntrega(30);
        assertEquals(11.20, e.calcularComissao(noPrazo), DELTA); // 9,20 + 2,00

        Pedido atrasado = pedidoEmPreparo(1);
        plataforma.despacharPedido(atrasado.getId());
        atrasado.registrarEntrega(50);
        assertEquals(9.20, e.calcularComissao(atrasado), DELTA);
    }

    @Test
    @DisplayName("RN09 - com frete grátis o entregador recebe a comissão mínima")
    void comissaoMinima() {
        Entregador e = plataforma.cadastrarEntregador("Marcos", "Moto");
        Pedido pedido = pedidoEmPreparo(2); // R$ 100 -> frete grátis
        plataforma.despacharPedido(pedido.getId());
        pedido.registrarEntrega(30);
        assertEquals(6.00, e.calcularComissao(pedido), DELTA); // 4,00 mínimo + 2,00 bônus
    }
}
