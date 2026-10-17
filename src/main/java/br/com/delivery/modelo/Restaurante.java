package br.com.delivery.modelo;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.util.Moeda;

import java.util.ArrayList;

/**
 * Restaurante cadastrado na plataforma.
 *
 * Composição: o restaurante POSSUI uma lista de produtos (cardápio) e uma lista
 * com os pedidos que recebeu. Também é ele quem define as políticas de entrega
 * (frete, raio de atendimento, prazo) e de pedido mínimo.
 */
public class Restaurante {

    // RN04 - parâmetros de frete (iguais para todos os restaurantes da plataforma)
    public static final double TAXA_ENTREGA_BASE = 4.00;
    public static final double VALOR_POR_KM = 1.50;
    public static final double RAIO_MAXIMO_KM = 10.0;

    // RN09 - estimativa de tempo de deslocamento do entregador
    public static final int MINUTOS_POR_KM = 3;

    private int id;
    private String nome;
    private String categoria;
    private Localizacao localizacao;
    private int tempoPreparoMinutos;
    private double pedidoMinimo;
    private double valorFreteGratis;
    private boolean aberto;
    private boolean ativo;
    private int somaNotas;
    private int quantidadeAvaliacoes;

    private ArrayList<Produto> cardapio;
    private ArrayList<Pedido> pedidos;

