package modelador.dominio.er;

/**
 * Representa una posición en el plano del diagrama, usada por los
 * elementos del modelo Entidad-Relación.
 */
public class Punto {

    private double x;
    private double y;

    public Punto(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Devuelve un nuevo punto desplazado respecto del actual.
     */
    public Punto desplazado(double dx, double dy) {
        return new Punto(x + dx, y + dy);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
