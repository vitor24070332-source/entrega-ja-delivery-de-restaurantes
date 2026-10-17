package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.FormaPagamento;
import br.com.delivery.modelo.Pagamento;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.Produto;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.modelo.StatusPedido;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.util.Moeda;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Menu da funcionalidade principal: o ciclo completo do pedido.
 * Repare que o menu só lê dados e chama métodos: quem decide se pode ou não
 * é o Pedido (e as classes que ele usa).
 */
public class MenuPedidos {

    private Plataforma plataforma;
    private Console console;

    public MenuPedidos(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("PEDIDOS");
            console.mensagem("1 - Novo pedido");
            console.mensagem("2 - Continuar pedido em aberto");
            console.mensagem("3 - Iniciar preparo");
            console.mensagem("4 - Despachar (entregador escolhido automaticamente)");
            console.mensagem("5 - Registrar entrega");
            console.mensagem("6 - Cancelar pedido");
            console.mensagem("7 - Avaliar pedido");
            console.mensagem("8 - Listar pedidos");
            console.mensagem("9 - Consultar pedido");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> novoPedido();
                    case 2 -> continuarPedido();
                    case 3 -> iniciarPreparo();
                    case 4 -> despachar();
                    case 5 -> registrarEntrega();
                    case 6 -> cancelar();
                    case 7 -> avaliar();
                    case 8 -> listar();
                    case 9 -> console.mensagem(lerPedido().detalhar());
                    default -> console.erro("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                console.erro(e.getMessage());
            }
        }
    }

    private Pedido lerPedido() {
        return plataforma.buscarPedido(console.lerInteiro("Id do pedido"));
    }

    // ---------------- Etapa 1: carrinho ----------------

    private void novoPedido() {
        console.mensagem("Clientes ativos:");
        for (Cliente c : plataforma.listarClientes()) {
            if (c.isAtivo()) {
                console.mensagem("  " + c);
            }
        }
        int clienteId = console.lerInteiro("Id do cliente");

        console.mensagem("Restaurantes abertos:");
        for (Restaurante r : plataforma.listarRestaurantes()) {
            if (r.isAberto()) {
                console.mensagem("  " + r);
            }
        }
        int restauranteId = console.lerInteiro("Id do restaurante");

        Pedido pedido = plataforma.criarPedido(clienteId, restauranteId);
        console.sucesso("Pedido #" + pedido.getId() + " criado.");
        editarCarrinho(pedido);
    }

    private void continuarPedido() {
        console.listar(plataforma.listarPedidosPorStatus(StatusPedido.ABERTO), "Nenhum pedido em aberto.");
        Pedido pedido = lerPedido();
        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new RegraNegocioException("Esse pedido não está mais em aberto.");
        }
        editarCarrinho(pedido);
    }

    private void editarCarrinho(Pedido pedido) {
        while (true) {
            console.titulo("CARRINHO - Pedido #" + pedido.getId());
            console.mensagem(pedido.detalhar());
            console.mensagem("1 - Adicionar item");
            console.mensagem("2 - Remover item");
            console.mensagem("3 - Aplicar cupom");
            console.mensagem("4 - Remover cupom");
            console.mensagem("5 - Finalizar e pagar");
            console.mensagem("6 - Salvar e sair (pedido continua em aberto)");
            console.mensagem("7 - Descartar pedido");
            int opcao = console.lerInteiro("Opção");
            try {
                switch (opcao) {
                    case 1 -> adicionarItem(pedido);
                    case 2 -> pedido.removerItem(console.lerInteiro("Id do produto a remover"));
                    case 3 -> {
                        pedido.aplicarCupom(plataforma.buscarCupom(console.lerTexto("Código do cupom")));
                        console.sucesso("Cupom aceito. Desconto aplicado: " + pedido.getOrigemDesconto());
                    }
                    case 4 -> pedido.removerCupom();
                    case 5 -> {
                        if (finalizar(pedido)) {
                            return;
                        }
                    }
                    case 6 -> {
                        console.sucesso("Pedido #" + pedido.getId() + " salvo em aberto.");
                        return;
                    }
                    case 7 -> {
                        plataforma.descartarPedido(pedido.getId());
                        console.sucesso("Pedido descartado.");
                        return;
                    }
                    default -> console.erro("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                console.erro(e.getMessage());
            }
        }
    }

    private void adicionarItem(Pedido pedido) {
        console.mensagem("Produtos disponíveis:");
        console.listar(pedido.getRestaurante().listarProdutosDisponiveis(), "Nenhum produto disponível.");
        int produtoId = console.lerInteiro("Id do produto");
        Produto produto = pedido.getRestaurante().buscarProduto(produtoId);
        int quantidade = console.lerInteiro("Quantidade");
        pedido.adicionarItem(produto, quantidade);
        console.sucesso(quantidade + "x " + produto.getNome() + " adicionado.");
    }

    // ---------------- Etapa 2: pagamento ----------------

    /** @return true se o pedido foi confirmado */
    private boolean finalizar(Pedido pedido) {
        console.mensagem("Total a pagar: " + Moeda.formatar(pedido.getTotal()));
        console.mensagem("Forma de pagamento: 1 - PIX | 2 - Cartão | 3 - Dinheiro");
        int forma = console.lerInteiro("Opção");
        Pagamento pagamento;
        switch (forma) {
            case 1 -> pagamento = new Pagamento(FormaPagamento.PIX);
            case 2 -> pagamento = new Pagamento(FormaPagamento.CARTAO);
            case 3 -> pagamento = new Pagamento(FormaPagamento.DINHEIRO,
                    console.lerDecimal("Valor que o cliente vai entregar (R$)"));
            default -> {
                console.erro("Forma de pagamento inválida.");
                return false;
            }
        }
        pedido.confirmar(pagamento);
        console.sucesso("Pedido #" + pedido.getId() + " confirmado! " + pagamento);
        console.sucesso("Prazo estimado: " + pedido.getPrazoEstimadoMinutos() + " minutos.");
        return true;
    }

    // ---------------- Etapas 3 a 5 ----------------

    private void iniciarPreparo() {
        console.listar(plataforma.listarPedidosPorStatus(StatusPedido.CONFIRMADO), "Nenhum pedido confirmado.");
        Pedido pedido = lerPedido();
        pedido.iniciarPreparo();
        console.sucesso("Pedido #" + pedido.getId() + " em preparo.");
    }

    private void despachar() {
        console.listar(plataforma.listarPedidosPorStatus(StatusPedido.EM_PREPARO), "Nenhum pedido em preparo.");
        int pedidoId = console.lerInteiro("Id do pedido");
        Entregador entregador = plataforma.despacharPedido(pedidoId);
        console.sucesso("Pedido #" + pedidoId + " saiu para entrega com " + entregador.getNome() + ".");
    }

    private void registrarEntrega() {
        console.listar(plataforma.listarPedidosPorStatus(StatusPedido.SAIU_PARA_ENTREGA), "Nenhum pedido em rota.");
        Pedido pedido = lerPedido();
        console.mensagem("Prazo prometido: " + pedido.getPrazoEstimadoMinutos() + " min");
        int minutos = console.lerInteiro("Tempo total desde a confirmação (min)");
        pedido.registrarEntrega(minutos);
        if (pedido.foiEntregueNoPrazo()) {
            console.sucesso("Entregue NO PRAZO.");
        } else {
            console.sucesso("Entregue com ATRASO de " + pedido.getMinutosDeAtraso() + " min.");
        }
        Entregador e = pedido.getEntregador();
        console.sucesso("Comissão de " + e.getNome() + ": " + Moeda.formatar(e.calcularComissao(pedido)));
    }

    private void cancelar() {
        Pedido pedido = lerPedido();
        console.mensagem(pedido.detalhar());
        if (!console.confirmar("Confirmar cancelamento?")) {
            return;
        }
        pedido.cancelar();
        console.sucesso("Pedido cancelado.");
        if (pedido.getPagamento() != null) {
            console.sucesso("Estorno ao cliente: " + Moeda.formatar(pedido.getPagamento().getValorEstornado()));
        }
        if (pedido.getMultaCancelamento() > 0) {
            console.sucesso("Multa retida (pedido já estava em preparo): "
                    + Moeda.formatar(pedido.getMultaCancelamento()));
        }
    }

    private void avaliar() {
        console.listar(plataforma.listarPedidosPorStatus(StatusPedido.ENTREGUE), "Nenhum pedido entregue.");
        Pedido pedido = lerPedido();
        pedido.avaliar(console.lerInteiro("Nota (1 a 5)"));
        console.sucesso("Obrigado! Nova nota média de " + pedido.getRestaurante().getNome() + ": "
                + String.format("%.1f", pedido.getRestaurante().getNotaMedia()));
    }

    private void listar() {
        console.mensagem("1 - Todos (mais recentes primeiro)  2 - Por status  3 - Por valor (maior primeiro)");
        int opcao = console.lerInteiro("Opção");
        if (opcao == 2) {
            StatusPedido[] status = StatusPedido.values();
            for (int i = 0; i < status.length; i++) {
                console.mensagem((i + 1) + " - " + status[i].getDescricao());
            }
            int escolhido = console.lerInteiro("Status");
            if (escolhido < 1 || escolhido > status.length) {
                throw new RegraNegocioException("Status inválido.");
            }
            console.listar(plataforma.listarPedidosPorStatus(status[escolhido - 1]), "Nenhum pedido.");
        } else if (opcao == 3) {
            console.listar(plataforma.listarPedidosPorValor(), "Nenhum pedido.");
        } else {
            ArrayList<Pedido> pedidos = plataforma.listarPedidos();
            Collections.reverse(pedidos);
            console.listar(pedidos, "Nenhum pedido.");
        }
    }
}
