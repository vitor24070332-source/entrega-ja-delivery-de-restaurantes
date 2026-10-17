package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.util.ArrayList;

/**
 * Cliente que faz pedidos. Guarda o próprio histórico de pedidos
 * (associação Cliente -> vários Pedidos) e calcula seu nível de fidelidade.
 */
public class Cliente {

    private int id;
    private String nome;
    private String telefone;
    private Localizacao localizacao;
    private boolean ativo;
    private ArrayList<Pedido> pedidos;

    public Cliente(int id, String nome, String telefone, Localizacao localizacao) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do cliente é obrigatório.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.telefone = telefone;
        this.localizacao = localizacao;
        this.ativo = true;
        this.pedidos = new ArrayList<>();
    }

    public void adicionarPedido(Pedido pedido) {
        pedidos.add(pedido);
    }

    public void removerPedido(Pedido pedido) {
        pedidos.remove(pedido);
    }

    public int quantidadePedidosEntregues() {
        int quantidade = 0;
        for (Pedido p : pedidos) {
            if (p.getStatus() == StatusPedido.ENTREGUE) {
                quantidade++;
            }
        }
        return quantidade;
    }

    /** RN06 - o nível muda automaticamente conforme o cliente recebe pedidos. */
    public NivelFidelidade getNivelFidelidade() {
        return NivelFidelidade.calcular(quantidadePedidosEntregues());
    }

    public double calcularTotalGasto() {
        double total = 0;
        for (Pedido p : pedidos) {
            if (p.getStatus() == StatusPedido.ENTREGUE) {
                total += p.getTotal();
            }
        }
        return Moeda.arredondar(total);
    }

    public boolean possuiPedidosEmAndamento() {
        for (Pedido p : pedidos) {
            if (p.getStatus().emAndamento()) {
                return true;
            }
        }
        return false;
    }

    public void alterarEndereco(Localizacao novaLocalizacao) {
        if (possuiPedidosEmAndamento()) {
            throw new RegraNegocioException("Não é possível mudar o endereço com pedidos em andamento.");
        }
        this.localizacao = novaLocalizacao;
    }

    public void desativar() {
        if (possuiPedidosEmAndamento()) {
            throw new RegraNegocioException("Não é possível desativar: o cliente tem pedidos em andamento.");
        }
        ativo = false;
    }

    public void ativar() {
        ativo = true;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do cliente é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public ArrayList<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    @Override
    public String toString() {
        return String.format("#%d %-20s %-15s %-8s %s | %d entregues%s",
                id, nome, telefone, getNivelFidelidade(), localizacao.getEndereco(),
                quantidadePedidosEntregues(), ativo ? "" : " | INATIVO");
    }
}
