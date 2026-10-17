package br.com.delivery.app;

import br.com.delivery.servico.Plataforma;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Ponto de entrada do sistema. Só monta os objetos e mostra o menu principal;
 * cada área tem sua própria classe de menu, e as regras ficam no pacote modelo.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Console console = new Console(scanner);
        Plataforma plataforma = new Plataforma();

        console.titulo("ENTREGA JÁ - Sistema de Delivery");
        if (console.confirmar("Carregar dados de exemplo?")) {
            DadosIniciais.carregar(plataforma);
            console.sucesso("Dados de exemplo carregados.");
        }

        MenuRestaurantes menuRestaurantes = new MenuRestaurantes(plataforma, console);
        MenuProdutos menuProdutos = new MenuProdutos(plataforma, console);
        MenuClientes menuClientes = new MenuClientes(plataforma, console);
        MenuEntregadores menuEntregadores = new MenuEntregadores(plataforma, console);
        MenuCupons menuCupons = new MenuCupons(plataforma, console);
        MenuPedidos menuPedidos = new MenuPedidos(plataforma, console);
        MenuRelatorios menuRelatorios = new MenuRelatorios(plataforma, console);

        try {
            while (true) {
                console.titulo("MENU PRINCIPAL");
                console.mensagem("1 - Restaurantes");
                console.mensagem("2 - Cardápio / Produtos");
                console.mensagem("3 - Clientes");
                console.mensagem("4 - Entregadores");
                console.mensagem("5 - Cupons");
                console.mensagem("6 - Pedidos");
                console.mensagem("7 - Relatórios e exportação CSV");
                console.mensagem("0 - Sair");
                int opcao = console.lerInteiro("Opção");
                switch (opcao) {
                    case 0 -> {
                        console.mensagem("Até logo!");
                        return;
                    }
                    case 1 -> menuRestaurantes.exibir();
                    case 2 -> menuProdutos.exibir();
                    case 3 -> menuClientes.exibir();
                    case 4 -> menuEntregadores.exibir();
                    case 5 -> menuCupons.exibir();
                    case 6 -> menuPedidos.exibir();
                    case 7 -> menuRelatorios.exibir();
                    default -> console.erro("Opção inválida.");
                }
            }
        } catch (NoSuchElementException e) {
            // entrada encerrada (Ctrl+D / fim do arquivo de entrada)
            console.mensagem("Entrada encerrada. Até logo!");
        }
    }
}
