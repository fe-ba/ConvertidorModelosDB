import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.swing.Icon;

// Miniatura para la paleta, con la misma geometria que el lienzo.
public class IconoSimbolo implements Icon {

    private static final int ANCHO = 42;
    private static final int ALTO = 30;
    private static final int MARGEN = 4;

    private TipoNodo tipo;

    public IconoSimbolo(TipoNodo tipo) {
        this.tipo = tipo;
    }

    public int getIconWidth() {
        return ANCHO;
    }

    public int getIconHeight() {
        return ALTO;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle r = new Rectangle(x + MARGEN, y + MARGEN,
                ANCHO - 2 * MARGEN, ALTO - 2 * MARGEN);

        Shape contorno = FormaSimbolo.contorno(tipo, r);
        g2.setColor(relleno());
        g2.fill(contorno);
        g2.setColor(borde());
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(contorno);

        Shape interior = FormaSimbolo.interior(tipo, r);
        if (interior != null) {
            g2.draw(interior);
        }
        if (tipo == TipoNodo.ATRIBUTO_CLAVE) {
            g2.drawLine(r.x + 8, r.y + r.height - 5,
                    r.x + r.width - 8, r.y + r.height - 5);
        }
        g2.dispose();
    }

    private Color relleno() {
        switch (tipo) {
            case ATRIBUTO:
            case ATRIBUTO_CLAVE:
                return Tema.RELLENO_ATRIBUTO;
            case RELACION:
            case RELACION_IDENTIFICADORA:
                return Tema.RELLENO_RELACION;
            default:
                return Tema.RELLENO_ENTIDAD;
        }
    }

    private Color borde() {
        switch (tipo) {
            case ATRIBUTO_CLAVE:
                return Tema.ORO;
            case ATRIBUTO:
                return Tema.BORDE_ATRIBUTO;
            case RELACION:
            case RELACION_IDENTIFICADORA:
                return Tema.BORDE_RELACION;
            default:
                return Tema.BORDE_ENTIDAD;
        }
    }
}
