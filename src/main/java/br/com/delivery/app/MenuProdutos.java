package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.io.ImportadorCsv;
import br.com.delivery.io.ResultadoImportacao;
import br.com.delivery.modelo.Produto;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.servico.Plataforma;

import java.io.IOException;
import java.nio.file.Path;

public class MenuProdutos {

    private Plataforma plataforma;
    private Console console;

    public MenuProdutos(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("CARDÁPIO / PRODUTOS");
            console.mensagem("1 - Cadastrar produto em um restaurante");
            console.mensagem("2 - Listar cardápio ordenado por preço");
            console.mensagem("3 - Consultar produto");
            console.mensagem("4 - Alterar preço");
            console.mensagem("5 - Repor estoque");
            console.mensagem("6 - Desativar / reativar produto");
            console.mensagem("7 - Pesquisar produto por nome");
            console.mensagem("8 - Importar produtos de arquivo CSV");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> listarPorPreco();
                    case 3 -> consultar();
                    case 4 -> alterarPreco();
                    case 5 -> reporEstoque();
                    case 6 -> desativarOuReativar();
                    case 7 -> pesquisar();
                    case 8 -> importarCsv();
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
        int restauranteId = console.lerInteiro("Id do restaurante");
        Restaurante r = plataforma.buscarRestaurante(restauranteId);
        console.mensagem("Restaurante: " + r.getNome());
        String nome = console.lerTexto("Nome do produto");
        String categoria = console.lerTexto("Categoria (ex.: Prato, Bebida, Sobremesa)");
        double preco = console.lerDecimal("Preço (R$)");
        int estoque = console.lerInteiro("Estoque inicial");
        Produto p = plataforma.cadastrarProduto(restauranteId, nome, categoria, preco, estoque);
        console.sucesso("Produto cadastrado com id #" + p.getId());
    }

    private void listarPorPreco() {
        int restauranteId = console.lerInteiro("Id do restaurante");
        boolean crescente = console.confirmar("Do mais barato para o mais caro?");
        console.listar(plataforma.listarCardapioPorPreco(restauranteId, crescente), "Cardápio vazio.");
    }

    private void consultar() {
        Produto p = plataforma.buscarProduto(console.lerInteiro("Id do produto"));
        console.mensagem(p.toString());
        console.mensagem("  Restaurante: " + plataforma.buscarRestauranteDoProduto(p).getNome());
        console.mensagem("  Disponível para venda: " + (p.estaDisponivel() ? "sim" : "não"));
    }

    private void alterarPreco() {
        Produto p = plataforma.buscarProduto(console.lerInteiro("Id do produto"));
        console.mensagem(p.toString());
        p.alterarPreco(console.lerDecimal("Novo preço (R$)"));
        console.sucesso("Preço atualizado. Pedidos já feitos mantêm o preço antigo.");
    }

    private void reporEstoque() {
        Produto p = plataforma.buscarProduto(console.lerInteiro("Id do produto"));
        console.mensagem(p.toString());
        p.reporEstoque(console.lerInteiro("Quantidade que chegou"));
        console.sucesso("Estoque atual: " + p.getEstoque());
    }

    private void desativarOuReativar() {
        Produto p = plataforma.buscarProduto(console.lerInteiro("Id do produto"));
        if (p.isAtivo()) {
            p.desativar();
            console.sucesso(p.getNome() + " foi desativado (não aparece mais para venda).");
        } else {
            p.ativar();
            console.sucesso(p.getNome() + " foi reativado.");
        }
    }

    private void pesquisar() {
        String trecho = console.lerTexto("Parte do nome");
        for (Produto p : plataforma.pesquisarProdutosPorNome(trecho)) {
            console.mensagem("  " + p + "  <- " + plataforma.buscarRestauranteDoProduto(p).getNome());
        }
    }

    private void importarCsv() {
        console.mensagem("Formato: restaurante_id;nome;categoria;preco;estoque (com cabeçalho)");
        String caminho = console.lerTextoOpcional("Caminho do arquivo [dados/cardapio_importacao.csv]");
        if (caminho.isEmpty()) {
            caminho = "dados/cardapio_importacao.csv";
        }
        try {
            ResultadoImportacao resultado = new ImportadorCsv().importarCardapio(plataforma, Path.of(caminho));
            console.sucesso(resultado.getImportados() + " produto(s) importado(s).");
            for (String erro : resultado.getErros()) {
                console.erro(erro);
            }
        } catch (IOException e) {
            console.erro("Não foi possível ler o arquivo: " + e.getMessage());
        }
    }
}
