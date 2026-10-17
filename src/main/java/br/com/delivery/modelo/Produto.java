package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

/**
 * Item do cardápio de um restaurante.
 * O próprio produto controla seu estoque (RN01): ninguém altera o atributo
 * "estoque" diretamente, só através de baixarEstoque/devolverEstoque/reporEstoque.
 */
public class Produto {

    private int id;
    private String nome;
    private String categoria;
    private double preco;
    private int estoque;
    private boolean ativo;

    public Produto(int id, String nome, String categoria, double preco, int estoque) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do produto é obrigatório.");
        }
        if (preco <= 0) {
            throw new RegraNegocioException("Preço deve ser maior que zero.");
        }
        if (estoque < 0) {
            throw new RegraNegocioException("Estoque não pode ser negativo.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.categoria = categoria;
        this.preco = Moeda.arredondar(preco);
        this.estoque = estoque;
        this.ativo = true;
    }

    // ---------------- RN01 - Controle de estoque ----------------

    /** Um produto só pode ser vendido se está ativo e tem pelo menos 1 unidade. */
    public boolean estaDisponivel() {
        return ativo && estoque > 0;
    }

    public boolean temEstoque(int quantidade) {
        return estoque >= quantidade;
    }

    /** Chamado quando o pedido é confirmado/pago. */
    public void baixarEstoque(int quantidade) {
        if (!temEstoque(quantidade)) {
            throw new RegraNegocioException("Estoque insuficiente de " + nome
                    + ": pedido " + quantidade + ", disponível " + estoque + ".");
        }
        estoque -= quantidade;
    }

    /** Chamado quando um pedido confirmado é cancelado antes do preparo. */
    public void devolverEstoque(int quantidade) {
        estoque += quantidade;
    }

    /** Entrada de mercadoria (reposição feita pelo restaurante). */
    public void reporEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade de reposição deve ser maior que zero.");
        }
        estoque += quantidade;
    }

    // ---------------- Edição ----------------

    public void alterarPreco(double novoPreco) {
        if (novoPreco <= 0) {
            throw new RegraNegocioException("Preço deve ser maior que zero.");
        }
        this.preco = Moeda.arredondar(novoPreco);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    // ---------------- Getters / setters ----------------

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do produto é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public boolean isAtivo() {
        return ativo;
    }

    @Override
    public String toString() {
        String situacao = !ativo ? "INATIVO" : (estoque == 0 ? "ESGOTADO" : "estoque " + estoque);
        return String.format("#%d %-28s %-12s %10s  [%s]", id, nome, categoria, Moeda.formatar(preco), situacao);
    }
}
