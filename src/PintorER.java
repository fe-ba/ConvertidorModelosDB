import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

// Todo el dibujo del lienzo, sin estado propio.
public final class PintorER {

    private static final int PASO_REJILLA = 28;

    private PintorER() {
    }

    // Solo la zona del lienzo que se ve ahora mismo.
    public static void rejilla(Graphics2D g2, Rectangle visible) {
        int desdeX = (visible.x / PASO_REJILLA - 1) * PASO_REJILLA;
        int desdeY = (visible.y / PASO_REJILLA - 1) * PASO_REJILLA;
        int hastaX = visible.x + visible.width + PASO_REJILLA;
        int hastaY = visible.y + visible.height + PASO_REJILLA;
        g2.setColor(Tema.REJILLA);
        for (int x = desdeX; x <= hastaX; x += PASO_REJILLA) {
            g2.drawLine(x, desdeY, x, hastaY);
        }
        for (int y = desdeY; y <= hastaY; y += PASO_REJILLA) {
            g2.drawLine(desdeX, y, hastaX, y);
        }
    }

    public static void enlaces(Graphics2D g2, Tablero tablero) {
        g2.setColor(Tema.LINEA);
        g2.setStroke(new BasicStroke(1.6f));
        for (NodoVista nodo : tablero.getNodos()) {
            if (nodo.getPadre() != null) {
                g2.drawLine(nodo.getX(), nodo.getY(),
                        nodo.getPadre().getX(), nodo.getPadre().getY());
            }
        }
        for (Enlace enlace : tablero.getEnlaces()) {
            boolean activo = (enlace == tablero.getEnlaceSeleccionado());
            g2.setColor(activo ? Tema.ORO : Tema.TEAL);
            g2.setStroke(new BasicStroke(activo ? 3f : 1.6f));
            NodoVista a = enlace.getOrigen();
            NodoVista b = enlace.getDestino();
            g2.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            punta(g2, a, b);
        }
    }

    // Un solo camino para cualquier simbolo: la geometria la da FormaSimbolo.
    public static void nodo(Graphics2D g2, NodoVista nodo, boolean activo) {
        Rectangle r = nodo.limites();
        Shape contorno = FormaSimbolo.contorno(nodo.getTipo(), r);

        g2.setColor(relleno(nodo));
        g2.fill(contorno);
        g2.setColor(activo ? Tema.ORO : borde(nodo));
        g2.setStroke(new BasicStroke(activo ? 2.4f : grosor(nodo)));
        g2.draw(contorno);

        Shape interior = FormaSimbolo.interior(nodo.getTipo(), r);
        if (interior != null) {
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(interior);
        }
        if (!nodo.getNombre().isEmpty()) {
            texto(g2, nodo);
        }
    }

    private static java.awt.Color relleno(NodoVista nodo) {
        if (nodo.esAtributo()) {
            return Tema.RELLENO_ATRIBUTO;
        }
        return nodo.esRelacion() ? Tema.RELLENO_RELACION : Tema.RELLENO_ENTIDAD;
    }

    private static java.awt.Color borde(NodoVista nodo) {
        if (nodo.getTipo() == TipoNodo.ATRIBUTO_CLAVE) {
            return Tema.ORO.darker();
        }
        if (nodo.esAtributo()) {
            return Tema.BORDE_ATRIBUTO;
        }
        return nodo.esRelacion() ? Tema.BORDE_RELACION : Tema.BORDE_ENTIDAD;
    }

    private static float grosor(NodoVista nodo) {
        return nodo.esAtributo() ? 1.4f : 1.7f;
    }

    private static void texto(Graphics2D g2, NodoVista nodo) {
        g2.setFont(Tema.ETIQUETA);
        g2.setColor(nodo.getTipo() == TipoNodo.ATRIBUTO_CLAVE ? Tema.ORO : Tema.TEXTO);
        int ancho = g2.getFontMetrics().stringWidth(nodo.getNombre());
        int base = nodo.getY() + 5;
        g2.drawString(nodo.getNombre(), nodo.getX() - ancho / 2, base);
        if (nodo.getTipo() == TipoNodo.ATRIBUTO_CLAVE) {
            g2.drawLine(nodo.getX() - ancho / 2, base + 3,
                    nodo.getX() + ancho / 2, base + 3);
        }
    }

