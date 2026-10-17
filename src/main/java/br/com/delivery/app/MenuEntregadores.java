package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Entregador;
import br.com.delivery.modelo.Pedido;
import br.com.delivery.modelo.StatusEntregador;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.util.Moeda;

public class MenuEntregadores {

    private Plataforma plataforma;
    private Console console;

    public MenuEntregadores(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("ENTREGADORES");
            console.mensagem("1 - Cadastrar entregador");
            console.mensagem("2 - Listar entregadores");
            console.mensagem("3 - Consultar entregador (entregas e comissões)");
            console.mensagem("4 - Editar entregador");
            console.mensagem("5 - Desativar / reativar entregador");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> console.listar(plataforma.listarEntregadores(), "Nenhum entregador cadastrado.");
                    case 3 -> consultar();
                    case 4 -> editar();
                    case 5 -> desativarOuReativar();
                    default -> console.erro("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                console.erro(e.getMessage());
            }
        }
    }

    private void cadastrar() {
        String nome = console.lerTexto("Nome");
        String veiculo = console.lerTexto("Veículo (Moto, Bicicleta, Carro)");
        Entregador e = plataforma.cadastrarEntregador(nome, veiculo);
        console.sucesso("Entregador cadastrado com id #" + e.getId());
    }

    private void consultar() {
        Entregador e = plataforma.buscarEntregador(console.lerInteiro("Id do entregador"));
        console.mensagem(e.toString());
        console.mensagem("  Entregas no prazo: " + e.quantidadeEntregasNoPrazo() + " de "
                + e.quantidadeEntregasConcluidas());
        console.mensagem("  Entregas:");
        if (e.getEntregas().isEmpty()) {
            console.mensagem("  Nenhuma entrega.");
        }
        for (Pedido p : e.getEntregas()) {
            console.mensagem("  " + p + " | comissão " + Moeda.formatar(e.calcularComissao(p)));
        }
    }

    private void editar() {
        Entregador e = plataforma.buscarEntregador(console.lerInteiro("Id do entregador"));
        console.mensagem("Deixe em branco para manter o valor atual.");
        String nome = console.lerTextoOpcional("Nome [" + e.getNome() + "]");
        if (!nome.isEmpty()) {
            e.setNome(nome);
        }
        String veiculo = console.lerTextoOpcional("Veículo [" + e.getVeiculo() + "]");
        if (!veiculo.isEmpty()) {
            e.setVeiculo(veiculo);
        }
        console.sucesso("Entregador atualizado: " + e);
    }

    private void desativarOuReativar() {
        Entregador e = plataforma.buscarEntregador(console.lerInteiro("Id do entregador"));
        if (e.getStatus() == StatusEntregador.INATIVO) {
            e.ativar();
            console.sucesso(e.getNome() + " está disponível novamente.");
        } else {
            e.desativar();
            console.sucesso(e.getNome() + " foi desativado.");
        }
    }
}
