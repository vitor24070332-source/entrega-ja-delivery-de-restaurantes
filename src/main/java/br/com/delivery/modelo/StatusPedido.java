package br.com.delivery.modelo;

/**
 * Etapas do ciclo de vida de um pedido (funcionalidade principal do sistema).
 *
 * ABERTO -> CONFIRMADO -> EM_PREPARO -> SAIU_PARA_ENTREGA -> ENTREGUE
 *   \__________\______________\______-> CANCELADO (só até EM_PREPARO)
 */
public enum StatusPedido {
    ABERTO("Aberto (montando carrinho)"),
    CONFIRMADO("Confirmado (pago)"),
    EM_PREPARO("Em preparo"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Pedido que já foi pago mas ainda não terminou (nem entregue, nem cancelado). */
    public boolean emAndamento() {
        return this == CONFIRMADO || this == EM_PREPARO || this == SAIU_PARA_ENTREGA;
    }
}