    public Restaurante(int id, String nome, String categoria, Localizacao localizacao,
                       int tempoPreparoMinutos, double pedidoMinimo, double valorFreteGratis) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do restaurante é obrigatório.");
        }
        if (tempoPreparoMinutos <= 0) {
            throw new RegraNegocioException("Tempo de preparo deve ser maior que zero.");
        }
        if (pedidoMinimo < 0 || valorFreteGratis < 0) {
            throw new RegraNegocioException("Valores de pedido mínimo e frete grátis não podem ser negativos.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.categoria = categoria;
        this.localizacao = localizacao;
        this.tempoPreparoMinutos = tempoPreparoMinutos;
        this.pedidoMinimo = pedidoMinimo;
        this.valorFreteGratis = valorFreteGratis;
        this.aberto = true;
        this.ativo = true;
        this.cardapio = new ArrayList<>();
        this.pedidos = new ArrayList<>();
    }

    // ---------------- Cardápio (associação Restaurante -> Produto) ----------------

    public void adicionarProduto(Produto produto) {
        for (Produto p : cardapio) {
            if (p.getNome().equalsIgnoreCase(produto.getNome())) {
                throw new RegraNegocioException("O restaurante já possui um produto chamado " + produto.getNome() + ".");
            }
        }
        cardapio.add(produto);
    }

    public boolean possuiProduto(Produto produto) {
        return cardapio.contains(produto);
    }

    public Produto buscarProduto(int produtoId) {
        for (Produto p : cardapio) {
            if (p.getId() == produtoId) {
                return p;
            }
        }
        throw new RegraNegocioException("Produto #" + produtoId + " não encontrado no cardápio de " + nome + ".");
    }

    public ArrayList<Produto> listarProdutosDisponiveis() {
        ArrayList<Produto> disponiveis = new ArrayList<>();
        for (Produto p : cardapio) {
            if (p.estaDisponivel()) {
                disponiveis.add(p);
            }
        }
        return disponiveis;
    }

    // ---------------- RN02 - Só aceita pedido se aberto e ativo ----------------

    public void validarRecebimentoPedido() {
        if (!ativo) {
            throw new RegraNegocioException("O restaurante " + nome + " está desativado.");
        }
        if (!aberto) {
            throw new RegraNegocioException("O restaurante " + nome + " está fechado no momento.");
        }
    }

    // ---------------- RN04 - Frete e área de entrega ----------------

    public double distanciaAte(Localizacao destino) {
        return localizacao.distanciaAte(destino);
    }

    public boolean atendeEndereco(Localizacao destino) {
        return distanciaAte(destino) <= RAIO_MAXIMO_KM;
    }

    /**
     * Frete = taxa base + R$ por km.
     * Se o subtotal atingir o valor de frete grátis do restaurante, o frete é zero.
     */
    public double calcularFrete(Localizacao destino, double subtotal) {
        if (!atendeEndereco(destino)) {
            throw new RegraNegocioException(String.format(
                    "Endereço fora da área de entrega (%.1f km; máximo %.1f km).",
                    distanciaAte(destino), RAIO_MAXIMO_KM));
        }
        if (valorFreteGratis > 0 && subtotal >= valorFreteGratis) {
            return 0.0;
        }
        return Moeda.arredondar(TAXA_ENTREGA_BASE + VALOR_POR_KM * distanciaAte(destino));
    }

    // ---------------- RN09 - Prazo estimado ----------------

    /** Prazo = tempo de preparo + 3 minutos por km (arredondado para cima). */
    public int calcularPrazoEstimado(Localizacao destino) {
        int deslocamento = (int) Math.ceil(distanciaAte(destino) * MINUTOS_POR_KM);
        return tempoPreparoMinutos + deslocamento;
    }

    // ---------------- Pedidos e avaliação ----------------

    public void adicionarPedido(Pedido pedido) {
        pedidos.add(pedido);
    }

    public void removerPedido(Pedido pedido) {
        pedidos.remove(pedido);
    }

    public boolean possuiPedidosEmAndamento() {
        for (Pedido p : pedidos) {
            if (p.getStatus().emAndamento()) {
                return true;
            }
        }
        return false;
    }

    /** RN10 - chamada pelo Pedido quando o cliente avalia. */
    public void registrarAvaliacao(int nota) {
        somaNotas += nota;
        quantidadeAvaliacoes++;
    }

    public double getNotaMedia() {
        if (quantidadeAvaliacoes == 0) {
            return 0.0;
        }
        return (double) somaNotas / quantidadeAvaliacoes;
    }

    /** Soma do que o restaurante efetivamente recebeu (pedidos entregues + multas de cancelamento). */
    public double calcularFaturamento() {
        double total = 0;
        for (Pedido p : pedidos) {
            total += p.getValorFaturado();
        }
        return Moeda.arredondar(total);
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

    // ---------------- Abrir / fechar / desativar ----------------

    public void abrir() {
        if (!ativo) {
            throw new RegraNegocioException("Restaurante desativado não pode ser aberto.");
        }
        aberto = true;
    }

    public void fechar() {
        aberto = false;
    }

    public void desativar() {
        if (possuiPedidosEmAndamento()) {
            throw new RegraNegocioException("Não é possível desativar: há pedidos em andamento.");
        }
        ativo = false;
        aberto = false;
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
            throw new RegraNegocioException("Nome do restaurante é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public int getTempoPreparoMinutos() {
        return tempoPreparoMinutos;
    }

    public void setTempoPreparoMinutos(int tempoPreparoMinutos) {
        if (tempoPreparoMinutos <= 0) {
            throw new RegraNegocioException("Tempo de preparo deve ser maior que zero.");
        }
        this.tempoPreparoMinutos = tempoPreparoMinutos;
    }

    public double getPedidoMinimo() {
        return pedidoMinimo;
    }

    public void setPedidoMinimo(double pedidoMinimo) {
        if (pedidoMinimo < 0) {
            throw new RegraNegocioException("Pedido mínimo não pode ser negativo.");
        }
        this.pedidoMinimo = pedidoMinimo;
    }

    public double getValorFreteGratis() {
        return valorFreteGratis;
    }

    public void setValorFreteGratis(double valorFreteGratis) {
        if (valorFreteGratis < 0) {
            throw new RegraNegocioException("Valor de frete grátis não pode ser negativo.");
        }
        this.valorFreteGratis = valorFreteGratis;
    }

    public boolean isAberto() {
        return aberto;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public int getQuantidadeAvaliacoes() {
        return quantidadeAvaliacoes;
    }

    /** Devolve uma CÓPIA da lista para que outras classes não alterem o cardápio por fora. */
    public ArrayList<Produto> getCardapio() {
        return new ArrayList<>(cardapio);
    }

    public ArrayList<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    @Override
    public String toString() {
        String situacao = !ativo ? "DESATIVADO" : (aberto ? "ABERTO" : "FECHADO");
        String nota = quantidadeAvaliacoes == 0 ? "sem avaliações"
                : String.format("nota %.1f (%d)", getNotaMedia(), quantidadeAvaliacoes);
        return String.format("#%d %-22s %-12s %-10s %s | mínimo %s | %d produtos",
                id, nome, categoria, situacao, nota, Moeda.formatar(pedidoMinimo), cardapio.size());
    }
}
