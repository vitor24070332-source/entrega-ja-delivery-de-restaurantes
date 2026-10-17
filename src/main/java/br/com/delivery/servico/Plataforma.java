package br.com.delivery.servico;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.Cupom;
import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.Produto;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.modelo.StatusPedido;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * "Banco de dados em memória" + ponto de entrada das operações.
 *
 * A Plataforma guarda as listas principais, gera os IDs, busca objetos e faz as
 * ASSOCIAÇÕES entre eles (ex.: liga um pedido ao cliente e ao restaurante).
 * As regras de negócio em si ficam nas classes do pacote modelo:
 * aqui só se delega (ex.: despacharPedido escolhe o entregador e chama pedido.despachar).
 */
public class Plataforma {

    private ArrayList<Restaurante> restaurantes = new ArrayList<>();
    private ArrayList<Cliente> clientes = new ArrayList<>();
    private ArrayList<Entregador> entregadores = new ArrayList<>();
    private ArrayList<Cupom> cupons = new ArrayList<>();
    private ArrayList<Pedido> pedidos = new ArrayList<>();

    private int proximoIdRestaurante = 1;
    private int proximoIdProduto = 1;
    private int proximoIdCliente = 1;
    private int proximoIdEntregador = 1;
    private int proximoIdPedido = 1;

    // =====================================================================
    // Restaurantes
    // =====================================================================

    public Restaurante cadastrarRestaurante(String nome, String categoria, Localizacao localizacao,
                                           int tempoPreparo, double pedidoMinimo, double valorFreteGratis) {
        Restaurante r = new Restaurante(proximoIdRestaurante, nome, categoria, localizacao,
                tempoPreparo, pedidoMinimo, valorFreteGratis);
        restaurantes.add(r);
        proximoIdRestaurante++;
        return r;
    }

    public Restaurante buscarRestaurante(int id) {
        for (Restaurante r : restaurantes) {
            if (r.getId() == id) {
                return r;
            }
        }
        throw new RegraNegocioException("Restaurante #" + id + " não encontrado.");
    }

    public ArrayList<Restaurante> listarRestaurantes() {
        return new ArrayList<>(restaurantes);
    }

    public ArrayList<Restaurante> pesquisarRestaurantesPorCategoria(String categoria) {
        ArrayList<Restaurante> encontrados = new ArrayList<>();
        for (Restaurante r : restaurantes) {
            if (r.getCategoria().toLowerCase().contains(categoria.toLowerCase())) {
                encontrados.add(r);
            }
        }
        return encontrados;
    }

    /** Ordena do melhor avaliado para o pior; empate -> ordem alfabética. */
    public ArrayList<Restaurante> listarRestaurantesPorNota() {
        ArrayList<Restaurante> ordenados = new ArrayList<>(restaurantes);
        ordenados.sort(Comparator.comparingDouble(Restaurante::getNotaMedia).reversed()
                .thenComparing(Restaurante::getNome));
        return ordenados;
    }

    // =====================================================================
    // Produtos
    // =====================================================================

    /** Cria o produto e o ASSOCIA ao cardápio do restaurante. */
    public Produto cadastrarProduto(int restauranteId, String nome, String categoria, double preco, int estoque) {
        Restaurante r = buscarRestaurante(restauranteId);
        Produto p = new Produto(proximoIdProduto, nome, categoria, preco, estoque);
        r.adicionarProduto(p);
        proximoIdProduto++;
        return p;
    }

    /** Procura em todos os cardápios. */
    public Produto buscarProduto(int produtoId) {
        for (Restaurante r : restaurantes) {
            for (Produto p : r.getCardapio()) {
                if (p.getId() == produtoId) {
                    return p;
                }
            }
        }
        throw new RegraNegocioException("Produto #" + produtoId + " não encontrado.");
    }

    public Restaurante buscarRestauranteDoProduto(Produto produto) {
        for (Restaurante r : restaurantes) {
            if (r.possuiProduto(produto)) {
                return r;
            }
        }
        throw new RegraNegocioException("Produto sem restaurante.");
    }

    public ArrayList<Produto> pesquisarProdutosPorNome(String trecho) {
        ArrayList<Produto> encontrados = new ArrayList<>();
        for (Restaurante r : restaurantes) {
            for (Produto p : r.getCardapio()) {
                if (p.getNome().toLowerCase().contains(trecho.toLowerCase())) {
                    encontrados.add(p);
                }
            }
        }
        return encontrados;
    }

    public ArrayList<Produto> listarCardapioPorPreco(int restauranteId, boolean crescente) {
        ArrayList<Produto> ordenados = buscarRestaurante(restauranteId).getCardapio();
        Comparator<Produto> porPreco = Comparator.comparingDouble(Produto::getPreco);
        ordenados.sort(crescente ? porPreco : porPreco.reversed());
        return ordenados;
    }

    // =====================================================================
    // Clientes
    // =====================================================================

    public Cliente cadastrarCliente(String nome, String telefone, Localizacao localizacao) {
        Cliente c = new Cliente(proximoIdCliente, nome, telefone, localizacao);
        clientes.add(c);
        proximoIdCliente++;
        return c;
    }

