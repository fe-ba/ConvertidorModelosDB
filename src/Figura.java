import java.awt.Rectangle;
import java.awt.Shape;

// Proyeccion del modelo E-R a algo dibujable: posicion absoluta, forma y nombre.
// No guarda estado propio; se construye al vuelo y escribe siempre en el modelo.
public class Figura {

    private static final int ALTO_ATRIBUTO = 34;
    private static final int ALTO_RELACION = 72;
    private static final int ALTO_ENTIDAD = 56;

    private final ElementoDelModelo elemento;
    // Solo para atributos: la entidad o la relacion de la que cuelgan.
    private final ElementoDelModelo duenno;

    public Figura(ElementoDelModelo elemento, ElementoDelModelo duenno) {
        this.elemento = elemento;
        this.duenno = duenno;
    }

    public static Figura de(ElementoDelModelo elemento) {
        return new Figura(elemento, null);
    }

    public static Figura deAtributo(Atributo atributo, ElementoDelModelo duenno) {
        return new Figura(atributo, duenno);
    }

    public ElementoDelModelo getElemento() {
        return elemento;
    }

    public ElementoDelModelo getDuenno() {
        return duenno;
    }

    public String getId() {
        return elemento.getId();
    }

    public String getNombre() {
        return elemento.getNombre();
    }

    public boolean esAtributo() {
        return elemento instanceof Atributo;
    }

    public boolean esRelacion() {
        return elemento instanceof Relacion;
    }

    public boolean esEntidad() {
        return elemento instanceof Entidad;
    }

    // El simbolo que toca dibujar se deduce del propio modelo.
    public TipoNodo getTipo() {
        if (elemento instanceof Atributo) {
            return ((Atributo) elemento).esClave()
                    ? TipoNodo.ATRIBUTO_CLAVE : TipoNodo.ATRIBUTO;
        }
        if (elemento instanceof Relacion) {
            return ((Relacion) elemento).esIdentificadora()
                    ? TipoNodo.RELACION_IDENTIFICADORA : TipoNodo.RELACION;
        }
        return ((Entidad) elemento).esDebil() ? TipoNodo.ENTIDAD_DEBIL : TipoNodo.ENTIDAD;
    }

    // Un atributo vive en coordenadas relativas a su duenno.
    public int getX() {
        if (esAtributo()) {
            return (int) (duenno.getPosicion().getX()
                    + ((Atributo) elemento).getDesplazamiento().getX());
        }
        return (int) elemento.getPosicion().getX();
    }

    public int getY() {
        if (esAtributo()) {
            return (int) (duenno.getPosicion().getY()
                    + ((Atributo) elemento).getDesplazamiento().getY());
        }
        return (int) elemento.getPosicion().getY();
    }

    // Mover escribe en el modelo: en el desplazamiento si es atributo, en la
    // posicion si no.
    public void mover(int x, int y) {
        if (esAtributo()) {
            ((Atributo) elemento).setDesplazamiento(new Punto(
                    x - duenno.getPosicion().getX(), y - duenno.getPosicion().getY()));
        } else {
            elemento.setPosicion(new Punto(x, y));
        }
    }

    public void desplazar(int dx, int dy) {
        mover(getX() + dx, getY() + dy);
    }

    // Rectangulo que envuelve al simbolo; un nombre largo lo ensancha.
    public Rectangle limites() {
        return limites(getTipo(), getNombre(), getX(), getY());
    }

    // Version estatica: la usa la vista previa del arrastre, que aun no tiene
    // ningun elemento del modelo detras.
    public static Rectangle limites(TipoNodo tipo, String nombre, int x, int y) {
        int ancho;
        int alto;
        if (tipo == TipoNodo.ATRIBUTO || tipo == TipoNodo.ATRIBUTO_CLAVE) {
            ancho = Math.max(90, nombre.length() * 8 + 30);
            alto = ALTO_ATRIBUTO;
        } else if (tipo == TipoNodo.RELACION
                || tipo == TipoNodo.RELACION_IDENTIFICADORA) {
            ancho = Math.max(130, nombre.length() * 9 + 30);
            alto = ALTO_RELACION;
        } else {
            ancho = Math.max(150, nombre.length() * 9 + 40);
            alto = ALTO_ENTIDAD;
        }
        return new Rectangle(x - ancho / 2, y - alto / 2, ancho, alto);
    }

    public Shape forma() {
        return FormaSimbolo.contorno(getTipo(), limites());
    }

    // Acierta contra la forma real, no contra el rectangulo que la envuelve.
    public boolean contiene(int px, int py) {
        return forma().contains(px, py);
    }

    // Dos figuras son la misma si envuelven al mismo elemento del modelo.
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Figura)) {
            return false;
        }
        return elemento == ((Figura) otro).elemento;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(elemento);
    }
}
