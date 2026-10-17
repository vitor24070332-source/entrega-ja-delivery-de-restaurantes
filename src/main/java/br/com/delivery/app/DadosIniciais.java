package br.com.delivery.app;

import br.com.delivery.modelo.FormaPagamento;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.Pagamento;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.servico.Plataforma;

import java.time.LocalDate;

/**
 * Carrega dados de exemplo para a apresentação.
 *
 * Os pedidos de histórico são criados pelo MESMO fluxo que o usuário usa no menu
 * (criar -> adicionar itens -> confirmar -> preparo -> despachar -> entregar -> avaliar),
 * então todas as regras de negócio são aplicadas também aqui.
 */
public class DadosIniciais {

    private DadosIniciais() {
    }

    public static void carregar(Plataforma p) {
        // ---------- Restaurantes e cardápios ----------
        // (nome, categoria, local, preparo min, pedido mínimo, frete grátis a partir de)
        p.cadastrarRestaurante("Cozinha da Vó", "Brasileira",
                new Localizacao("Rua das Flores, 100", 2, 3), 25, 30, 80);          // id 1
        p.cadastrarRestaurante("Pizzaria Bella Napoli", "Pizza",
                new Localizacao("Av. Central, 1500", 5, 1), 30, 40, 100);           // id 2
        p.cadastrarRestaurante("Açaí da Praça", "Sobremesa",
                new Localizacao("Praça da Matriz, 12", 1, 1), 10, 15, 0);           // id 3
        p.cadastrarRestaurante("Burger Station", "Lanches",
                new Localizacao("Rua do Comércio, 845", 8, 8), 20, 25, 70);         // id 4

        p.cadastrarProduto(1, "Feijoada", "Prato", 42.90, 15);                       // 1
        p.cadastrarProduto(1, "Frango com Quiabo", "Prato", 36.00, 10);              // 2
        p.cadastrarProduto(1, "Escondidinho de Carne", "Prato", 34.50, 12);          // 3
        p.cadastrarProduto(1, "Suco de Laranja", "Bebida", 9.00, 40);                // 4

        p.cadastrarProduto(2, "Pizza Margherita", "Pizza", 49.90, 20);               // 5
        p.cadastrarProduto(2, "Pizza Calabresa", "Pizza", 52.90, 20);                // 6
        p.cadastrarProduto(2, "Pizza Quatro Queijos", "Pizza", 58.90, 3);            // 7
        p.cadastrarProduto(2, "Refrigerante 2L", "Bebida", 14.00, 30);               // 8

        p.cadastrarProduto(3, "Açaí 500ml", "Sobremesa", 18.90, 50);                 // 9
        p.cadastrarProduto(3, "Açaí 1L", "Sobremesa", 32.00, 25);                    // 10
        p.cadastrarProduto(3, "Tapioca de Queijo", "Lanche", 12.00, 2);              // 11

        p.cadastrarProduto(4, "X-Burger", "Lanche", 28.00, 25);                      // 12
        p.cadastrarProduto(4, "X-Bacon", "Lanche", 34.00, 15);                       // 13
        p.cadastrarProduto(4, "Batata Frita", "Porção", 16.00, 0);                   // 14 (esgotado)
        p.cadastrarProduto(4, "Milkshake", "Bebida", 19.00, 10);                     // 15

        // ---------- Clientes ----------
        p.cadastrarCliente("Ana Souza", "(91) 98888-1111", new Localizacao("Rua A, 10", 3, 4));        // 1
        p.cadastrarCliente("Bruno Lima", "(91) 98888-2222", new Localizacao("Rua B, 220", 6, 2));      // 2
        p.cadastrarCliente("Carla Mendes", "(91) 98888-3333", new Localizacao("Rua C, 35", 1, 5));     // 3
        p.cadastrarCliente("Diego Rocha", "(91) 98888-4444", new Localizacao("Rua D, 400", 9, 6));     // 4
        p.cadastrarCliente("Carlos Prado", "(91) 98888-5555", new Localizacao("Distrito Rural, km 18", 15, 15)); // 5 (longe)

        // ---------- Entregadores ----------
        p.cadastrarEntregador("Marcos Silva", "Moto");
        p.cadastrarEntregador("Juliana Costa", "Moto");
        p.cadastrarEntregador("Pedro Alves", "Bicicleta");

        // ---------- Cupons ----------
        LocalDate hoje = LocalDate.now();
        p.cadastrarCupom("BEMVINDO", 10, 15, 30, hoje.plusDays(60), 100);
        p.cadastrarCupom("PIZZA20", 20, 25, 60, hoje.plusDays(30), 50);
        p.cadastrarCupom("VERAO", 15, 20, 20, hoje.minusDays(1), 100); // já expirado (para demonstrar RN05)

        // ---------- Histórico de pedidos ----------
        // Ana: 5 pedidos entregues -> vira cliente PRATA (RN06)
        entregue(p, 1, 1, new int[][]{{1, 1}, {4, 2}}, null, FormaPagamento.PIX, 28, 5);
        entregue(p, 1, 2, new int[][]{{5, 1}, {8, 1}}, null, FormaPagamento.PIX, 40, 4);
        entregue(p, 1, 3, new int[][]{{10, 1}}, null, FormaPagamento.CARTAO, 25, 3);       // atrasado
        entregue(p, 1, 4, new int[][]{{13, 2}}, null, FormaPagamento.CARTAO, 35, 4);
        entregue(p, 1, 1, new int[][]{{3, 2}, {4, 1}}, null, FormaPagamento.CARTAO, 30, 5);
        // Bruno com cupom PIZZA20 e frete grátis (subtotal >= 100)
        entregue(p, 2, 2, new int[][]{{6, 2}}, "PIZZA20", FormaPagamento.PIX, 33, 5);
        // Carla com cupom BEMVINDO
        entregue(p, 3, 3, new int[][]{{9, 2}}, "BEMVINDO", FormaPagamento.CARTAO, 20, 4);
        // Diego: frete grátis no Burger, mas entrega atrasada
        entregue(p, 4, 4, new int[][]{{12, 2}, {15, 1}}, null, FormaPagamento.PIX, 45, 2);

        // Bruno cancelou durante o preparo -> multa (RN07)
        Pedido cancelado = confirmado(p, 2, 1, new int[][]{{1, 1}}, FormaPagamento.PIX);
        cancelado.iniciarPreparo();
        cancelado.cancelar();

        // Pedidos em andamento, prontos para demonstrar o fluxo na apresentação
        Pedido emPreparo = confirmado(p, 3, 2, new int[][]{{5, 1}}, FormaPagamento.CARTAO);
        emPreparo.iniciarPreparo();
        confirmado(p, 4, 1, new int[][]{{1, 1}}, FormaPagamento.DINHEIRO);
    }

    private static Pedido confirmado(Plataforma p, int clienteId, int restauranteId,
                                     int[][] itens, FormaPagamento forma) {
        Pedido pedido = p.criarPedido(clienteId, restauranteId);
        for (int[] item : itens) {
            pedido.adicionarItem(p.buscarProduto(item[0]), item[1]);
        }
        Pagamento pagamento = forma == FormaPagamento.DINHEIRO
                ? new Pagamento(forma, 100.00)
                : new Pagamento(forma);
        pedido.confirmar(pagamento);
        return pedido;
    }

    private static void entregue(Plataforma p, int clienteId, int restauranteId, int[][] itens,
                                 String cupom, FormaPagamento forma, int minutos, int nota) {
        Pedido pedido = p.criarPedido(clienteId, restauranteId);
        for (int[] item : itens) {
            pedido.adicionarItem(p.buscarProduto(item[0]), item[1]);
        }
        if (cupom != null) {
            pedido.aplicarCupom(p.buscarCupom(cupom));
        }
        pedido.confirmar(new Pagamento(forma));
        pedido.iniciarPreparo();
        p.despacharPedido(pedido.getId());
        pedido.registrarEntrega(minutos);
        pedido.avaliar(nota);
    }
}
