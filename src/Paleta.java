import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

// Barra lateral plegable con los simbolos, arrastrables al lienzo.
public class Paleta extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color FONDO = Tema.PANEL;
    private static final Color BORDE = Tema.BORDE;
    private static final Color TEXTO = Tema.TEXTO;
    private static final Color TENUE = Tema.TENUE;
    private static final int ANCHO_ABIERTA = 210;
    private static final int ANCHO_PLEGADA = 34;

    private JPanel listaSimbolos;
    private JButton botonPlegar;
    private boolean plegada;

    public Paleta(LienzoER lienzo) {
        this.plegada = false;
        setLayout(new BorderLayout());
        setBackground(FONDO);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDE));
        setPreferredSize(new Dimension(ANCHO_ABIERTA, 0));

        botonPlegar = new JButton("\u25C0  Simbolos");
        botonPlegar.setFocusable(false);
        botonPlegar.addActionListener(e -> alternarPlegado());

        listaSimbolos = new JPanel();
        listaSimbolos.setLayout(new BoxLayout(listaSimbolos, BoxLayout.Y_AXIS));
        listaSimbolos.setBackground(FONDO);
        listaSimbolos.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        agregarSimbolo(lienzo, TipoNodo.ENTIDAD, "Entidad", "Sera una tabla");
        agregarSimbolo(lienzo, TipoNodo.ENTIDAD_DEBIL, "Entidad debil",
                "Depende de otra");
        agregarSimbolo(lienzo, TipoNodo.RELACION, "Relacion", "Une entidades");
        agregarSimbolo(lienzo, TipoNodo.RELACION_IDENTIFICADORA, "R. identificadora",
                "Da clave a la debil");
        agregarSimbolo(lienzo, TipoNodo.ATRIBUTO, "Atributo", "Sueltalo sobre un nodo");
        agregarSimbolo(lienzo, TipoNodo.ATRIBUTO_CLAVE, "Atributo clave",
                "Forma la clave primaria");
        listaSimbolos.add(Box.createVerticalGlue());

        JLabel pista = new JLabel("<html><body style='width:170px'>"
                + "Arrastra un simbolo al lienzo.</body></html>");
        pista.setForeground(TENUE);
        pista.setFont(pista.getFont().deriveFont(11f));
        pista.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));

        add(botonPlegar, BorderLayout.NORTH);
        add(listaSimbolos, BorderLayout.CENTER);
        add(pista, BorderLayout.SOUTH);
    }

    private void alternarPlegado() {
        plegada = !plegada;
        listaSimbolos.setVisible(!plegada);
        botonPlegar.setText(plegada ? "\u25B6" : "\u25C0  Simbolos");
        setPreferredSize(new Dimension(
                plegada ? ANCHO_PLEGADA : ANCHO_ABIERTA, 0));
        revalidate();
        repaint();
    }

    // El dato que viaja al arrastrar es el nombre de la constante de TipoNodo.
    private void agregarSimbolo(LienzoER lienzo, TipoNodo tipo, String titulo,
                                String descripcion) {
        JPanel ficha = new JPanel(new BorderLayout(8, 0));
        ficha.setBackground(Tema.FICHA);
        ficha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        ficha.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ficha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        ficha.setToolTipText("Arrastralo al lienzo, o haz doble clic para "
                + "ponerlo en el centro");

        JLabel icono = new JLabel(new IconoSimbolo(tipo));

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        JLabel arriba = new JLabel(titulo);
        arriba.setForeground(TEXTO);
        arriba.setFont(arriba.getFont().deriveFont(Font.BOLD, 12f));
        JLabel abajo = new JLabel(descripcion);
        abajo.setForeground(TENUE);
        abajo.setFont(abajo.getFont().deriveFont(10f));
        textos.add(arriba);
        textos.add(abajo);

        ficha.add(icono, BorderLayout.WEST);
        ficha.add(textos, BorderLayout.CENTER);

        ficha.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                // la imagen viaja pegada al cursor mientras se arrastra
                setDragImage(miniatura(tipo));
                setDragImageOffset(new Point(21, 15));
                return new StringSelection(tipo.name());
            }
        });

        ficha.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    lienzo.soltarEnElCentro(tipo);
                }
            }
        });
        ficha.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JComponent origen = (JComponent) e.getSource();
                origen.getTransferHandler().exportAsDrag(origen, e, TransferHandler.COPY);
            }
        });

        listaSimbolos.add(ficha);
        listaSimbolos.add(Box.createVerticalStrut(6));
    }

    // Imagen que Swing lleva pegada al cursor durante el arrastre.
    private BufferedImage miniatura(TipoNodo tipo) {
        IconoSimbolo icono = new IconoSimbolo(tipo);
        BufferedImage img = new BufferedImage(icono.getIconWidth(),
                icono.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icono.paintIcon(this, g, 0, 0);
        g.dispose();
        return img;
    }
}
