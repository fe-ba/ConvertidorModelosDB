import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

// Vista: traduce raton, rueda y teclado en operaciones del Tablero, y delega el dibujo.
public class LienzoER extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final double ESCALA_MINIMA = 0.4;
    private static final double ESCALA_MAXIMA = 2.5;
    private static final int RADIO_ASA = 9;
    private static final int LADO_ICONO = 30;
    private static final int MARGEN_ICONO = 12;

    private transient Tablero tablero;

    // Zoom y desplazamiento. Pantalla = lienzo * escala + origen.
    private double escala;
    private int origenX;
    private int origenY;

    // --- gestos en curso ---
    private Point ultimoArrastre;
    private transient NodoVista enlazandoDesde;
    private Point puntaFlecha;
    private Point inicioMarco;
    private Rectangle marco;
    private transient TipoNodo previsualizando;
    private Point puntoPrevisualizacion;

    public LienzoER(Tablero tablero) {
        this.tablero = tablero;
        this.escala = 1.0;
        this.origenX = 0;
        this.origenY = 0;
        setBackground(Tema.FONDO);
        setPreferredSize(new Dimension(900, 640));
        setFocusable(true);
        tablero.alCambiar(new Runnable() {
            public void run() {
                repaint();
            }
        });
        instalarRaton();
        instalarRueda();
        instalarSoltar();
        instalarTeclas();
    }

    public Tablero getTablero() {
        return tablero;
    }

    // --- Zoom y coordenadas ---
    // De pantalla a coordenadas del lienzo. Evita que el zoom se cuele en cada manejador.
    private Point aMundo(int sx, int sy) {
        return new Point((int) Math.round((sx - origenX) / escala),
                (int) Math.round((sy - origenY) / escala));
    }

    // Acerca o aleja dejando quieto el punto que hay bajo el cursor.
    public void ampliar(double factor, int anclaX, int anclaY) {
        double nueva = Math.max(ESCALA_MINIMA,
                Math.min(ESCALA_MAXIMA, escala * factor));
        if (nueva == escala) {
            return;
        }
        Point antes = aMundo(anclaX, anclaY);
        escala = nueva;
        origenX = (int) Math.round(anclaX - antes.x * escala);
        origenY = (int) Math.round(anclaY - antes.y * escala);
        repaint();
    }

    private void instalarRueda() {
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                ampliar(e.getWheelRotation() < 0 ? 1.12 : 1 / 1.12, e.getX(), e.getY());
            }
        });
    }

    // --- Icono de vaciar, fijo arriba a la derecha ---
    // En coordenadas de pantalla, asi que el zoom no le afecta.
    private Rectangle zonaIconoVaciar() {
        return new Rectangle(getWidth() - LADO_ICONO - MARGEN_ICONO, MARGEN_ICONO,
                LADO_ICONO, LADO_ICONO);
    }

    private void pedirVaciado() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se borraran todos los elementos del lienzo. No se puede deshacer.",
                "Vaciar tablero", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (respuesta == JOptionPane.OK_OPTION) {
            tablero.vaciar();
        }
    }

    // --- Colocar un simbolo ---
    public void soltar(TipoNodo tipo, int x, int y) {
        if (!tablero.puedeColocarse(tipo, x, y)) {
            JOptionPane.showMessageDialog(this,
                    "Suelta el atributo cerca de una entidad o de una relacion.",
                    "Nada donde engancharlo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        tablero.agregar(tipo, tablero.nombrePorDefecto(tipo), x, y);
    }

    // Para el doble clic en la paleta.
    public void soltarEnElCentro(TipoNodo tipo) {
        Point centro = aMundo(getWidth() / 2, getHeight() / 2);
        soltar(tipo, centro.x, centro.y);
    }

    // --- Asa de enlazar ---
    private Point asaDe(NodoVista nodo) {
        Rectangle r = nodo.limites();
        return new Point(r.x + r.width + RADIO_ASA + 2, r.y + r.height / 2);
    }

    private boolean sobreElAsa(int x, int y) {
        NodoVista unico = tablero.getSeleccionado();
        if (unico == null || unico.esAtributo()) {
            return false;
        }
        Point a = asaDe(unico);
        return Math.hypot(a.x - x, a.y - y) <= RADIO_ASA + 3;
    }

    // --- Raton ---
    private void instalarRaton() {
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                // el icono se comprueba en pantalla, antes de traducir nada
                if (!tablero.estaVacio() && zonaIconoVaciar().contains(e.getPoint())) {
                    pedirVaciado();
                    return;
                }
                Point m = aMundo(e.getX(), e.getY());
                // Orden: asa, nodo, enlace, fondo. Al reves, una linea encima taparia al nodo.
                if (sobreElAsa(m.x, m.y)) {
                    enlazandoDesde = tablero.getSeleccionado();
                    puntaFlecha = m;
                    repaint();
                    return;
                }
                NodoVista bajo = tablero.nodoEn(m.x, m.y);
                if (bajo != null) {
                    if (e.isControlDown() || e.isShiftDown()) {
                        tablero.alternarSeleccion(bajo);
                    } else if (!tablero.estaSeleccionado(bajo)) {
                        tablero.seleccionarSolo(bajo);
                    }
                    ultimoArrastre = m;
                    return;
                }
                Enlace enlace = tablero.enlaceEn(m.x, m.y);
                if (enlace != null) {
                    tablero.seleccionarEnlace(enlace);
                    return;
                }
                tablero.seleccionarSolo(null);
                inicioMarco = m;
                marco = new Rectangle(inicioMarco);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point m = aMundo(e.getX(), e.getY());
                if (enlazandoDesde != null) {
                    puntaFlecha = m;
                    repaint();
                    return;
                }
                if (inicioMarco != null) {
                    marco = new Rectangle(
                            Math.min(inicioMarco.x, m.x), Math.min(inicioMarco.y, m.y),
                            Math.abs(m.x - inicioMarco.x), Math.abs(m.y - inicioMarco.y));
                    repaint();
                    return;
                }
                if (ultimoArrastre != null && !tablero.getSeleccionados().isEmpty()) {
                    // el grupo entero se mueve con el mismo desplazamiento
                    int dx = m.x - ultimoArrastre.x;
                    int dy = m.y - ultimoArrastre.y;
                    for (NodoVista nodo : tablero.getSeleccionados()) {
                        nodo.mover(nodo.getX() + dx, nodo.getY() + dy);
                    }
                    ultimoArrastre = m;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point m = aMundo(e.getX(), e.getY());
                if (enlazandoDesde != null) {
                    tablero.enlazar(enlazandoDesde, tablero.nodoEn(m.x, m.y));
                    enlazandoDesde = null;
                    puntaFlecha = null;
                }
                if (marco != null) {
                    seleccionarDentroDelMarco();
                    inicioMarco = null;
                    marco = null;
                }
                ultimoArrastre = null;
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
    }

    // Entra todo lo que el marco toque, aunque sea en parte.
    private void seleccionarDentroDelMarco() {
        if (marco.width < 4 && marco.height < 4) {
            return;   // fue un clic, no un arrastre
        }
        List<NodoVista> dentro = new ArrayList<NodoVista>();
        for (NodoVista nodo : tablero.getNodos()) {
            if (marco.intersects(nodo.limites())) {
                dentro.add(nodo);
            }
        }
        tablero.seleccionarVarios(dentro);
    }

    // --- Teclado ---
    // Atadas a WHEN_FOCUSED: borrar texto en el panel no borra el nodo.
    private void instalarTeclas() {
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "borrar");
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "borrar");
        getActionMap().put("borrar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                tablero.eliminarSeleccion();
            }
        });
    }

    // --- Soltar desde la paleta ---
    private void instalarSoltar() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport soporte) {
                if (!soporte.isDrop()
                        || !soporte.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }
                previsualizando = leerTipo(soporte);
                Point p = soporte.getDropLocation().getDropPoint();
                puntoPrevisualizacion = aMundo(p.x, p.y);
                repaint();
                return true;
            }

            @Override
            public boolean importData(TransferSupport soporte) {
                TipoNodo tipo = leerTipo(soporte);
                Point p = soporte.getDropLocation().getDropPoint();
                limpiarPrevisualizacion();
                if (tipo == null) {
                    return false;
                }
                Point donde = aMundo(p.x, p.y);
                soltar(tipo, donde.x, donde.y);
                return true;
            }

            private TipoNodo leerTipo(TransferSupport soporte) {
                try {
                    return TipoNodo.valueOf((String) soporte.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor));
                } catch (Exception ex) {
                    return null;
                }
            }
        });
        // getDropTarget() puede ser nulo si el componente aun no es visible
        if (getDropTarget() == null) {
            return;
        }
        try {
            getDropTarget().addDropTargetListener(new DropTargetAdapter() {
                public void drop(java.awt.dnd.DropTargetDropEvent e) {
                    limpiarPrevisualizacion();
                }

                @Override
                public void dragExit(DropTargetEvent e) {
                    limpiarPrevisualizacion();
                }
            });
        } catch (java.util.TooManyListenersException ex) {
            // el TransferHandler ya registro el suyo
        }
    }

    private void limpiarPrevisualizacion() {
        previsualizando = null;
        puntoPrevisualizacion = null;
        repaint();
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

        PintorER.rejilla(mundo, zonaVisible());
        PintorER.enlaces(mundo, tablero);
        for (NodoVista nodo : tablero.getNodos()) {
            PintorER.nodo(mundo, nodo, tablero.estaSeleccionado(nodo));
        }
        NodoVista unico = tablero.getSeleccionado();
        if (unico != null && !unico.esAtributo()) {
            PintorER.asa(mundo, asaDe(unico), RADIO_ASA);
        }
        if (enlazandoDesde != null && puntaFlecha != null) {
            PintorER.flechaEnCurso(mundo, enlazandoDesde, puntaFlecha);
        }
        if (marco != null && (marco.width > 1 || marco.height > 1)) {
            PintorER.marco(mundo, marco);
        }
        if (previsualizando != null && puntoPrevisualizacion != null) {
            PintorER.previsualizacion(mundo, previsualizando, puntoPrevisualizacion);
        }
        mundo.dispose();

        // sin transformar: se quedan fijos aunque se acerque o se desplace
        Graphics2D fijo = (Graphics2D) g;
        fijo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (!tablero.estaVacio()) {
            PintorER.iconoVaciar(fijo, zonaIconoVaciar());
        }
        if (Math.abs(escala - 1.0) >= 0.01) {
            PintorER.nivelDeZoom(fijo, escala, 12, getHeight() - 12);
        }
    }

    // Trozo del lienzo que cabe ahora mismo en la ventana.
    private Rectangle zonaVisible() {
        Point esquina = aMundo(0, 0);
        Point opuesta = aMundo(getWidth(), getHeight());
        return new Rectangle(esquina.x, esquina.y,
                opuesta.x - esquina.x, opuesta.y - esquina.y);
    }
}
