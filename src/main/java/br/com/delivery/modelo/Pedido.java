package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Classe central da funcionalidade principal: o ciclo completo do pedido.
 *
 *   criar (ABERTO) -> adicionar itens / cupom -> confirmar com pagamento (CONFIRMADO)
 *   -> iniciar preparo (EM_PREPARO) -> despachar com entregador (SAIU_PARA_ENTREGA)
 *   -> registrar entrega (ENTREGUE) -> avaliar
 *
 * O pedido liga várias classes: Cliente, Restaurante, Produto (via ItemPedido),
 * Cupom, Pagamento e Entregador.
 */
public class Pedido {

    /** RN07 - multa sobre o valor dos produtos quando o cancelamento ocorre durante o preparo. */
    public static final double PERCENTUAL_MULTA_CANCELAMENTO = 0.30;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private Cliente cliente;
    private Restaurante restaurante;
    private ArrayList<ItemPedido> itens;
    private StatusPedido status;
    private Cupom cupom;
    private Pagamento pagamento;
    private Entregador entregador;
    private LocalDateTime dataHoraCriacao;

    private double frete;
    private double desconto;
    private String origemDesconto;
    private boolean cupomUtilizado;
    private int prazoEstimadoMinutos;
    private int tempoEntregaMinutos;
    private double multaCancelamento;
    private int nota;

