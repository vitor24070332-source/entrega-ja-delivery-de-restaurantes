package br.com.delivery;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.Cupom;
import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.FormaPagamento;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.NivelFidelidade;
import br.com.delivery.modelo.Pagamento;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.Produto;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.modelo.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de negócio ligadas ao Pedido.
 *
 * Cenário: restaurante na posição (0,0) e cliente em (3,4) -> distância de 5 km.
 *   frete = 4,00 + 1,50 x 5 = 11,50
 *   prazo = 20 min de preparo + 5 km x 3 min = 35 min
 *   pedido mínimo = 30,00 | frete grátis a partir de 100,00
 */
class PedidoTest {

    private static final double DELTA = 0.001;

    private Restaurante restaurante;
    private Cliente cliente;
    private Produto pizza;
    private Produto refri;

    @BeforeEach
    void preparar() {
        restaurante = new Restaurante(1, "Pizzaria Teste", "Pizza", new Localizacao("Centro", 0, 0), 20, 30.0, 100.0);
        cliente = new Cliente(1, "Ana", "9999-0000", new Localizacao("Casa", 3, 4));
        pizza = new Produto(1, "Pizza", "Pizza", 50.0, 10);
        refri = new Produto(2, "Refri", "Bebida", 10.0, 5);
        restaurante.adicionarProduto(pizza);
        restaurante.adicionarProduto(refri);
    }

    private Pedido novoPedido() {
        Pedido pedido = new Pedido(1, cliente, restaurante);
        cliente.adicionarPedido(pedido);
        restaurante.adicionarPedido(pedido);
        return pedido;
    }

    private void entregar(Pedido pedido, int minutos) {
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        pedido.iniciarPreparo();
        pedido.despachar(new Entregador(1, "Motoboy", "Moto"));
        pedido.registrarEntrega(minutos);
    }

    // ---------------- RN01 - Estoque ----------------

