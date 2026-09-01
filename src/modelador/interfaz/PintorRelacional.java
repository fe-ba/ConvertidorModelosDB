package modelador.interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;

// Dibujo del diagrama relacional: cajas de tabla y lineas de clave foranea.
public final class PintorRelacional {

    private static final int PASO_REJILLA = 28;
    private static final int LARGO_PUNTA = 10;

    private PintorRelacional() {
    }

    public static void rejilla(Graphics2D g2, Rectangle visible) {
        PintorER.rejilla(g2, visible);
    }

    // Las referencias van detras de las cajas, que son opacas.
    public static void referencias(Graphics2D g2, List<FiguraTabla> figuras) {
        g2.setStroke(new BasicStroke(1.5f));
        for (FiguraTabla origen : figuras) {
            for (Restriccion foranea : origen.getTabla().foraneas()) {
                FiguraTabla destino = buscar(figuras, foranea.getTablaReferida());
                if (destino == null || destino == origen) {
                    continue;
                }
                referencia(g2, origen, destino);
            }
        }
    }

    private static void referencia(Graphics2D g2, FiguraTabla hija, FiguraTabla padre) {
        Point desde = hija.borde(padre.getX(), padre.getY());
        Point hasta = padre.borde(hija.getX(), hija.getY());
        g2.setColor(Tema.TEAL);
        g2.drawLine(desde.x, desde.y, hasta.x, hasta.y);
        punta(g2, desde, hasta);
    }

    // Punta de flecha apoyada en el borde de la tabla referenciada.
    private static void punta(Graphics2D g2, Point desde, Point hasta) {
        double angulo = Math.atan2(hasta.y - desde.y, hasta.x - desde.x);
        Polygon punta = new Polygon();
        punta.addPoint(hasta.x, hasta.y);
        punta.addPoint((int) (hasta.x - LARGO_PUNTA * Math.cos(angulo - 0.4)),
                (int) (hasta.y - LARGO_PUNTA * Math.sin(angulo - 0.4)));
        punta.addPoint((int) (hasta.x - LARGO_PUNTA * Math.cos(angulo + 0.4)),
                (int) (hasta.y - LARGO_PUNTA * Math.sin(angulo + 0.4)));
        g2.fillPolygon(punta);
    }

    private static FiguraTabla buscar(List<FiguraTabla> figuras, String nombre) {
        if (nombre == null) {
            return null;
        }
        for (FiguraTabla figura : figuras) {
            if (figura.getTabla().getNombre().equalsIgnoreCase(nombre)
                    || figura.getTabla().nombreNormalizado()
                            .equalsIgnoreCase(nombre)) {
                return figura;
            }
        }
        return null;
    }

    // Caja con cabecera, franja de color segun el origen, y una fila por columna.
    public static void tabla(Graphics2D g2, FiguraTabla figura) {
        Tabla tabla = figura.getTabla();
        Rectangle r = figura.limites();
        Color acento = colorDeOrigen(tabla.getOrigen());

        g2.setColor(Tema.RELLENO_TABLA);
        g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 6, 6));
        g2.setColor(Tema.BORDE_TABLA);
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 6, 6));

        g2.setColor(Tema.CABECERA_TABLA);
        g2.fillRect(r.x + 1, r.y + 1, r.width - 2, FiguraTabla.ALTO_CABECERA - 1);
        g2.setColor(acento);
        g2.fillRect(r.x + 1, r.y + 1, 4, r.height - 2);
        g2.setColor(Tema.BORDE_TABLA);
        g2.drawLine(r.x, r.y + FiguraTabla.ALTO_CABECERA,
                r.x + r.width, r.y + FiguraTabla.ALTO_CABECERA);

        g2.setColor(Tema.TEXTO);
        g2.setFont(Tema.TITULO_TABLA);
        g2.drawString(tabla.nombreNormalizado(), r.x + 14, r.y + 20);
        g2.setColor(Tema.TENUE);
        g2.setFont(Tema.MENUDA);
        String etiqueta = etiquetaDeOrigen(tabla.getOrigen());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(etiqueta, r.x + r.width - 10 - fm.stringWidth(etiqueta), r.y + 20);

        columnas(g2, figura, r);
    }

    private static void columnas(Graphics2D g2, FiguraTabla figura, Rectangle r) {
        Tabla tabla = figura.getTabla();
        int fila = 0;
        for (Columna columna : tabla.getColumnas()) {
            int y = r.y + FiguraTabla.ALTO_CABECERA + 15 + fila * FiguraTabla.ALTO_FILA;
            boolean esClave = tabla.esClave(columna.getNombre());
            boolean esForanea = tabla.esForanea(columna.getNombre());

            g2.setFont(Tema.COLUMNA);
            g2.setColor(esClave ? Tema.ORO : (esForanea ? Tema.TEAL : Tema.TEXTO_COLUMNA));
            // una columna puede ser clave y foranea a la vez: se marcan ambas
            String marca;
            if (esClave) {
                marca = esForanea ? "\u25C6\u25B8 " : "\u25C6  ";
            } else {
                marca = esForanea ? "\u25B8  " : "    ";
            }
            g2.drawString(marca + columna.getNombre(), r.x + 12, y);

            if (esClave) {
                FontMetrics fm = g2.getFontMetrics();
                int inicio = r.x + 12 + fm.stringWidth(marca);
                g2.drawLine(inicio, y + 3,
                        inicio + fm.stringWidth(columna.getNombre()), y + 3);
            }
            g2.setColor(Tema.TENUE);
            g2.setFont(Tema.MENUDA);
            String tipo = columna.getTipo().name().toLowerCase()
                    + (columna.admiteNulos() ? "" : " *");
            FontMetrics fmTipo = g2.getFontMetrics();
            g2.drawString(tipo, r.x + r.width - 10 - fmTipo.stringWidth(tipo), y);
            fila++;
        }
    }

    private static Color colorDeOrigen(OrigenTabla origen) {
        switch (origen) {
            case RELACION_NM:
            case RELACION_NARIA:
                return Tema.VIOLETA;
            case ATRIBUTO_MULTIVALUADO:
                return Tema.TEAL;
            case ENTIDAD_DEBIL:
                return Tema.ORO.darker();
            default:
                return Tema.ORO;
        }
    }

    private static String etiquetaDeOrigen(OrigenTabla origen) {
        switch (origen) {
            case ENTIDAD_FUERTE: return "entidad";
            case ENTIDAD_DEBIL: return "debil";
            case RELACION_NM: return "N:M";
            case RELACION_NARIA: return "n-aria";
            case ATRIBUTO_MULTIVALUADO: return "multivaluado";
            default: return "";
        }
    }

    // Leyenda fija abajo a la izquierda.
    public static void leyenda(Graphics2D g2, int x, int y) {
        g2.setFont(Tema.MENUDA);
        g2.setColor(Tema.ORO);
        g2.drawString("\u25C6 clave primaria", x, y);
        g2.setColor(Tema.TEAL);
        g2.drawString("\u25B8 clave foranea", x + 110, y);
        g2.setColor(Tema.TENUE);
        g2.drawString("* obligatorio", x + 220, y);
    }
}