    public Pedido(int id, Cliente cliente, Restaurante restaurante) {
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Cliente " + cliente.getNome() + " está inativo.");
        }
        restaurante.validarRecebimentoPedido(); // RN02
        if (!restaurante.atendeEndereco(cliente.getLocalizacao())) { // RN04
            throw new RegraNegocioException(String.format(
                    "%s não entrega no endereço do cliente (%.1f km; máximo %.1f km).",
                    restaurante.getNome(), restaurante.distanciaAte(cliente.getLocalizacao()),
                    Restaurante.RAIO_MAXIMO_KM));
        }
        this.id = id;
        this.cliente = cliente;
        this.restaurante = restaurante;
        this.itens = new ArrayList<>();
        this.status = StatusPedido.ABERTO;
        this.dataHoraCriacao = LocalDateTime.now();
        this.origemDesconto = "Nenhum";
        this.frete = restaurante.calcularFrete(cliente.getLocalizacao(), 0);
    }

    // =====================================================================
    // Etapa 1 - Montar o carrinho
    // =====================================================================

    public void adicionarItem(Produto produto, int quantidade) {
        exigirStatus(StatusPedido.ABERTO, "adicionar itens");
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero.");
        }
        // RN02 - um pedido só pode ter produtos de UM restaurante
        if (!restaurante.possuiProduto(produto)) {
            throw new RegraNegocioException(produto.getNome() + " não pertence ao cardápio de "
                    + restaurante.getNome() + ".");
        }
        // RN01 - não vende produto inativo ou sem estoque (somando o que já está no carrinho)
        if (!produto.estaDisponivel()) {
            throw new RegraNegocioException(produto.getNome() + " está indisponível.");
        }
        int jaNoCarrinho = quantidadeNoCarrinho(produto);
        if (!produto.temEstoque(jaNoCarrinho + quantidade)) {
            throw new RegraNegocioException("Estoque insuficiente de " + produto.getNome()
                    + ": disponível " + produto.getEstoque() + ", no carrinho " + jaNoCarrinho + ".");
        }

        ItemPedido existente = buscarItem(produto);
        if (existente != null) {
            existente.aumentarQuantidade(quantidade);
        } else {
            itens.add(new ItemPedido(produto, quantidade));
        }
        atualizarValores();
    }

    public void removerItem(int produtoId) {
        exigirStatus(StatusPedido.ABERTO, "remover itens");
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).getProduto().getId() == produtoId) {
                itens.remove(i);
                atualizarValores();
                return;
            }
        }
        throw new RegraNegocioException("Produto #" + produtoId + " não está no pedido.");
    }

    public void aplicarCupom(Cupom cupom) {
        exigirStatus(StatusPedido.ABERTO, "aplicar cupom");
        cupom.validar(LocalDate.now(), calcularSubtotal()); // RN05 - lança exceção com o motivo
        this.cupom = cupom;
        atualizarValores();
    }

    public void removerCupom() {
        exigirStatus(StatusPedido.ABERTO, "remover cupom");
        this.cupom = null;
        atualizarValores();
    }

    /**
     * Recalcula frete e desconto sempre que o carrinho muda.
     *
     * RN06 - Cupom e fidelidade NÃO acumulam: o sistema aplica automaticamente
     * o MAIOR dos dois descontos para o cliente.
     */
    private void atualizarValores() {
        double subtotal = calcularSubtotal();
        frete = restaurante.calcularFrete(cliente.getLocalizacao(), subtotal);

        NivelFidelidade nivel = cliente.getNivelFidelidade();
        double descontoFidelidade = Moeda.arredondar(subtotal * nivel.getPercentualDesconto() / 100.0);

        double descontoCupom = 0;
        if (cupom != null && cupom.estaValido(LocalDate.now(), subtotal)) {
            descontoCupom = cupom.calcularDesconto(subtotal);
        }

        if (descontoCupom > 0 && descontoCupom >= descontoFidelidade) {
            desconto = descontoCupom;
            cupomUtilizado = true;
            origemDesconto = "Cupom " + cupom.getCodigo();
        } else if (descontoFidelidade > 0) {
            desconto = descontoFidelidade;
            cupomUtilizado = false;
            origemDesconto = "Fidelidade " + nivel;
        } else {
            desconto = 0;
            cupomUtilizado = false;
            origemDesconto = "Nenhum";
        }
    }

    // =====================================================================
    // Etapa 2 - Confirmar e pagar
    // =====================================================================

    public void confirmar(Pagamento pagamento) {
        exigirStatus(StatusPedido.ABERTO, "confirmar");
        if (itens.isEmpty()) {
            throw new RegraNegocioException("O pedido não tem itens.");
        }
        restaurante.validarRecebimentoPedido(); // RN02 - pode ter fechado enquanto o cliente escolhia

        // RN03 - pedido mínimo do restaurante (sobre o valor dos produtos)
        double subtotal = calcularSubtotal();
        if (subtotal < restaurante.getPedidoMinimo()) {
            throw new RegraNegocioException("Pedido mínimo de " + restaurante.getNome() + " é "
                    + Moeda.formatar(restaurante.getPedidoMinimo()) + ". Faltam "
                    + Moeda.formatar(restaurante.getPedidoMinimo() - subtotal) + ".");
        }

        // RN01 - confere o estoque de novo (outro pedido pode ter comprado antes)
        for (ItemPedido item : itens) {
            Produto p = item.getProduto();
            if (!p.isAtivo() || !p.temEstoque(item.getQuantidade())) {
                throw new RegraNegocioException(p.getNome() + " não tem mais estoque suficiente.");
            }
        }

        atualizarValores();
        pagamento.processar(getTotal());

        for (ItemPedido item : itens) {
            item.getProduto().baixarEstoque(item.getQuantidade());
        }
        if (cupomUtilizado) {
            cupom.registrarUso();
        } else {
            cupom = null; // cupom informado mas não usado (fidelidade era melhor) não gasta um uso
        }

        this.pagamento = pagamento;
        this.prazoEstimadoMinutos = restaurante.calcularPrazoEstimado(cliente.getLocalizacao());
        this.status = StatusPedido.CONFIRMADO;
    }

    // =====================================================================
    // Etapas 3, 4 e 5 - Preparo, entrega e avaliação
    // =====================================================================

    public void iniciarPreparo() {
        exigirStatus(StatusPedido.CONFIRMADO, "iniciar o preparo");
        status = StatusPedido.EM_PREPARO;
    }

    /** RN08 - o entregador precisa estar disponível; quem verifica é o próprio Entregador. */
    public void despachar(Entregador entregador) {
        exigirStatus(StatusPedido.EM_PREPARO, "sair para entrega");
        entregador.assumirEntrega(this);
        this.entregador = entregador;
        status = StatusPedido.SAIU_PARA_ENTREGA;
    }

    /**
     * @param minutosDesdeConfirmacao tempo total entre a confirmação e a chegada ao cliente
     */
    public void registrarEntrega(int minutosDesdeConfirmacao) {
        exigirStatus(StatusPedido.SAIU_PARA_ENTREGA, "registrar entrega");
        if (minutosDesdeConfirmacao <= 0) {
            throw new RegraNegocioException("Tempo de entrega deve ser maior que zero.");
        }
        this.tempoEntregaMinutos = minutosDesdeConfirmacao;
        this.status = StatusPedido.ENTREGUE;
        entregador.finalizarEntrega(); // RN08 - entregador volta a ficar disponível
    }

    /** RN09 - comparação do tempo real com o prazo prometido. */
    public boolean foiEntregueNoPrazo() {
        return status == StatusPedido.ENTREGUE && tempoEntregaMinutos <= prazoEstimadoMinutos;
    }

    public int getMinutosDeAtraso() {
        if (status != StatusPedido.ENTREGUE) {
            return 0;
        }
        return Math.max(0, tempoEntregaMinutos - prazoEstimadoMinutos);
    }

    /** RN10 - só pedidos entregues podem ser avaliados, e apenas uma vez. */
    public void avaliar(int nota) {
        exigirStatus(StatusPedido.ENTREGUE, "ser avaliado");
        if (this.nota != 0) {
            throw new RegraNegocioException("Este pedido já foi avaliado.");
        }
        if (nota < 1 || nota > 5) {
            throw new RegraNegocioException("A nota deve ser de 1 a 5.");
        }
        this.nota = nota;
        restaurante.registrarAvaliacao(nota);
    }

    // =====================================================================
    // RN07 - Cancelamento
    // =====================================================================

    /**
     * ABERTO     -> cancela sem custo (nada foi pago).
     * CONFIRMADO -> estorno integral, estoque e cupom são devolvidos.
     * EM_PREPARO -> multa de 30% do valor dos produtos; estoque NÃO volta (comida já em preparo).
     * Depois que saiu para entrega -> não pode mais cancelar.
     */
    public void cancelar() {
        switch (status) {
            case ABERTO:
                break;
            case CONFIRMADO:
                for (ItemPedido item : itens) {
                    item.getProduto().devolverEstoque(item.getQuantidade());
                }
                if (cupom != null) {
                    cupom.liberarUso();
                }
                multaCancelamento = 0;
                pagamento.estornar(getTotal());
                break;
            case EM_PREPARO:
                double valorProdutos = calcularSubtotal() - desconto;
                multaCancelamento = Moeda.arredondar(valorProdutos * PERCENTUAL_MULTA_CANCELAMENTO);
                pagamento.estornar(getTotal() - multaCancelamento);
                break;
            case SAIU_PARA_ENTREGA:
                throw new RegraNegocioException("O pedido já saiu para entrega e não pode ser cancelado.");
            default:
                throw new RegraNegocioException("Pedido " + status.getDescricao().toLowerCase()
                        + " não pode ser cancelado.");
        }
        status = StatusPedido.CANCELADO;
    }

    // =====================================================================
    // Cálculos
    // =====================================================================

    public double calcularSubtotal() {
        double soma = 0;
        for (ItemPedido item : itens) {
            soma += item.calcularSubtotal();
        }
        return Moeda.arredondar(soma);
    }

    public double getTotal() {
        return Moeda.arredondar(calcularSubtotal() - desconto + frete);
    }

    /** Quanto o pedido gerou de receita: total se entregue, multa se cancelado, zero caso contrário. */
    public double getValorFaturado() {
        if (status == StatusPedido.ENTREGUE) {
            return getTotal();
        }
        if (status == StatusPedido.CANCELADO) {
            return multaCancelamento;
        }
        return 0.0;
    }

    public int quantidadeNoCarrinho(Produto produto) {
        ItemPedido item = buscarItem(produto);
        return item == null ? 0 : item.getQuantidade();
    }

    private ItemPedido buscarItem(Produto produto) {
        for (ItemPedido item : itens) {
            if (item.getProduto() == produto) {
                return item;
            }
        }
        return null;
    }

    /** Garante que o pedido está na etapa certa antes de mudar de status. */
    private void exigirStatus(StatusPedido esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Pedido #" + id + " está \"" + status.getDescricao()
                    + "\" e não pode " + acao + ". Status necessário: " + esperado.getDescricao() + ".");
        }
    }

    // =====================================================================
    // Getters
    // =====================================================================

    public int getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Restaurante getRestaurante() {
        return restaurante;
    }

    public ArrayList<ItemPedido> getItens() {
        return new ArrayList<>(itens);
    }

    public StatusPedido getStatus() {
        return status;
    }

    public Cupom getCupom() {
        return cupom;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public Entregador getEntregador() {
        return entregador;
    }

    public LocalDateTime getDataHoraCriacao() {
        return dataHoraCriacao;
    }

    public double getFrete() {
        return frete;
    }

    public double getDesconto() {
        return desconto;
    }

    public String getOrigemDesconto() {
        return origemDesconto;
    }

    public int getPrazoEstimadoMinutos() {
        return prazoEstimadoMinutos;
    }

    public int getTempoEntregaMinutos() {
        return tempoEntregaMinutos;
    }

    public double getMultaCancelamento() {
        return multaCancelamento;
    }

    public int getNota() {
        return nota;
    }

    /** Resumo em uma linha (listagens). */
    @Override
    public String toString() {
        return String.format("#%d %-16s %-20s %-26s %10s  %s",
                id, dataHoraCriacao.format(FORMATO_DATA), cliente.getNome(), restaurante.getNome(),
                Moeda.formatar(getTotal()), status.getDescricao());
    }

    /** Descrição completa (consulta de um pedido). */
    public String detalhar() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pedido #").append(id).append(" - ").append(status.getDescricao()).append('\n');
        sb.append("Cliente: ").append(cliente.getNome()).append(" (").append(cliente.getNivelFidelidade()).append(")\n");
        sb.append("Restaurante: ").append(restaurante.getNome()).append('\n');
        sb.append(String.format("Distância: %.1f km%n", restaurante.distanciaAte(cliente.getLocalizacao())));
        sb.append("Itens:\n");
        if (itens.isEmpty()) {
            sb.append("   (vazio)\n");
        }
        for (ItemPedido item : itens) {
            sb.append("   ").append(item).append('\n');
        }
        sb.append("Subtotal: ").append(Moeda.formatar(calcularSubtotal())).append('\n');
        sb.append("Desconto: -").append(Moeda.formatar(desconto)).append(" (").append(origemDesconto).append(")\n");
        sb.append("Frete:    ").append(frete == 0 ? "GRÁTIS" : Moeda.formatar(frete)).append('\n');
        sb.append("TOTAL:    ").append(Moeda.formatar(getTotal())).append('\n');
        if (status == StatusPedido.ABERTO && calcularSubtotal() < restaurante.getPedidoMinimo()) {
            sb.append("(pedido mínimo: ").append(Moeda.formatar(restaurante.getPedidoMinimo())).append(")\n");
        }
        if (pagamento != null) {
            sb.append("Pagamento: ").append(pagamento).append('\n');
        }
        if (prazoEstimadoMinutos > 0) {
            sb.append("Prazo estimado: ").append(prazoEstimadoMinutos).append(" min\n");
        }
        if (entregador != null) {
            sb.append("Entregador: ").append(entregador.getNome()).append('\n');
        }
        if (status == StatusPedido.ENTREGUE) {
            sb.append("Entregue em ").append(tempoEntregaMinutos).append(" min - ")
                    .append(foiEntregueNoPrazo() ? "NO PRAZO" : "ATRASADO " + getMinutosDeAtraso() + " min")
                    .append('\n');
            sb.append("Avaliação: ").append(nota == 0 ? "ainda não avaliado" : nota + "/5").append('\n');
        }
        if (status == StatusPedido.CANCELADO && multaCancelamento > 0) {
            sb.append("Multa de cancelamento: ").append(Moeda.formatar(multaCancelamento)).append('\n');
        }
        return sb.toString();
    }
}
