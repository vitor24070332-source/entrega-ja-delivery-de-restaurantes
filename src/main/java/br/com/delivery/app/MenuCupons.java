package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Cupom;
import br.com.delivery.servico.Plataforma;

import java.time.LocalDate;

public class MenuCupons {

    private Plataforma plataforma;
    private Console console;

    public MenuCupons(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("CUPONS");
            console.mensagem("1 - Cadastrar cupom");
            console.mensagem("2 - Listar cupons");
            console.mensagem("3 - Desativar cupom");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> console.listar(plataforma.listarCupons(), "Nenhum cupom cadastrado.");
                    case 3 -> desativar();
                    default -> console.erro("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                console.erro(e.getMessage());
            }
        }
    }

    private void cadastrar() {
        String codigo = console.lerTexto("Código (ex.: BEMVINDO)");
        double percentual = console.lerDecimal("Desconto (%)");
        double maximo = console.lerDecimal("Desconto máximo (R$)");
        double minimo = console.lerDecimal("Valor mínimo do pedido (R$)");
        int dias = console.lerInteiro("Válido por quantos dias a partir de hoje");
        int limite = console.lerInteiro("Limite de usos");
        Cupom c = plataforma.cadastrarCupom(codigo, percentual, maximo, minimo, LocalDate.now().plusDays(dias), limite);
        console.sucesso("Cupom criado: " + c);
    }

    private void desativar() {
        Cupom c = plataforma.buscarCupom(console.lerTexto("Código"));
        c.desativar();
        console.sucesso("Cupom " + c.getCodigo() + " desativado.");
    }
}
