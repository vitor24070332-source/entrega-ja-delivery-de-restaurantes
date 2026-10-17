package br.com.delivery.app;

import br.com.delivery.excecao.RegraNegocioException;
import br.com.delivery.modelo.Localizacao;
import br.com.delivery.modelo.Restaurante;
import br.com.delivery.servico.Plataforma;
import br.com.delivery.util.Moeda;

public class MenuRestaurantes {

    private Plataforma plataforma;
    private Console console;

    public MenuRestaurantes(Plataforma plataforma, Console console) {
        this.plataforma = plataforma;
        this.console = console;
    }

    public void exibir() {
        while (true) {
            console.titulo("RESTAURANTES");
            console.mensagem("1 - Cadastrar restaurante");
            console.mensagem("2 - Listar restaurantes");
            console.mensagem("3 - Consultar restaurante (com cardápio)");
            console.mensagem("4 - Editar restaurante");
            console.mensagem("5 - Abrir / fechar restaurante");
            console.mensagem("6 - Desativar restaurante");
            console.mensagem("7 - Pesquisar por categoria");
            console.mensagem("8 - Listar por nota (melhores primeiro)");
            console.mensagem("0 - Voltar");
            int opcao = console.lerInteiro("Opção");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> console.listar(plataforma.listarRestaurantes(), "Nenhum restaurante cadastrado.");
                    case 3 -> consultar();
                    case 4 -> editar();
                    case 5 -> abrirOuFechar();
                    case 6 -> desativar();
                    case 7 -> pesquisarPorCategoria();
                    case 8 -> console.listar(plataforma.listarRestaurantesPorNota(), "Nenhum restaurante cadastrado.");
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
        String categoria = console.lerTexto("Categoria (ex.: Pizza, Lanches, Brasileira)");
        String endereco = console.lerTexto("Endereço");
        double x = console.lerDecimal("Coordenada X (km)");
        double y = console.lerDecimal("Coordenada Y (km)");
        int preparo = console.lerInteiro("Tempo médio de preparo (min)");
        double minimo = console.lerDecimal("Pedido mínimo (R$)");
        double freteGratis = console.lerDecimal("Frete grátis a partir de (R$, 0 = nunca)");
        Restaurante r = plataforma.cadastrarRestaurante(nome, categoria, new Localizacao(endereco, x, y),
                preparo, minimo, freteGratis);
        console.sucesso("Restaurante cadastrado com id #" + r.getId());
    }

    private void consultar() {
        Restaurante r = plataforma.buscarRestaurante(console.lerInteiro("Id do restaurante"));
        console.mensagem(r.toString());
        console.mensagem("  Endereço: " + r.getLocalizacao());
        console.mensagem("  Preparo médio: " + r.getTempoPreparoMinutos() + " min");
        console.mensagem("  Frete grátis a partir de: "
                + (r.getValorFreteGratis() == 0 ? "não oferece" : Moeda.formatar(r.getValorFreteGratis())));
        console.mensagem("  Faturamento: " + Moeda.formatar(r.calcularFaturamento())
                + " | pedidos entregues: " + r.quantidadePedidosEntregues());
        console.mensagem("  Cardápio:");
        console.listar(r.getCardapio(), "Cardápio vazio.");
    }

    private void editar() {
        Restaurante r = plataforma.buscarRestaurante(console.lerInteiro("Id do restaurante"));
        console.mensagem("Deixe em branco para manter o valor atual.");
        String nome = console.lerTextoOpcional("Nome [" + r.getNome() + "]");
        if (!nome.isEmpty()) {
            r.setNome(nome);
        }
        String categoria = console.lerTextoOpcional("Categoria [" + r.getCategoria() + "]");
        if (!categoria.isEmpty()) {
            r.setCategoria(categoria);
        }
        String preparo = console.lerTextoOpcional("Tempo de preparo [" + r.getTempoPreparoMinutos() + "]");
        if (!preparo.isEmpty()) {
            r.setTempoPreparoMinutos(Integer.parseInt(preparo));
        }
        String minimo = console.lerTextoOpcional("Pedido mínimo [" + r.getPedidoMinimo() + "]");
        if (!minimo.isEmpty()) {
            r.setPedidoMinimo(Double.parseDouble(minimo.replace(',', '.')));
        }
        String freteGratis = console.lerTextoOpcional("Frete grátis a partir de [" + r.getValorFreteGratis() + "]");
        if (!freteGratis.isEmpty()) {
            r.setValorFreteGratis(Double.parseDouble(freteGratis.replace(',', '.')));
        }
        console.sucesso("Restaurante atualizado: " + r);
    }

    private void abrirOuFechar() {
        Restaurante r = plataforma.buscarRestaurante(console.lerInteiro("Id do restaurante"));
        if (r.isAberto()) {
            r.fechar();
            console.sucesso(r.getNome() + " agora está FECHADO.");
        } else {
            r.abrir();
            console.sucesso(r.getNome() + " agora está ABERTO.");
        }
    }

    private void desativar() {
        Restaurante r = plataforma.buscarRestaurante(console.lerInteiro("Id do restaurante"));
        if (console.confirmar("Desativar " + r.getNome() + "?")) {
            r.desativar();
            console.sucesso("Restaurante desativado.");
        }
    }

    private void pesquisarPorCategoria() {
        String categoria = console.lerTexto("Categoria");
        console.listar(plataforma.pesquisarRestaurantesPorCategoria(categoria), "Nenhum restaurante encontrado.");
    }
}
