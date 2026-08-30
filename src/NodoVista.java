import java.awt.Rectangle;
import java.awt.Shape;

// TEMPORAL: luego envuelve a ElementoDelModelo y lee nombre y la posicion.
public class NodoVista {

    private static final int ALTO_ATRIBUTO = 34;
    private static final int ALTO_RELACION = 72;
    private static final int ALTO_ENTIDAD = 56;

    private TipoNodo tipo;
    private String nombre;
    private int x;
    private int y;
    // Solo para atributos: nodo del que cuelgan.
    private NodoVista padre;

    public NodoVista(TipoNodo tipo, String nombre, int x, int y) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.padre = null;
    }

    public boolean esAtributo() {
        return tipo == TipoNodo.ATRIBUTO || tipo == TipoNodo.ATRIBUTO_CLAVE;
    }

    public boolean esRelacion() {
        return tipo == TipoNodo.RELACION || tipo == TipoNodo.RELACION_IDENTIFICADORA;
    }

    // Rectangulo que envuelve al simbolo; un nombre largo lo ensancha.
    public Rectangle limites() {
        int ancho;
        int alto;
        if (esAtributo()) {
            ancho = Math.max(90, nombre.length() * 8 + 30);
            alto = ALTO_ATRIBUTO;
        } else if (esRelacion()) {
            ancho = Math.max(130, nombre.length() * 9 + 30);
            alto = ALTO_RELACION;
        } else {
            ancho = Math.max(150, nombre.length() * 9 + 40);
            alto = ALTO_ENTIDAD;
        }
        return new Rectangle(x - ancho / 2, y - alto / 2, ancho, alto);
    }

    public Shape forma() {
        return FormaSimbolo.contorno(tipo, limites());
    }

    // Acierta contra la forma real: el rectangulo del rombo atrapaba clics ajenos.
    public boolean contiene(int px, int py) {
        return forma().contains(px, py);
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    public void setTipo(TipoNodo tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public NodoVista getPadre() {
        return padre;
    }

    public void setPadre(NodoVista padre) {
        this.padre = padre;
    }
}
