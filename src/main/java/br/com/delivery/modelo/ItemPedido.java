package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

/**
 * Uma linha do pedido: qual produto, quantas unidades e por qual preço.
 *
 * O preço é COPIADO do produto no momento em que o item é criado. Assim, se o
 * restaurante mudar o preço depois, os pedidos antigos não mudam de valor.
 */
public class ItemPedido {

    private Produto produto;
    private int quantidade;
    private double precoUnitario;

    public ItemPedido(Produto produto, int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero.");
        }
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public void aumentarQuantidade(int adicional) {
        if (adicional <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero.");
        }
        quantidade += adicional;
    }

    public double calcularSubtotal() {
        return Moeda.arredondar(precoUnitario * quantidade);
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    @Override
    public String toString() {
        return String.format("%2dx %-28s %10s  = %s", quantidade, produto.getNome(),
                Moeda.formatar(precoUnitario), Moeda.formatar(calcularSubtotal()));
    }
}
