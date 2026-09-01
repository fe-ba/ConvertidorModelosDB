import java.awt.Rectangle;

// Una tabla colocada en el lienzo relacional. La posicion no vive en el modelo
// relacional (se regenera en cada conversion), asi que se calcula aqui.
public class FiguraTabla {

    public static final int ANCHO = 250;
    public static final int ALTO_CABECERA = 30;
    public static final int ALTO_FILA = 21;
    private static final int MARGEN_INFERIOR = 8;

    private final Tabla tabla;
    private int x;
    private int y;

    public FiguraTabla(Tabla tabla, int x, int y) {
        this.tabla = tabla;
        this.x = x;
        this.y = y;
    }

    public Tabla getTabla() {
        return tabla;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void mover(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getAlto() {
        return ALTO_CABECERA + tabla.getColumnas().size() * ALTO_FILA + MARGEN_INFERIOR;
    }

    public Rectangle limites() {
        return new Rectangle(x - ANCHO / 2, y - getAlto() / 2, ANCHO, getAlto());
    }

    // Corte de la recta hacia otro punto con el borde de la caja, para apoyar
    // ahi las lineas de clave foranea.
    public java.awt.Point borde(int haciaX, int haciaY) {
        double dx = haciaX - x;
        double dy = haciaY - y;
        if (dx == 0 && dy == 0) {
            return new java.awt.Point(x, y);
        }
        double mitadAncho = ANCHO / 2.0;
        double mitadAlto = getAlto() / 2.0;
        double escalaX = Math.abs(dx) < 0.001
                ? Double.MAX_VALUE : mitadAncho / Math.abs(dx);
        double escalaY = Math.abs(dy) < 0.001
                ? Double.MAX_VALUE : mitadAlto / Math.abs(dy);
        double escala = Math.min(escalaX, escalaY);
        return new java.awt.Point((int) (x + dx * escala), (int) (y + dy * escala));
    }
}
