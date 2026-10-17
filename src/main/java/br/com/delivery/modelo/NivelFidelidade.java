package br.com.delivery.modelo;

/**
 * RN06 - Programa de fidelidade.
 * O nível depende de quantos pedidos o cliente já recebeu (status ENTREGUE).
 *
 *   BRONZE: 0 a 4 pedidos  -> 0% de desconto
 *   PRATA : 5 a 9 pedidos  -> 5% de desconto
 *   OURO  : 10 ou mais     -> 10% de desconto
 */
public enum NivelFidelidade {
    BRONZE(0, 0.0),
    PRATA(5, 5.0),
    OURO(10, 10.0);

    private final int pedidosMinimos;
    private final double percentualDesconto;

    NivelFidelidade(int pedidosMinimos, double percentualDesconto) {
        this.pedidosMinimos = pedidosMinimos;
        this.percentualDesconto = percentualDesconto;
    }

    /** Descobre o nível a partir da quantidade de pedidos entregues. */
    public static NivelFidelidade calcular(int pedidosEntregues) {
        if (pedidosEntregues >= OURO.pedidosMinimos) {
            return OURO;
        }
        if (pedidosEntregues >= PRATA.pedidosMinimos) {
            return PRATA;
        }
        return BRONZE;
    }

    public int getPedidosMinimos() {
        return pedidosMinimos;
    }

    public double getPercentualDesconto() {
        return percentualDesconto;
    }
}
