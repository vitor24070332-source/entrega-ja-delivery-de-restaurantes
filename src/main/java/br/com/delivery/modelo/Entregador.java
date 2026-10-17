package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.util.ArrayList;

/**
 * Entregador (motoboy/ciclista). Guarda a lista de pedidos que já levou
 * e controla sua própria disponibilidade (RN08) e comissão (RN09).
 */
public class Entregador {

    // RN09 - comissão do entregador
    public static final double PERCENTUAL_DO_FRETE = 0.80;
    public static final double COMISSAO_MINIMA = 4.00;
    public static final double BONUS_PONTUALIDADE = 2.00;

    private int id;
    private String nome;
    private String veiculo;
    private StatusEntregador status;
    private ArrayList<Pedido> entregas;

    public Entregador(int id, String nome, String veiculo) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do entregador é obrigatório.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.veiculo = veiculo;
        this.status = StatusEntregador.DISPONIVEL;
        this.entregas = new ArrayList<>();
    }

    // ---------------- RN08 - Disponibilidade ----------------

    public boolean estaDisponivel() {
        return status == StatusEntregador.DISPONIVEL;
    }

    /** O entregador só pode levar UM pedido por vez. */
    public void assumirEntrega(Pedido pedido) {
        if (!estaDisponivel()) {
            throw new RegraNegocioException("Entregador " + nome + " não está disponível (" + status + ").");
        }
        entregas.add(pedido);
        status = StatusEntregador.EM_ENTREGA;
    }

    /** Ao concluir a entrega, volta automaticamente a ficar disponível. */
    public void finalizarEntrega() {
        status = StatusEntregador.DISPONIVEL;
    }

    // ---------------- RN09 - Comissão ----------------

    /**
     * Comissão = 80% do frete (mínimo R$ 4,00), + R$ 2,00 de bônus se entregou no prazo.
     * O mínimo garante que o entregador recebe algo mesmo quando o frete é grátis para o cliente.
     */
    public double calcularComissao(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.ENTREGUE) {
            return 0.0;
        }
        double comissao = Math.max(pedido.getFrete() * PERCENTUAL_DO_FRETE, COMISSAO_MINIMA);
        if (pedido.foiEntregueNoPrazo()) {
            comissao += BONUS_PONTUALIDADE;
        }
        return Moeda.arredondar(comissao);
    }

    public double calcularTotalComissoes() {
        double total = 0;
        for (Pedido p : entregas) {
            total += calcularComissao(p);
        }
        return Moeda.arredondar(total);
    }

    public int quantidadeEntregasConcluidas() {
        int quantidade = 0;
        for (Pedido p : entregas) {
            if (p.getStatus() == StatusPedido.ENTREGUE) {
                quantidade++;
            }
        }
        return quantidade;
    }

    public int quantidadeEntregasNoPrazo() {
        int quantidade = 0;
        for (Pedido p : entregas) {
            if (p.getStatus() == StatusPedido.ENTREGUE && p.foiEntregueNoPrazo()) {
                quantidade++;
            }
        }
        return quantidade;
    }

    public void desativar() {
        if (status == StatusEntregador.EM_ENTREGA) {
            throw new RegraNegocioException("Não é possível desativar um entregador durante uma entrega.");
        }
        status = StatusEntregador.INATIVO;
    }

    public void ativar() {
        if (status == StatusEntregador.INATIVO) {
            status = StatusEntregador.DISPONIVEL;
        }
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do entregador é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(String veiculo) {
        this.veiculo = veiculo;
    }

    public StatusEntregador getStatus() {
        return status;
    }

    public ArrayList<Pedido> getEntregas() {
        return new ArrayList<>(entregas);
    }

    @Override
    public String toString() {
        return String.format("#%d %-18s %-8s %-11s | %d entregas | comissões %s",
                id, nome, veiculo, status, quantidadeEntregasConcluidas(), Moeda.formatar(calcularTotalComissoes()));
    }
}