    @Test
    @DisplayName("RN01 - não permite colocar no carrinho mais do que o estoque (somando o que já está lá)")
    void naoUltrapassaEstoque() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(refri, 4);
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem(refri, 2));
        assertEquals(4, pedido.quantidadeNoCarrinho(refri));
    }

    @Test
    @DisplayName("RN01 - produto com estoque zero fica indisponível automaticamente")
    void produtoEsgotadoFicaIndisponivel() {
        Produto esgotado = new Produto(3, "Calzone", "Pizza", 40.0, 0);
        restaurante.adicionarProduto(esgotado);
        assertFalse(esgotado.estaDisponivel());
        Pedido pedido = novoPedido();
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem(esgotado, 1));
    }

    @Test
    @DisplayName("RN01 - estoque só baixa quando o pedido é confirmado")
    void estoqueBaixaNaConfirmacao() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 2);
        assertEquals(10, pizza.getEstoque());
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        assertEquals(8, pizza.getEstoque());
    }

    // ---------------- RN02 - Restaurante aberto / produtos do mesmo restaurante ----------------

    @Test
    @DisplayName("RN02 - restaurante fechado não recebe pedidos")
    void restauranteFechadoNaoRecebePedido() {
        restaurante.fechar();
        assertThrows(RegraNegocioException.class, () -> new Pedido(1, cliente, restaurante));
    }

    @Test
    @DisplayName("RN02 - não aceita produto de outro restaurante no mesmo pedido")
    void naoMisturaRestaurantes() {
        Restaurante outro = new Restaurante(2, "Outro", "Lanches", new Localizacao("X", 1, 1), 10, 0, 0);
        Produto lanche = new Produto(9, "X-Burger", "Lanche", 25.0, 10);
        outro.adicionarProduto(lanche);
        Pedido pedido = novoPedido();
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem(lanche, 1));
    }

    // ---------------- RN03 - Pedido mínimo ----------------

    @Test
    @DisplayName("RN03 - não confirma pedido abaixo do valor mínimo e nada é alterado")
    void pedidoMinimo() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(refri, 1); // R$ 10 < R$ 30
        assertThrows(RegraNegocioException.class, () -> pedido.confirmar(new Pagamento(FormaPagamento.PIX)));
        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
        assertEquals(5, refri.getEstoque());
    }

    // ---------------- RN04 - Frete ----------------

    @Test
    @DisplayName("RN04 - frete = taxa base + valor por km")
    void calculaFretePorDistancia() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        assertEquals(11.50, pedido.getFrete(), DELTA);
        assertEquals(61.50, pedido.getTotal(), DELTA);
    }

    @Test
    @DisplayName("RN04 - frete grátis quando o subtotal atinge o valor do restaurante")
    void freteGratis() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 2); // R$ 100
        assertEquals(0.0, pedido.getFrete(), DELTA);
    }

    @Test
    @DisplayName("RN04 - não cria pedido para endereço fora do raio de entrega")
    void foraDaAreaDeEntrega() {
        Cliente longe = new Cliente(2, "Carlos", "0000", new Localizacao("Sítio", 12, 0));
        assertThrows(RegraNegocioException.class, () -> new Pedido(2, longe, restaurante));
    }

    // ---------------- RN05 - Cupom ----------------

    @Test
    @DisplayName("RN05 - cupom aplica percentual sobre o subtotal")
    void cupomPercentual() {
        Cupom cupom = new Cupom("DEZ", 10, 15, 30, LocalDate.now().plusDays(5), 10);
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pedido.aplicarCupom(cupom);
        assertEquals(5.00, pedido.getDesconto(), DELTA);
    }

    @Test
    @DisplayName("RN05 - desconto do cupom respeita o valor máximo")
    void cupomRespeitaTeto() {
        Cupom cupom = new Cupom("DEZ", 10, 15, 30, LocalDate.now().plusDays(5), 10);
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 4); // 10% de 200 = 20, mas o teto é 15
        pedido.aplicarCupom(cupom);
        assertEquals(15.00, pedido.getDesconto(), DELTA);
    }

    @Test
    @DisplayName("RN05 - cupom vencido, sem usos ou abaixo do mínimo é recusado")
    void cupomInvalido() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        Cupom vencido = new Cupom("VELHO", 10, 15, 0, LocalDate.now().minusDays(1), 10);
        Cupom minimoAlto = new Cupom("GRANDE", 10, 15, 200, LocalDate.now().plusDays(5), 10);
        Cupom esgotado = new Cupom("UNICO", 10, 15, 0, LocalDate.now().plusDays(5), 1);
        esgotado.registrarUso();
        assertThrows(RegraNegocioException.class, () -> pedido.aplicarCupom(vencido));
        assertThrows(RegraNegocioException.class, () -> pedido.aplicarCupom(minimoAlto));
        assertThrows(RegraNegocioException.class, () -> pedido.aplicarCupom(esgotado));
    }

    @Test
    @DisplayName("RN05 - confirmar consome um uso do cupom; cancelar antes do preparo devolve")
    void usoDoCupom() {
        Cupom cupom = new Cupom("DEZ", 10, 15, 30, LocalDate.now().plusDays(5), 10);
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pedido.aplicarCupom(cupom);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        assertEquals(1, cupom.getUsos());
        pedido.cancelar();
        assertEquals(0, cupom.getUsos());
    }

    // ---------------- RN06 - Fidelidade ----------------

    @Test
    @DisplayName("RN06 - cliente vira PRATA com 5 pedidos entregues")
    void nivelFidelidade() {
        for (int i = 0; i < 4; i++) {
            Pedido p = novoPedido();
            p.adicionarItem(pizza, 1);
            entregar(p, 30);
            pizza.reporEstoque(1);
        }
        assertEquals(NivelFidelidade.BRONZE, cliente.getNivelFidelidade());
        Pedido quinto = novoPedido();
        quinto.adicionarItem(pizza, 1);
        entregar(quinto, 30);
        assertEquals(NivelFidelidade.PRATA, cliente.getNivelFidelidade());
    }

    @Test
    @DisplayName("RN06 - cupom e fidelidade não acumulam: vale o maior e o cupom não é gasto à toa")
    void maiorDescontoVence() {
        for (int i = 0; i < 5; i++) {
            Pedido p = novoPedido();
            p.adicionarItem(pizza, 1);
            entregar(p, 30);
            pizza.reporEstoque(1);
        }
        // cliente PRATA = 5%. Pedido de R$ 100 -> fidelidade R$ 5,00; cupom de 3% -> R$ 3,00
        Cupom fraco = new Cupom("TRES", 3, 50, 0, LocalDate.now().plusDays(5), 10);
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 2);
        pedido.aplicarCupom(fraco);
        assertEquals(5.00, pedido.getDesconto(), DELTA);
        assertEquals("Fidelidade PRATA", pedido.getOrigemDesconto());

        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        assertEquals(0, fraco.getUsos());
        assertNull(pedido.getCupom());
    }

    // ---------------- RN07 - Cancelamento ----------------

    @Test
    @DisplayName("RN07 - cancelar pedido confirmado: estorno total e estoque devolvido")
    void cancelarConfirmado() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        assertEquals(9, pizza.getEstoque());

        pedido.cancelar();
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertEquals(10, pizza.getEstoque());
        assertEquals(61.50, pedido.getPagamento().getValorEstornado(), DELTA);
        assertEquals(0.0, pedido.getValorFaturado(), DELTA);
    }

    @Test
    @DisplayName("RN07 - cancelar durante o preparo cobra multa de 30% e não devolve estoque")
    void cancelarEmPreparo() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        pedido.iniciarPreparo();

        pedido.cancelar();
        assertEquals(15.00, pedido.getMultaCancelamento(), DELTA);          // 30% de 50
        assertEquals(46.50, pedido.getPagamento().getValorEstornado(), DELTA); // 61,50 - 15
        assertEquals(15.00, pedido.getValorFaturado(), DELTA);
        assertEquals(9, pizza.getEstoque());
    }

    @Test
    @DisplayName("RN07 - pedido que saiu para entrega não pode ser cancelado")
    void naoCancelaEmRota() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        pedido.iniciarPreparo();
        pedido.despachar(new Entregador(1, "Motoboy", "Moto"));
        assertThrows(RegraNegocioException.class, pedido::cancelar);
    }

    // ---------------- Fluxo / status ----------------

    @Test
    @DisplayName("Fluxo - não permite pular etapas do pedido")
    void naoPulaEtapas() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        assertThrows(RegraNegocioException.class, pedido::iniciarPreparo);
        pedido.confirmar(new Pagamento(FormaPagamento.PIX));
        assertThrows(RegraNegocioException.class, () -> pedido.registrarEntrega(30));
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem(refri, 1));
    }

    @Test
    @DisplayName("Fluxo - pagamento em dinheiro calcula troco e recusa valor insuficiente")
    void pagamentoEmDinheiro() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1); // total 61,50
        assertThrows(RegraNegocioException.class,
                () -> pedido.confirmar(new Pagamento(FormaPagamento.DINHEIRO, 50)));
        Pagamento pagamento = new Pagamento(FormaPagamento.DINHEIRO, 100);
        pedido.confirmar(pagamento);
        assertEquals(38.50, pagamento.getTroco(), DELTA);
    }

    @Test
    @DisplayName("Fluxo - preço fica congelado no item mesmo se o produto mudar de preço")
    void precoCongelado() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        pizza.alterarPreco(60);
        assertEquals(50.00, pedido.calcularSubtotal(), DELTA);
    }

    // ---------------- RN10 - Avaliação ----------------

    @Test
    @DisplayName("RN10 - só avalia pedido entregue, uma vez, com nota de 1 a 5")
    void avaliacao() {
        Pedido pedido = novoPedido();
        pedido.adicionarItem(pizza, 1);
        assertThrows(RegraNegocioException.class, () -> pedido.avaliar(5));

        entregar(pedido, 30);
        assertThrows(RegraNegocioException.class, () -> pedido.avaliar(6));
        pedido.avaliar(4);
        assertThrows(RegraNegocioException.class, () -> pedido.avaliar(5));
        assertEquals(4.0, restaurante.getNotaMedia(), DELTA);
        assertTrue(restaurante.getQuantidadeAvaliacoes() == 1);
    }
}
