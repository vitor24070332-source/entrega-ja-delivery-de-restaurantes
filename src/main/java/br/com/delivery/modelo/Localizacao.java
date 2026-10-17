package br.com.delivery.modelo;

/**
 * Endereço de um cliente ou restaurante.
 *
 * Para não depender de GPS/API de mapas, a cidade é tratada como um plano cartesiano
 * em quilômetros: cada endereço tem uma coordenada (x, y). A distância entre dois pontos
 * é calculada em linha reta (teorema de Pitágoras).
 */
public class Localizacao {

    private String endereco;
    private double x;
    private double y;

    public Localizacao(String endereco, double x, double y) {
        this.endereco = endereco;
        this.x = x;
        this.y = y;
    }

    /** Distância em km até outra localização. */
    public double distanciaAte(Localizacao outra) {
        double dx = this.x - outra.x;
        double dy = this.y - outra.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public String getEndereco() {
        return endereco;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return endereco + " (" + x + ", " + y + ")";
    }
}
