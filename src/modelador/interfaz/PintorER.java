package modelador.interfaz;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.Modalidad;

// Todo el dibujo del lienzo, sin estado propio.
public final class PintorER {

    private static final int PASO_REJILLA = 28;
    private static final double SEPARACION_TOTAL = 3.4;

    private PintorER() {
    }

    // Rejilla solo en la zona visible.
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

    // Lineas de atributo a su duenno, y participaciones con su cardinalidad.
    public static void enlaces(Graphics2D g2, Tablero tablero) {
        g2.setColor(Tema.LINEA);
        g2.setStroke(new BasicStroke(1.6f));
        for (Figura figura : tablero.getFiguras()) {
            if (figura.esAtributo()) {
                Figura duenno = Figura.de(figura.getDuenno());
                g2.drawLine(figura.getX(), figura.getY(), duenno.getX(), duenno.getY());
            }
        }
        for (EnlaceVista enlace : tablero.getEnlaces()) {
            participacion(g2, enlace, enlace == tablero.getEnlaceSeleccionado());
        }
    }

    // Notacion de Chen: linea doble si la participacion es total, y una cajita
    // con la cardinalidad hacia el lado de la entidad.
    private static void participacion(Graphics2D g2, EnlaceVista enlace, boolean activo) {
        Figura relacion = Figura.de(enlace.getRelacion());
        Figura entidad = Figura.de(enlace.getEntidad());
        int rx = relacion.getX();
        int ry = relacion.getY();
        int ex = entidad.getX();
        int ey = entidad.getY();

        g2.setColor(activo ? Tema.ORO : Tema.LINEA);
        g2.setStroke(new BasicStroke(activo ? 3f : 1.6f));
        if (enlace.getParte().getModalidad() == Modalidad.TOTAL) {
            double dx = ex - rx;
            double dy = ey - ry;
            double largo = Math.max(1, Math.hypot(dx, dy));
            int nx = (int) Math.round(-dy / largo * SEPARACION_TOTAL);
            int ny = (int) Math.round(dx / largo * SEPARACION_TOTAL);
            g2.drawLine(rx + nx, ry + ny, ex + nx, ey + ny);
            g2.drawLine(rx - nx, ry - ny, ex - nx, ey - ny);
        } else {
            g2.drawLine(rx, ry, ex, ey);
        }
        cardinalidad(g2, enlace, rx + (int) ((ex - rx) * 0.62),
                ry + (int) ((ey - ry) * 0.62), activo);
    }

    private static void cardinalidad(Graphics2D g2, EnlaceVista enlace,
                                     int cx, int cy, boolean activo) {
        String texto = enlace.getParte().getCardinalidad() == Cardinalidad.UNO ? "1" : "N";
        Rectangle caja = new Rectangle(cx - 13, cy - 11, 26, 22);
        g2.setColor(Tema.FONDO);
        g2.fill(caja);
        g2.setColor(activo ? Tema.ORO : Tema.BORDE);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(caja);
        g2.setColor(Tema.TEAL);
        g2.setFont(Tema.ETIQUETA);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, cx - fm.stringWidth(texto) / 2, cy + 5);
    }

    // Un solo camino para cualquier simbolo: la geometria la da FormaSimbolo.
    public static void nodo(Graphics2D g2, Figura figura, boolean activo) {
        dibujar(g2, figura.getTipo(), figura.getNombre(), figura.limites(), activo);
    }

    private static void dibujar(Graphics2D g2, TipoNodo tipo, String nombre,
                                Rectangle r, boolean activo) {
        Shape contorno = FormaSimbolo.contorno(tipo, r);
        g2.setColor(relleno(tipo));
        g2.fill(contorno);
        g2.setColor(activo ? Tema.ORO : borde(tipo));
        g2.setStroke(new BasicStroke(activo ? 2.4f : grosor(tipo)));
        g2.draw(contorno);

        Shape interior = FormaSimbolo.interior(tipo, r);
        if (interior != null) {
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(interior);
        }
        if (!nombre.isEmpty()) {
            texto(g2, tipo, nombre, r);
        }
    }

    private static Color relleno(TipoNodo tipo) {
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

    private static Color borde(TipoNodo tipo) {
        switch (tipo) {
            case ATRIBUTO_CLAVE:
                return Tema.ORO.darker();
            case ATRIBUTO:
                return Tema.BORDE_ATRIBUTO;
            case RELACION:
            case RELACION_IDENTIFICADORA:
                return Tema.BORDE_RELACION;
            default:
                return Tema.BORDE_ENTIDAD;
        }
    }

    private static float grosor(TipoNodo tipo) {
        return (tipo == TipoNodo.ATRIBUTO || tipo == TipoNodo.ATRIBUTO_CLAVE)
                ? 1.4f : 1.7f;
    }

    private static void texto(Graphics2D g2, TipoNodo tipo, String nombre, Rectangle r) {
        g2.setFont(Tema.ETIQUETA);
        g2.setColor(tipo == TipoNodo.ATRIBUTO_CLAVE ? Tema.ORO : Tema.TEXTO);
        int ancho = g2.getFontMetrics().stringWidth(nombre);
        int cx = r.x + r.width / 2;
        int base = r.y + r.height / 2 + 5;
        g2.drawString(nombre, cx - ancho / 2, base);
        if (tipo == TipoNodo.ATRIBUTO_CLAVE) {
            g2.drawLine(cx - ancho / 2, base + 3, cx + ancho / 2, base + 3);
        }
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

    public static void flechaEnCurso(Graphics2D g2, Figura desde, Point hasta) {
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
        dibujar(g2, tipo, "", Figura.limites(tipo, "", donde.x, donde.y), false);
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

    // Recordatorio de los gestos de navegacion.
    public static void ayudaDeNavegacion(Graphics2D g2, int x, int y) {
        g2.setColor(Tema.TENUE);
        g2.setFont(Tema.MENUDA);
        g2.drawString("rueda: zoom   ·   boton central o espacio: desplazar"
                + "   ·   Inicio: encuadrar", x, y);
    }

    public static void nivelDeZoom(Graphics2D g2, double escala, int x, int y) {
        g2.setColor(Tema.TENUE);
        g2.setFont(Tema.MENUDA);
        g2.drawString(Math.round(escala * 100) + "%", x, y);
    }
}