    // Punta de flecha apoyada en el borde del nodo de destino.
    private static void punta(Graphics2D g2, NodoVista desde, NodoVista hasta) {
        Point p = puntoBorde(hasta, desde.getX(), desde.getY());
        double ang = Math.atan2(hasta.getY() - desde.getY(),
                hasta.getX() - desde.getX());
        int largo = 11;
        Polygon punta = new Polygon();
        punta.addPoint(p.x, p.y);
        punta.addPoint((int) (p.x - largo * Math.cos(ang - 0.4)),
                (int) (p.y - largo * Math.sin(ang - 0.4)));
        punta.addPoint((int) (p.x - largo * Math.cos(ang + 0.4)),
                (int) (p.y - largo * Math.sin(ang + 0.4)));
        g2.fillPolygon(punta);
    }

    private static Point puntoBorde(NodoVista nodo, int haciaX, int haciaY) {
        Rectangle r = nodo.limites();
        double dx = haciaX - nodo.getX();
        double dy = haciaY - nodo.getY();
        if (dx == 0 && dy == 0) {
            return new Point(nodo.getX(), nodo.getY());
        }
        double escalaX = (Math.abs(dx) < 0.001)
                ? Double.MAX_VALUE : (r.width / 2.0) / Math.abs(dx);
        double escalaY = (Math.abs(dy) < 0.001)
                ? Double.MAX_VALUE : (r.height / 2.0) / Math.abs(dy);
        double escala = Math.min(escalaX, escalaY);
        return new Point((int) (nodo.getX() + dx * escala),
                (int) (nodo.getY() + dy * escala));
    }

    public static void asa(Graphics2D g2, Point centro, int radio) {
        g2.setColor(Tema.FONDO);
        g2.fillOval(centro.x - radio, centro.y - radio, radio * 2, radio * 2);
        g2.setColor(Tema.TEAL);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(centro.x - radio, centro.y - radio, radio * 2, radio * 2);
        g2.drawLine(centro.x - 4, centro.y, centro.x + 3, centro.y);
        g2.drawLine(centro.x, centro.y - 4, centro.x + 4, centro.y);
        g2.drawLine(centro.x, centro.y + 4, centro.x + 4, centro.y);
    }

    public static void flechaEnCurso(Graphics2D g2, NodoVista desde, Point hasta) {
        g2.setColor(Tema.TEAL);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[] {6f, 5f}, 0f));
        g2.drawLine(desde.getX(), desde.getY(), hasta.x, hasta.y);
    }

    public static void marco(Graphics2D g2, Rectangle marco) {
        Composite antes = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g2.setColor(Tema.TEAL);
        g2.fill(marco);
        g2.setComposite(antes);
        g2.setColor(Tema.TEAL);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 1f, new float[] {5f, 4f}, 0f));
        g2.draw(marco);
    }

    // Sombra de lo que se arrastra desde la paleta.
    public static void previsualizacion(Graphics2D g2, TipoNodo tipo, Point donde) {
        Composite antes = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        nodo(g2, new NodoVista(tipo, "", donde.x, donde.y), false);
        g2.setComposite(antes);
    }

    // Papelera dibujada a mano, sin depender de archivos de imagen.
    public static void iconoVaciar(Graphics2D g2, Rectangle z) {
        g2.setColor(Tema.RELLENO_ICONO);
        g2.fill(new RoundRectangle2D.Float(z.x, z.y, z.width, z.height, 8, 8));
        g2.setColor(Tema.BORDE_ICONO);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(z.x, z.y, z.width, z.height, 8, 8));

        int cx = z.x + z.width / 2;
        int cy = z.y + z.height / 2;
        g2.setColor(Tema.TRAZO_ICONO);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine(cx - 7, cy - 5, cx + 7, cy - 5);
        g2.drawLine(cx - 2, cy - 8, cx + 2, cy - 8);
        g2.drawLine(cx - 5, cy - 5, cx - 4, cy + 8);
        g2.drawLine(cx + 5, cy - 5, cx + 4, cy + 8);
        g2.drawLine(cx - 4, cy + 8, cx + 4, cy + 8);
        g2.drawLine(cx, cy - 2, cx, cy + 5);
    }

    public static void nivelDeZoom(Graphics2D g2, double escala, int x, int y) {
        g2.setColor(Tema.TENUE);
        g2.setFont(Tema.MENUDA);
        g2.drawString(Math.round(escala * 100) + "%", x, y);
    }
}
