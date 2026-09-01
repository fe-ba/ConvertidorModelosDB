package modelador.interfaz;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import modelador.dominio.er.Atributo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Relacion;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.Tabla;

// Diagrama del esquema relacional. Comparte con el lienzo E-R la navegacion:
// rueda para el zoom, boton central o espacio para desplazar, Inicio para
// encuadrar. Las tablas se colocan solas y no se arrastran.
public class LienzoRelacional extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final double ESCALA_MINIMA = 0.4;
    private static final double ESCALA_MAXIMA = 2.5;
    private static final int SEPARACION_X = 320;
    private static final int SEPARACION_Y = 260;
    private static final int POR_FILA = 3;
    private static final int MARGEN = 180;

    private transient EsquemaRelacional esquema;
    private transient List<FiguraTabla> figuras = new ArrayList<>();

    private double escala = 1.0;
    private int origenX;
    private int origenY;
    private Point inicioPanoramica;
    private boolean espacioPulsado;

    public LienzoRelacional() {
        setBackground(Tema.FONDO);
        setPreferredSize(new Dimension(900, 640));
        setFocusable(true);
        instalarRaton();
        instalarRueda();
        instalarTeclas();
    }

    // Recoloca todo: se llama tras cada conversion.
    public void mostrar(EsquemaRelacional esquema) {
        this.esquema = esquema;
        this.figuras = colocar(esquema);
        encuadrar();
        repaint();
    }

    public boolean estaVacio() {
        return figuras.isEmpty();
    }

    // Cada tabla nace de un elemento del E-R; se coloca cerca de el para que el
    // diagrama recuerde al original. Las que no tienen origen van a una rejilla.
    private List<FiguraTabla> colocar(EsquemaRelacional esquema) {
        List<FiguraTabla> colocadas = new ArrayList<>();
        if (esquema == null) {
            return colocadas;
        }
        Map<String, Point> posicionesER = posicionesDelModelo();
        int sueltas = 0;
        for (Tabla tabla : esquema.getTablas()) {
            Point origen = posicionesER.get(tabla.getProcedeDe());
            Point donde;
            if (origen != null) {
                donde = new Point(origen.x, origen.y);
            } else {
                donde = new Point(MARGEN + (sueltas % POR_FILA) * SEPARACION_X,
                        MARGEN + (sueltas / POR_FILA) * SEPARACION_Y);
                sueltas++;
            }
            colocadas.add(new FiguraTabla(tabla, donde.x, donde.y));
        }
        separar(colocadas);
        return colocadas;
    }

    // El modelo E-R lo pone la ventana antes de convertir; si no hay, se
    // reparten en rejilla.
    private Map<String, Point> posicionesDelModelo() {
        Map<String, Point> posiciones = new HashMap<>();
        if (modelo == null) {
            return posiciones;
        }
        for (Entidad entidad : modelo.getEntidades()) {
            posiciones.put(entidad.getId(), new Point(
                    (int) entidad.getPosicion().getX(),
                    (int) entidad.getPosicion().getY()));
            for (Atributo atributo : entidad.getAtributos()) {
                posiciones.put(atributo.getId(), new Point(
                        (int) (entidad.getPosicion().getX()
                                + atributo.getDesplazamiento().getX()),
                        (int) (entidad.getPosicion().getY()
                                + atributo.getDesplazamiento().getY())));
            }
        }
        for (Relacion relacion : modelo.getRelaciones()) {
            posiciones.put(relacion.getId(), new Point(
                    (int) relacion.getPosicion().getX(),
                    (int) relacion.getPosicion().getY()));
        }
        return posiciones;
    }

    private transient ModeloER modelo;

    public void setModelo(ModeloER modelo) {
        this.modelo = modelo;
    }

    // Las cajas son mas grandes que los simbolos del E-R, asi que se separan
    // hasta que dejan de pisarse.
    private void separar(List<FiguraTabla> colocadas) {
        for (int vuelta = 0; vuelta < 60; vuelta++) {
            boolean hubieronChoques = false;
            for (int i = 0; i < colocadas.size(); i++) {
                for (int j = i + 1; j < colocadas.size(); j++) {
                    FiguraTabla a = colocadas.get(i);
                    FiguraTabla b = colocadas.get(j);
                    Rectangle ra = holgura(a);
                    if (!ra.intersects(holgura(b))) {
                        continue;
                    }
                    hubieronChoques = true;
                    int dx = b.getX() - a.getX();
                    int dy = b.getY() - a.getY();
                    if (dx == 0 && dy == 0) {
                        dx = 1;
                    }
                    double largo = Math.max(1, Math.hypot(dx, dy));
                    int paso = 14;
                    a.mover(a.getX() - (int) (dx / largo * paso),
                            a.getY() - (int) (dy / largo * paso));
                    b.mover(b.getX() + (int) (dx / largo * paso),
                            b.getY() + (int) (dy / largo * paso));
                }
            }
            if (!hubieronChoques) {
                return;
            }
        }
    }

    private Rectangle holgura(FiguraTabla figura) {
        Rectangle r = figura.limites();
        return new Rectangle(r.x - 20, r.y - 20, r.width + 40, r.height + 40);
    }

    // --- Navegacion, igual que en el lienzo E-R ---

    private Point aMundo(int sx, int sy) {
        return new Point((int) Math.round((sx - origenX) / escala),
                (int) Math.round((sy - origenY) / escala));
    }

    public void ampliar(double factor, int anclaX, int anclaY) {
        double nueva = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, escala * factor));
        if (nueva == escala) {
            return;
        }
        Point antes = aMundo(anclaX, anclaY);
        escala = nueva;
        origenX = (int) Math.round(anclaX - antes.x * escala);
        origenY = (int) Math.round(anclaY - antes.y * escala);
        repaint();
    }

    // Ajusta el zoom y el desplazamiento para que quepa todo el diagrama.
    public void encuadrar() {
        if (figuras.isEmpty() || getWidth() == 0) {
            escala = 1.0;
            origenX = 0;
            origenY = 0;
            return;
        }
        Rectangle todo = null;
        for (FiguraTabla figura : figuras) {
            todo = (todo == null) ? figura.limites() : todo.union(figura.limites());
        }
        todo.grow(40, 40);
        double porAncho = getWidth() / (double) todo.width;
        double porAlto = getHeight() / (double) todo.height;
        escala = Math.max(ESCALA_MINIMA, Math.min(1.0, Math.min(porAncho, porAlto)));
        origenX = (int) (getWidth() / 2.0 - (todo.x + todo.width / 2.0) * escala);
        origenY = (int) (getHeight() / 2.0 - (todo.y + todo.height / 2.0) * escala);
        repaint();
    }

    private void instalarRueda() {
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                ampliar(e.getWheelRotation() < 0 ? 1.12 : 1 / 1.12, e.getX(), e.getY());
            }
        });
    }

    private void instalarRaton() {
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (esGestoDeDesplazar(e)) {
                    inicioPanoramica = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inicioPanoramica == null) {
                    return;
                }
                origenX += e.getX() - inicioPanoramica.x;
                origenY += e.getY() - inicioPanoramica.y;
                inicioPanoramica = e.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                inicioPanoramica = null;
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
    }

    // Aqui el arrastre normal tambien desplaza: no hay marco de seleccion que
    // ocupe ese gesto, porque las tablas no se seleccionan.
    private boolean esGestoDeDesplazar(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON1
                || e.getButton() == MouseEvent.BUTTON2
                || espacioPulsado;
    }

    private void instalarTeclas() {
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("pressed SPACE"), "manoAbajo");
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("released SPACE"), "manoArriba");
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "encuadrar");
        getActionMap().put("manoAbajo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                espacioPulsado = true;
            }
        });
        getActionMap().put("manoArriba", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                espacioPulsado = false;
            }
        });
        getActionMap().put("encuadrar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                encuadrar();
            }
        });
    }

    // --- Dibujo ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D mundo = (Graphics2D) g.create();
        mundo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        mundo.translate(origenX, origenY);
        mundo.scale(escala, escala);

        PintorRelacional.rejilla(mundo, zonaVisible());
        PintorRelacional.referencias(mundo, figuras);
        for (FiguraTabla figura : figuras) {
            PintorRelacional.tabla(mundo, figura);
        }
        mundo.dispose();

        Graphics2D fijo = (Graphics2D) g;
        fijo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (figuras.isEmpty()) {
            mensajeVacio(fijo);
        } else {
            PintorRelacional.leyenda(fijo, 12, getHeight() - 30);
            PintorER.ayudaDeNavegacion(fijo, 12, getHeight() - 12);
            if (Math.abs(escala - 1.0) >= 0.01) {
                PintorER.nivelDeZoom(fijo, escala, 12, getHeight() - 48);
            }
        }
    }

    private void mensajeVacio(Graphics2D g2) {
        g2.setColor(Tema.TENUE);
        g2.setFont(Tema.ETIQUETA);
        String texto = "Convierte el diagrama E-R para ver aqui las tablas (F9).";
        int ancho = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, (getWidth() - ancho) / 2, getHeight() / 2);
    }

    private Rectangle zonaVisible() {
        Point esquina = aMundo(0, 0);
        Point opuesta = aMundo(getWidth(), getHeight());
        return new Rectangle(esquina.x, esquina.y,
                opuesta.x - esquina.x, opuesta.y - esquina.y);
    }
}
