package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.time.LocalDateTime;

/**
 * Pagamento de um pedido.
 * Em dinheiro, o cliente informa quanto vai entregar ao motoboy e o sistema calcula o troco.
 * Em PIX/cartão, o valor recebido é exatamente o total.
 */
public class Pagamento {

    private FormaPagamento forma;
    private double valorRecebido;
    private double valorCobrado;
    private double troco;
    private double valorEstornado;
    private boolean aprovado;
    private LocalDateTime dataHora;

    /** Para PIX e cartão. */
    public Pagamento(FormaPagamento forma) {
        this(forma, 0);
    }

    /** Para dinheiro: valorRecebido é a cédula que o cliente vai entregar. */
    public Pagamento(FormaPagamento forma, double valorRecebido) {
        this.forma = forma;
        this.valorRecebido = valorRecebido;
    }

    /** Efetiva o pagamento do valor total do pedido. */
    public void processar(double total) {
        if (aprovado) {
            throw new RegraNegocioException("Pagamento já foi processado.");
        }
        if (forma == FormaPagamento.DINHEIRO) {
            if (valorRecebido < total) {
                throw new RegraNegocioException("Valor em dinheiro (" + Moeda.formatar(valorRecebido)
                        + ") é menor que o total do pedido (" + Moeda.formatar(total) + ").");
            }
            troco = Moeda.arredondar(valorRecebido - total);
        } else {
            valorRecebido = total;
            troco = 0;
        }
        valorCobrado = total;
        aprovado = true;
        dataHora = LocalDateTime.now();
    }

    /** Devolve ao cliente parte (ou todo) o valor cobrado. */
    public void estornar(double valor) {
        if (!aprovado) {
            throw new RegraNegocioException("Não há pagamento aprovado para estornar.");
        }
        valorEstornado = Moeda.arredondar(valor);
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public double getValorRecebido() {
        return valorRecebido;
    }

    public double getValorCobrado() {
        return valorCobrado;
    }

    public double getTroco() {
        return troco;
    }

    public double getValorEstornado() {
        return valorEstornado;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        String texto = forma + " " + Moeda.formatar(valorCobrado);
        if (troco > 0) {
            texto += " (troco " + Moeda.formatar(troco) + ")";
        }
        if (valorEstornado > 0) {
            texto += " | estornado " + Moeda.formatar(valorEstornado);
        }
        return texto;
    }
}
