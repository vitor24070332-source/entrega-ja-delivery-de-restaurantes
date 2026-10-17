package br.com.delivery.excecao;

/**
 * Exceção lançada quando uma operação viola uma regra de negócio do sistema.
 * Ex.: adicionar ao pedido um produto sem estoque, cancelar um pedido que já saiu para entrega.
 *
 * É uma RuntimeException para não obrigar try/catch em todo lugar;
 * o menu (camada app) captura e mostra a mensagem ao usuário.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
