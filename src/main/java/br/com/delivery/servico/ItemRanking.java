package br.com.delivery.servico;

/**
 * Uma posição de ranking: um nome e um valor (faturamento, quantidade vendida, etc.).
 * Usada pelos relatórios e pela exportação CSV.
 */
public class ItemRanking {

    private String descricao;
    private double valor;

    public ItemRanking(String descricao, double valor) {
        this.descricao = descricao;
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getValor() {
        return valor;
    }
}
