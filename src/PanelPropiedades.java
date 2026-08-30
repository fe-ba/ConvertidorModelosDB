import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// Cuatro caras segun la seleccion: nada, un nodo, varios o un enlace. Se reconstruye entero.
public class PanelPropiedades extends JPanel {

    private static final long serialVersionUID = 1L;

    private transient Tablero tablero;
    private transient LienzoER lienzo;

    public PanelPropiedades(LienzoER lienzo) {
        this.lienzo = lienzo;
        this.tablero = lienzo.getTablero();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(270, 0));
        setBackground(Tema.PANEL);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        tablero.alCambiar(new Runnable() {
            public void run() {
                reconstruir();
            }
        });
        reconstruir();
    }

    private void reconstruir() {
        removeAll();
        if (tablero.getEnlaceSeleccionado() != null) {
            construirParaEnlace();
        } else if (tablero.getSeleccionados().size() > 1) {
            construirParaVarios();
        } else if (tablero.getSeleccionado() != null) {
            construirParaNodo(tablero.getSeleccionado());
        } else {
            construirVacio();
        }
        revalidate();
        repaint();
    }

    // --- Las cuatro caras ---
    private void construirVacio() {
        add(titulo("Nada seleccionado"), BorderLayout.NORTH);
        add(ayuda("Arrastra un simbolo desde la paleta.<br><br>"
                + "Arrastra sobre el fondo para seleccionar varios a la vez, o "
                + "haz clic con Ctrl para irlos sumando.<br><br>"
                + "La rueda del raton acerca y aleja."), BorderLayout.CENTER);
    }

    private void construirParaVarios() {
        int cuantos = tablero.getSeleccionados().size();
        add(titulo(cuantos + " elementos seleccionados"), BorderLayout.NORTH);
        add(ayuda("Arrastra cualquiera de ellos para mover el grupo entero."),
                BorderLayout.CENTER);
        add(botonEliminar("Eliminar los " + cuantos), BorderLayout.SOUTH);
    }

    // Un enlace solo se puede borrar.
    private void construirParaEnlace() {
        Enlace enlace = tablero.getEnlaceSeleccionado();
        add(titulo("Enlace"), BorderLayout.NORTH);
        add(ayuda(enlace.getOrigen().getNombre() + "  &mdash;  "
                + enlace.getDestino().getNombre()), BorderLayout.CENTER);
        add(botonEliminar("Eliminar enlace"), BorderLayout.SOUTH);
    }

    private void construirParaNodo(NodoVista nodo) {
        add(titulo(nodo.getTipo().toString()), BorderLayout.NORTH);

        // Los escuchadores van despues de fijar los valores: sin banderas ni disparos en cadena.
        JTextField campoNombre = new JTextField(nodo.getNombre());
        campoNombre.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                renombrar(nodo, campoNombre.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                renombrar(nodo, campoNombre.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                renombrar(nodo, campoNombre.getText());
            }
        });

        JComboBox<TipoNodo> selectorTipo = new JComboBox<TipoNodo>(TipoNodo.values());
        selectorTipo.setSelectedItem(nodo.getTipo());
        selectorTipo.addActionListener(e -> {
            nodo.setTipo((TipoNodo) selectorTipo.getSelectedItem());
            lienzo.repaint();
        });

        JPanel formulario = new JPanel(new GridLayout(0, 1, 0, 4));
        formulario.setOpaque(false);
        formulario.add(rotulo("Nombre"));
        formulario.add(campoNombre);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Tipo"));
        formulario.add(selectorTipo);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(listaDeRelaciones(nodo), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(botonEliminar("Eliminar elemento"), BorderLayout.SOUTH);
    }

    // --- Lista de relaciones ---
    private JPanel listaDeRelaciones(NodoVista nodo) {
        JPanel caja = new JPanel(new BorderLayout(0, 4));
        caja.setOpaque(false);
        caja.add(rotulo("Relaciones"), BorderLayout.NORTH);

        JPanel filas = new JPanel();
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        filas.setOpaque(false);

        List<Enlace> suyos = tablero.enlacesDe(nodo);
        List<NodoVista> atributos = tablero.atributosDe(nodo);
        boolean tienePadre = (nodo.getPadre() != null);

        if (suyos.isEmpty() && atributos.isEmpty() && !tienePadre) {
            JLabel ninguna = new JLabel("Sin relaciones todavia");
            ninguna.setForeground(Tema.TENUE);
            ninguna.setFont(Tema.MENUDA);
            ninguna.setAlignmentX(Component.LEFT_ALIGNMENT);
            filas.add(ninguna);
        }
        for (Enlace enlace : suyos) {
            filas.add(fila(tablero.otroExtremo(enlace, nodo).getNombre(), "enlace",
                    "Quitar este enlace", e -> tablero.eliminarEnlace(enlace)));
            filas.add(Box.createVerticalStrut(4));
        }
        for (NodoVista atributo : atributos) {
            filas.add(fila(atributo.getNombre(), "atributo",
                    "Eliminar este atributo", e -> tablero.eliminarNodo(atributo)));
            filas.add(Box.createVerticalStrut(4));
        }
        if (tienePadre) {
            // sin equis: un atributo sin duenno no significa nada
            filas.add(fila(nodo.getPadre().getNombre(), "duenno", null, null));
            filas.add(Box.createVerticalStrut(4));
        }

        JScrollPane desplazable = new JScrollPane(filas);
        desplazable.setBorder(BorderFactory.createEmptyBorder());
        desplazable.setOpaque(false);
        desplazable.getViewport().setOpaque(false);
        caja.add(desplazable, BorderLayout.CENTER);
        return caja;
    }

    // La clase dice que union es; si la accion es nula, no se dibuja la equis.
    private JPanel fila(String nombre, String clase, String consejo,
                        ActionListener accion) {
        JPanel fila = new JPanel(new BorderLayout(6, 0));
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(4, 8, 4, 4)));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel(new BorderLayout(6, 0));
        textos.setOpaque(false);
        JLabel etiquetaNombre = new JLabel(nombre);
        etiquetaNombre.setForeground(Tema.TEXTO);
        JLabel etiquetaClase = new JLabel(clase);
        etiquetaClase.setForeground(Tema.TENUE);
        etiquetaClase.setFont(etiquetaClase.getFont().deriveFont(10f));
        textos.add(etiquetaNombre, BorderLayout.CENTER);
        textos.add(etiquetaClase, BorderLayout.EAST);
        fila.add(textos, BorderLayout.CENTER);

        if (accion != null) {
            JButton quitar = new JButton("\u00D7");
            quitar.setToolTipText(consejo);
            quitar.setMargin(new java.awt.Insets(0, 4, 0, 4));
            quitar.setFocusable(false);
            quitar.addActionListener(accion);
            fila.add(quitar, BorderLayout.EAST);
        }
        return fila;
    }

    // --- Piezas comunes ---
    private JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TENUE);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return l;
    }

    private JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TENUE);
        return l;
    }

    private JLabel ayuda(String html) {
        JLabel l = new JLabel("<html><body style='width:220px'>" + html + "</body></html>");
        l.setForeground(Tema.TENUE);
        l.setVerticalAlignment(JLabel.TOP);
        return l;
    }

    private JButton botonEliminar(String texto) {
        JButton b = new JButton(texto);
        b.setToolTipText("Tambien con Suprimir o Retroceso");
        b.addActionListener(e -> tablero.eliminarSeleccion());
        return b;
    }

    // No pasa por avisar(): reconstruiria el panel a media escritura.
    private void renombrar(NodoVista nodo, String texto) {
        nodo.setNombre(texto);
        lienzo.repaint();
    }
}
