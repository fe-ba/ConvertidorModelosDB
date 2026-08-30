import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

// Unica definicion de la geometria: sirve para dibujar, para la miniatura y para acertar los clics.
public final class FormaSimbolo {

    private static final int MARGEN_INTERIOR_ENTIDAD = 5;
    private static final int MARGEN_INTERIOR_ROMBO = 9;

    private FormaSimbolo() {
    }

    // Contorno exterior del simbolo.
    public static Shape contorno(TipoNodo tipo, Rectangle r) {
        switch (tipo) {
            case ATRIBUTO:
            case ATRIBUTO_CLAVE:
                return new Ellipse2D.Double(r.x, r.y, r.width, r.height);
            case RELACION:
            case RELACION_IDENTIFICADORA:
                return rombo(r);
            default:
                return new Rectangle(r);
        }
    }

    // Borde interior de la entidad debil y la relacion identificadora; null si no lleva.
    public static Shape interior(TipoNodo tipo, Rectangle r) {
        if (tipo == TipoNodo.ENTIDAD_DEBIL) {
            return encoger(r, MARGEN_INTERIOR_ENTIDAD);
        }
        if (tipo == TipoNodo.RELACION_IDENTIFICADORA) {
            return rombo(encoger(r, MARGEN_INTERIOR_ROMBO));
        }
        return null;
    }

    private static Polygon rombo(Rectangle r) {
        Polygon p = new Polygon();
        p.addPoint(r.x + r.width / 2, r.y);
        p.addPoint(r.x + r.width, r.y + r.height / 2);
        p.addPoint(r.x + r.width / 2, r.y + r.height);
        p.addPoint(r.x, r.y + r.height / 2);
        return p;
    }

    private static Rectangle encoger(Rectangle r, int margen) {
        return new Rectangle(r.x + margen, r.y + margen,
                r.width - 2 * margen, r.height - 2 * margen);
    }
}
