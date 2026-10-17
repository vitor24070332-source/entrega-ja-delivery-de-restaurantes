package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cliente;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.NivelFidelidade;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.util.Moeda;

public class MenuClientes {

    private Plataforma plataforma;
    private Console console;

    public MenuClientes(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("CLIENTES");
            console.mensagem("1 - Cadastrar cliente");
            console.mensagem("2 - Listar clientes (ordem alfabética)");
            console.mensagem("3 - Consultar cliente (histórico e fidelidade)");
            console.mensagem("4 - Editar cliente");
            console.mensagem("5 - Desativar / reativar cliente");
            console.mensagem("6 - Pesquisar cliente por nome");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> console.listar(plataforma.listarClientesPorNome(), "Nenhum cliente cadastrado.");
                    case 3 -> consultar();
                    case 4 -> editar();
                    case 5 -> desativarOuReativar();
                    case 6 -> console.listar(plataforma.pesquisarClientesPorNome(console.lerTexto("Parte do nome")),
                            "Nenhum cliente encontrado.");
                    default -> console.erro("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                console.erro(e.getMessage());
            } catch (NumberFormatException e) {
                console.erro("Número inválido.");
            }
        }
    }

    private void cadastrar() {
        String nome = console.lerTexto("Nome");
        String telefone = console.lerTexto("Telefone");
        Localizacao local = lerLocalizacao();
        Cliente c = plataforma.cadastrarCliente(nome, telefone, local);
        console.sucesso("Cliente cadastrado com id #" + c.getId());
    }

    private Localizacao lerLocalizacao() {
        String endereco = console.lerTexto("Endereço");
        double x = console.lerDecimal("Coordenada X (km)");
        double y = console.lerDecimal("Coordenada Y (km)");
        return new Localizacao(endereco, x, y);
    }

    private void consultar() {
        Cliente c = plataforma.buscarCliente(console.lerInteiro("Id do cliente"));
        console.mensagem(c.toString());
        NivelFidelidade nivel = c.getNivelFidelidade();
        console.mensagem("  Nível " + nivel + ": " + nivel.getPercentualDesconto() + "% de desconto automático");
        console.mensagem("  Total gasto: " + Moeda.formatar(c.calcularTotalGasto()));
        console.mensagem("  Histórico de pedidos:");
        console.listar(c.getPedidos(), "Nenhum pedido.");
    }

    private void editar() {
        Cliente c = plataforma.buscarCliente(console.lerInteiro("Id do cliente"));
        console.mensagem("Deixe em branco para manter o valor atual.");
        String nome = console.lerTextoOpcional("Nome [" + c.getNome() + "]");
        if (!nome.isEmpty()) {
            c.setNome(nome);
        }
        String telefone = console.lerTextoOpcional("Telefone [" + c.getTelefone() + "]");
        if (!telefone.isEmpty()) {
            c.setTelefone(telefone);
        }
        if (console.confirmar("Alterar endereço? (atual: " + c.getLocalizacao() + ")")) {
            c.alterarEndereco(lerLocalizacao());
        }
        console.sucesso("Cliente atualizado: " + c);
    }

    private void desativarOuReativar() {
        Cliente c = plataforma.buscarCliente(console.lerInteiro("Id do cliente"));
        if (c.isAtivo()) {
            c.desativar();
            console.sucesso(c.getNome() + " foi desativado.");
        } else {
            c.ativar();
            console.sucesso(c.getNome() + " foi reativado.");
        }
    }
}
