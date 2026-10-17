package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * RN05 - Cupom de desconto.
 * Um cupom só vale se: estiver ativo, dentro da validade, ainda tiver usos
 * disponíveis e o subtotal do pedido atingir o valor mínimo.
 * O desconto é percentual, mas limitado a um valor máximo (teto).
 */
public class Cupom {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String codigo;
    private double percentual;
    private double descontoMaximo;
    private double valorMinimoPedido;
    private LocalDate validade;
    private int limiteUsos;
    private int usos;
    private boolean ativo;

    public Cupom(String codigo, double percentual, double descontoMaximo,
                 double valorMinimoPedido, LocalDate validade, int limiteUsos) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraNegocioException("Código do cupom é obrigatório.");
        }
        if (percentual <= 0 || percentual > 100) {
            throw new RegraNegocioException("Percentual deve estar entre 0 e 100.");
        }
        if (descontoMaximo <= 0 || limiteUsos <= 0) {
            throw new RegraNegocioException("Desconto máximo e limite de usos devem ser maiores que zero.");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.percentual = percentual;
        this.descontoMaximo = descontoMaximo;
        this.valorMinimoPedido = valorMinimoPedido;
        this.validade = validade;
        this.limiteUsos = limiteUsos;
        this.usos = 0;
        this.ativo = true;
    }

    /** Lança exceção explicando o motivo se o cupom não puder ser usado. */
    public void validar(LocalDate hoje, double subtotal) {
        if (!ativo) {
            throw new RegraNegocioException("Cupom " + codigo + " está desativado.");
        }
        if (hoje.isAfter(validade)) {
            throw new RegraNegocioException("Cupom " + codigo + " expirou em " + validade.format(FORMATO_DATA) + ".");
        }
        if (usos >= limiteUsos) {
            throw new RegraNegocioException("Cupom " + codigo + " atingiu o limite de usos.");
        }
        if (subtotal < valorMinimoPedido) {
            throw new RegraNegocioException("Cupom " + codigo + " exige pedido mínimo de "
                    + Moeda.formatar(valorMinimoPedido) + ".");
        }
    }

    /** Versão que só responde sim/não (usada no recálculo do pedido). */
    public boolean estaValido(LocalDate hoje, double subtotal) {
        try {
            validar(hoje, subtotal);
            return true;
        } catch (RegraNegocioException e) {
            return false;
        }
    }

    public double calcularDesconto(double subtotal) {
        double desconto = subtotal * percentual / 100.0;
        return Moeda.arredondar(Math.min(desconto, descontoMaximo));
    }

    /** Chamado quando o pedido que usou o cupom é confirmado. */
    public void registrarUso() {
        usos++;
    }

    /** Chamado quando o pedido que usou o cupom é cancelado: o uso volta a ficar disponível. */
    public void liberarUso() {
        if (usos > 0) {
            usos--;
        }
    }

    public void desativar() {
        ativo = false;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getPercentual() {
        return percentual;
    }

    public double getDescontoMaximo() {
        return descontoMaximo;
    }

    public double getValorMinimoPedido() {
        return valorMinimoPedido;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public int getLimiteUsos() {
        return limiteUsos;
    }

    public int getUsos() {
        return usos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    @Override
    public String toString() {
        return String.format("%-10s %.0f%% (máx %s) | mínimo %s | válido até %s | usos %d/%d%s",
                codigo, percentual, Moeda.formatar(descontoMaximo), Moeda.formatar(valorMinimoPedido),
                validade.format(FORMATO_DATA), usos, limiteUsos, ativo ? "" : " | DESATIVADO");
    }
}
