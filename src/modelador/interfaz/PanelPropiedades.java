package modelador.interfaz;

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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.ElementoDelModelo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Marca;
import modelador.dominio.er.Modalidad;
import modelador.dominio.er.Naturaleza;
import modelador.dominio.er.Relacion;
import modelador.dominio.tipos.TipoDato;

// Cuatro caras segun la seleccion: nada, una figura, varias o un enlace.
// Se reconstruye entero cada vez que cambia, para no dejar estados a medias.
public class PanelPropiedades extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Tablero tablero;
    private final transient LienzoER lienzo;

    public PanelPropiedades(LienzoER lienzo) {
        this.lienzo = lienzo;
        this.tablero = lienzo.getTablero();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(280, 0));
        setBackground(Tema.PANEL);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        tablero.alCambiar(this::reconstruir);
        reconstruir();
    }

    private void reconstruir() {
        removeAll();
        if (tablero.getEnlaceSeleccionado() != null) {
            construirParaEnlace();
        } else if (tablero.getSeleccionados().size() > 1) {
            construirParaVarios();
        } else if (tablero.getSeleccionado() != null) {
            construirParaFigura(tablero.getSeleccionado());
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
                + "Arrastra sobre el fondo para seleccionar varios, o haz clic con "
                + "Ctrl para irlos sumando.<br><br>"
                + "Para enlazar, selecciona una relacion y tira del asa hasta una "
                + "entidad."), BorderLayout.CENTER);
    }

    private void construirParaVarios() {
        int cuantos = tablero.getSeleccionados().size();
        add(titulo(cuantos + " elementos seleccionados"), BorderLayout.NORTH);
        add(ayuda("Arrastra cualquiera de ellos para mover el grupo entero."),
                BorderLayout.CENTER);
        add(botonEliminar("Eliminar los " + cuantos), BorderLayout.SOUTH);
    }

    // Un enlace es una participacion: aqui se le ponen cardinalidad y modalidad.
    private void construirParaEnlace() {
        EnlaceVista enlace = tablero.getEnlaceSeleccionado();
        add(titulo("Participacion"), BorderLayout.NORTH);

        JComboBox<Cardinalidad> cardinalidad =
                new JComboBox<>(Cardinalidad.values());
        cardinalidad.setSelectedItem(enlace.getParte().getCardinalidad());
        cardinalidad.addActionListener(e -> {
            enlace.getParte().setCardinalidad(
                    (Cardinalidad) cardinalidad.getSelectedItem());
            tablero.avisar();
        });

        JComboBox<Modalidad> modalidad = new JComboBox<>(Modalidad.values());
        modalidad.setSelectedItem(enlace.getParte().getModalidad());
        modalidad.addActionListener(e -> {
            enlace.getParte().setModalidad((Modalidad) modalidad.getSelectedItem());
            tablero.avisar();
        });

        JPanel formulario = new JPanel(new GridLayout(0, 1, 0, 4));
        formulario.setOpaque(false);
        formulario.add(rotulo(enlace.getRelacion().getNombre() + "  -  "
                + enlace.getEntidad().getNombre()));
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Cardinalidad"));
        formulario.add(cardinalidad);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Participacion"));
        formulario.add(modalidad);

        add(arriba(formulario), BorderLayout.CENTER);
        add(botonEliminar("Quitar la participacion"), BorderLayout.SOUTH);
    }

    private void construirParaFigura(Figura figura) {
        add(titulo(nombreDelTipo(figura)), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(0, 1, 0, 4));
        formulario.setOpaque(false);
        formulario.add(rotulo("Nombre"));
        formulario.add(campoNombre(figura));
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Tipo"));
        formulario.add(selectorDeTipo(figura));

        if (figura.esAtributo()) {
            agregarCamposDeAtributo(formulario, (Atributo) figura.getElemento());
        }

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(listaDeRelaciones(figura), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(botonEliminar("Eliminar elemento"), BorderLayout.SOUTH);
    }

    // --- Piezas del formulario ---

    private JTextField campoNombre(Figura figura) {
        JTextField campo = new JTextField(figura.getNombre());
        campo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                renombrar(figura, campo.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                renombrar(figura, campo.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                renombrar(figura, campo.getText());
            }
        });
        return campo;
    }

    // Solo se ofrecen las variantes de la misma familia: una entidad no puede
    // convertirse en relacion, son clases distintas del modelo.
    private JComboBox<TipoNodo> selectorDeTipo(Figura figura) {
        TipoNodo[] opciones;
        if (figura.esEntidad()) {
            opciones = new TipoNodo[] {TipoNodo.ENTIDAD, TipoNodo.ENTIDAD_DEBIL};
        } else if (figura.esRelacion()) {
            opciones = new TipoNodo[] {TipoNodo.RELACION,
                    TipoNodo.RELACION_IDENTIFICADORA};
        } else {
            opciones = new TipoNodo[] {TipoNodo.ATRIBUTO, TipoNodo.ATRIBUTO_CLAVE};
        }
        JComboBox<TipoNodo> selector = new JComboBox<>(opciones);
        selector.setSelectedItem(figura.getTipo());
        selector.addActionListener(e ->
                cambiarTipo(figura, (TipoNodo) selector.getSelectedItem()));
        return selector;
    }

    private void cambiarTipo(Figura figura, TipoNodo tipo) {
        ElementoDelModelo elemento = figura.getElemento();
        if (elemento instanceof Entidad) {
            ((Entidad) elemento).setDebil(tipo == TipoNodo.ENTIDAD_DEBIL);
        } else if (elemento instanceof Relacion) {
            ((Relacion) elemento).setIdentificadora(
                    tipo == TipoNodo.RELACION_IDENTIFICADORA);
        } else {
            ((Atributo) elemento).marcar(Marca.CLAVE, tipo == TipoNodo.ATRIBUTO_CLAVE);
        }
        lienzo.repaint();
    }

    private void agregarCamposDeAtributo(JPanel formulario, Atributo atributo) {
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Tipo de dato"));
        JComboBox<TipoDato> tipos = new JComboBox<>(TipoDato.values());
        tipos.setSelectedItem(atributo.getTipo());
        tipos.addActionListener(e -> {
            atributo.setTipo((TipoDato) tipos.getSelectedItem());
            lienzo.repaint();
        });
        formulario.add(tipos);

        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Naturaleza"));
        JComboBox<Naturaleza> naturaleza = new JComboBox<>(Naturaleza.values());
        naturaleza.setSelectedItem(atributo.getNaturaleza());
        naturaleza.addActionListener(e -> {
            atributo.setNaturaleza((Naturaleza) naturaleza.getSelectedItem());
            lienzo.repaint();
        });
        formulario.add(naturaleza);

        formulario.add(Box.createVerticalStrut(8));
        formulario.add(rotulo("Restricciones"));
        for (Marca marca : Marca.values()) {
            JCheckBox casilla = new JCheckBox(marca.name().toLowerCase(),
                    atributo.getMarcas().contains(marca));
            casilla.setForeground(Tema.TENUE);
            casilla.setOpaque(false);
            casilla.addActionListener(e -> {
                atributo.marcar(marca, casilla.isSelected());
                tablero.avisar();
            });
            formulario.add(casilla);
        }
    }

    // --- Lista de relaciones ---

    private JPanel listaDeRelaciones(Figura figura) {
        JPanel caja = new JPanel(new BorderLayout(0, 4));
        caja.setOpaque(false);
        caja.add(rotulo("Relaciones"), BorderLayout.NORTH);

        JPanel filas = new JPanel();
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        filas.setOpaque(false);

        List<EnlaceVista> enlaces = tablero.enlacesDe(figura);
        List<Figura> atributos = tablero.atributosDe(figura);
        boolean tieneDuenno = figura.esAtributo();

        if (enlaces.isEmpty() && atributos.isEmpty() && !tieneDuenno) {
            JLabel ninguna = new JLabel("Sin relaciones todavia");
            ninguna.setForeground(Tema.TENUE);
            ninguna.setFont(Tema.MENUDA);
            ninguna.setAlignmentX(Component.LEFT_ALIGNMENT);
            filas.add(ninguna);
        }
        for (EnlaceVista enlace : enlaces) {
            String otro = tablero.otroExtremo(enlace, figura).getNombre();
            String clase = enlace.getParte().getCardinalidad() == Cardinalidad.UNO
                    ? "1" : "N";
            filas.add(fila(otro, clase, "Quitar esta participacion",
                    e -> tablero.eliminarEnlace(enlace)));
            filas.add(Box.createVerticalStrut(4));
        }
        for (Figura atributo : atributos) {
            filas.add(fila(atributo.getNombre(), "atributo", "Eliminar este atributo",
                    e -> tablero.eliminar(List.of(atributo))));
            filas.add(Box.createVerticalStrut(4));
        }
        if (tieneDuenno) {
            // sin equis: un atributo sin duenno no significa nada
            filas.add(fila(figura.getDuenno().getNombre(), "duenno", null, null));
            filas.add(Box.createVerticalStrut(4));
        }

        JScrollPane desplazable = new JScrollPane(filas);
        desplazable.setBorder(BorderFactory.createEmptyBorder());
        desplazable.setOpaque(false);
        desplazable.getViewport().setOpaque(false);
        caja.add(desplazable, BorderLayout.CENTER);
        return caja;
    }

    // La clase dice de que union se trata; sin accion no se dibuja la equis.
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

    private String nombreDelTipo(Figura figura) {
        if (figura.esEntidad()) {
            return ((Entidad) figura.getElemento()).esDebil()
                    ? "Entidad debil" : "Entidad";
        }
        if (figura.esRelacion()) {
            return ((Relacion) figura.getElemento()).esIdentificadora()
                    ? "Relacion identificadora" : "Relacion";
        }
        return "Atributo";
    }

    private JPanel arriba(JPanel contenido) {
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.add(contenido, BorderLayout.NORTH);
        return envoltura;
    }

    private JLabel titulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(Tema.TENUE);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 11f));
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return etiqueta;
    }

    private JLabel rotulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(Tema.TENUE);
        return etiqueta;
    }

    private JLabel ayuda(String html) {
        JLabel etiqueta = new JLabel("<html><body style='width:220px'>"
                + html + "</body></html>");
        etiqueta.setForeground(Tema.TENUE);
        etiqueta.setVerticalAlignment(JLabel.TOP);
        return etiqueta;
    }

    private JButton botonEliminar(String texto) {
        JButton boton = new JButton(texto);
        boton.setToolTipText("Tambien con Suprimir o Retroceso");
        boton.addActionListener(e -> {
            tablero.eliminarSeleccion();
            String aviso = tablero.recogerAviso();
            if (aviso != null) {
                javax.swing.JOptionPane.showMessageDialog(this, aviso, "Aviso",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return boton;
    }

    // No pasa por avisar(): reconstruiria el panel a media escritura.
    private void renombrar(Figura figura, String texto) {
        figura.getElemento().setNombre(texto);
        lienzo.repaint();
    }
}