    public Cliente buscarCliente(int id) {
        for (Cliente c : clientes) {
            if (c.getId() == id) {
                return c;
            }
        }
        throw new RegraNegocioException("Cliente #" + id + " não encontrado.");
    }

    public ArrayList<Cliente> listarClientes() {
        return new ArrayList<>(clientes);
    }

    public ArrayList<Cliente> pesquisarClientesPorNome(String trecho) {
        ArrayList<Cliente> encontrados = new ArrayList<>();
        for (Cliente c : clientes) {
            if (c.getNome().toLowerCase().contains(trecho.toLowerCase())) {
                encontrados.add(c);
            }
        }
        return encontrados;
    }

    public ArrayList<Cliente> listarClientesPorNome() {
        ArrayList<Cliente> ordenados = new ArrayList<>(clientes);
        ordenados.sort(Comparator.comparing(Cliente::getNome, String.CASE_INSENSITIVE_ORDER));
        return ordenados;
    }

    // =====================================================================
    // Entregadores
    // =====================================================================

    public Entregador cadastrarEntregador(String nome, String veiculo) {
        Entregador e = new Entregador(proximoIdEntregador, nome, veiculo);
        entregadores.add(e);
        proximoIdEntregador++;
        return e;
    }

    public Entregador buscarEntregador(int id) {
        for (Entregador e : entregadores) {
            if (e.getId() == id) {
                return e;
            }
        }
        throw new RegraNegocioException("Entregador #" + id + " não encontrado.");
    }

    public ArrayList<Entregador> listarEntregadores() {
        return new ArrayList<>(entregadores);
    }

    /**
     * RN08 - Distribuição automática: entre os entregadores DISPONÍVEIS,
     * escolhe o que tem MENOS entregas concluídas (divide o trabalho de forma justa).
     */
    public Entregador escolherEntregadorDisponivel() {
        Entregador escolhido = null;
        for (Entregador e : entregadores) {
            if (e.estaDisponivel()) {
                if (escolhido == null
                        || e.quantidadeEntregasConcluidas() < escolhido.quantidadeEntregasConcluidas()) {
                    escolhido = e;
                }
            }
        }
        if (escolhido == null) {
            throw new RegraNegocioException("Nenhum entregador disponível no momento.");
        }
        return escolhido;
    }

    // =====================================================================
    // Cupons
    // =====================================================================

    public Cupom cadastrarCupom(String codigo, double percentual, double descontoMaximo,
                                double valorMinimo, LocalDate validade, int limiteUsos) {
        for (Cupom c : cupons) {
            if (c.getCodigo().equalsIgnoreCase(codigo.trim())) {
                throw new RegraNegocioException("Já existe um cupom com o código " + codigo + ".");
            }
        }
        Cupom cupom = new Cupom(codigo, percentual, descontoMaximo, valorMinimo, validade, limiteUsos);
        cupons.add(cupom);
        return cupom;
    }

    public Cupom buscarCupom(String codigo) {
        for (Cupom c : cupons) {
            if (c.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return c;
            }
        }
        throw new RegraNegocioException("Cupom " + codigo + " não existe.");
    }

    public ArrayList<Cupom> listarCupons() {
        return new ArrayList<>(cupons);
    }

    // =====================================================================
    // Pedidos (funcionalidade principal)
    // =====================================================================

    /** Cria o pedido e o ASSOCIA ao cliente e ao restaurante. */
    public Pedido criarPedido(int clienteId, int restauranteId) {
        Cliente cliente = buscarCliente(clienteId);
        Restaurante restaurante = buscarRestaurante(restauranteId);
        Pedido pedido = new Pedido(proximoIdPedido, cliente, restaurante);
        proximoIdPedido++;
        pedidos.add(pedido);
        cliente.adicionarPedido(pedido);
        restaurante.adicionarPedido(pedido);
        return pedido;
    }

    /** Remove um carrinho que nunca foi confirmado (desfaz as associações). */
    public void descartarPedido(int pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new RegraNegocioException("Só pedidos em aberto podem ser descartados. Use cancelar.");
        }
        pedidos.remove(pedido);
        pedido.getCliente().removerPedido(pedido);
        pedido.getRestaurante().removerPedido(pedido);
    }

    /** Escolhe automaticamente o entregador e despacha o pedido. */
    public Entregador despacharPedido(int pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        Entregador entregador = escolherEntregadorDisponivel();
        pedido.despachar(entregador);
        return entregador;
    }

    public Pedido buscarPedido(int id) {
        for (Pedido p : pedidos) {
            if (p.getId() == id) {
                return p;
            }
        }
        throw new RegraNegocioException("Pedido #" + id + " não encontrado.");
    }

    public ArrayList<Pedido> listarPedidos() {
        return new ArrayList<>(pedidos);
    }

    public ArrayList<Pedido> listarPedidosPorStatus(StatusPedido status) {
        ArrayList<Pedido> encontrados = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (p.getStatus() == status) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    public ArrayList<Pedido> listarPedidosPorValor() {
        ArrayList<Pedido> ordenados = new ArrayList<>(pedidos);
        ordenados.sort(Comparator.comparingDouble(Pedido::getTotal).reversed());
        return ordenados;
    }
}
