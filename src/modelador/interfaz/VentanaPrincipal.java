package modelador.interfaz;

import java.awt.BorderLayout;
import java.io.File;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import modelador.aplicacion.Fachada;
import modelador.dominio.tipos.Aviso;
import modelador.generacion.Destino;

// Ventana principal: menu, tres pestanas y el lienzo.
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient Fachada fachada;
    private transient Tablero tablero;
    private LienzoER lienzo;
    private JPanel paginaER;
    private final LienzoRelacional lienzoRelacional;
    private final VistaCodigo vistaCodigo;
    private JComboBox<Destino> destinos;
    private VistaTraza vistaTraza;
    private JTabbedPane pestanas;

    public VentanaPrincipal() {
        super("Modelador E-R");
        this.fachada = new Fachada();
        this.lienzoRelacional = new LienzoRelacional();
        this.vistaCodigo = new VistaCodigo();
        this.vistaTraza = new VistaTraza();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setJMenuBar(construirMenu());

        pestanas = new JTabbedPane();
        paginaER = new JPanel(new BorderLayout());
        montarLienzo();
        pestanas.addTab("Diagrama E-R", paginaER);
        pestanas.addTab("Diagrama relacional", lienzoRelacional);
        pestanas.addTab("Codigo", montarPaginaCodigo());
        pestanas.addTab("Traza", vistaTraza);
        pestanas.addChangeListener(this::alCambiarPestana);
        add(pestanas, BorderLayout.CENTER);
    }

    // Se rehace al abrir un archivo: el lienzo apunta a un modelo nuevo.
    private void montarLienzo() {
        paginaER.removeAll();
        tablero = new Tablero(fachada.getModelo());
        lienzo = new LienzoER(tablero);
        paginaER.add(new Paleta(lienzo), BorderLayout.WEST);
        paginaER.add(lienzo, BorderLayout.CENTER);
        paginaER.add(new PanelPropiedades(lienzo), BorderLayout.EAST);
        paginaER.revalidate();
        paginaER.repaint();
    }

    private JPanel montarPaginaCodigo() {
        JPanel panel = new JPanel(new BorderLayout());
        destinos = new JComboBox<>(Destino.values());
        destinos.addActionListener(e -> generarCodigo());
        JButton exportar = new JButton("Exportar...");
        exportar.setToolTipText("Guarda el codigo generado en un archivo");
        exportar.addActionListener(e -> exportarCodigo());
        JPanel controles = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 6));
        controles.add(destinos);
        controles.add(exportar);
        JPanel superior = new JPanel(new BorderLayout());
        superior.add(controles, BorderLayout.WEST);
        panel.add(superior, BorderLayout.NORTH);
        panel.add(new JScrollPane(vistaCodigo), BorderLayout.CENTER);
        return panel;
    }

    private JMenuBar construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(opcion("Nuevo", "control N", e -> nuevo()));
        archivo.add(opcion("Abrir...", "control O", e -> abrir()));
        archivo.add(opcion("Guardar como...", "control S", e -> guardar()));
        archivo.addSeparator();
        archivo.add(opcion("Salir", "control Q", e -> dispose()));
        barra.add(archivo);

        JMenu modelo = new JMenu("Modelo");
        modelo.add(opcion("Validar", "F8", e -> validar()));
        modelo.add(opcion("Convertir a relacional", "F9", e -> convertir()));
        barra.add(modelo);
        return barra;
    }

    private JMenuItem opcion(String texto, String atajo,
                             java.awt.event.ActionListener accion) {
        JMenuItem opcion = new JMenuItem(texto);
        opcion.setAccelerator(KeyStroke.getKeyStroke(atajo));
        opcion.addActionListener(accion);
        return opcion;
    }

    // --- Acciones ---

    private void nuevo() {
        fachada.nuevo();
        montarLienzo();
    }

    private void abrir() {
        JFileChooser dialogo = new JFileChooser();
        dialogo.setFileFilter(new FileNameExtensionFilter("Modelo E-R (*.json)",
                fachada.getExtensionDeModelo()));
        if (dialogo.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            fachada.abrir(dialogo.getSelectedFile().getAbsolutePath());
            montarLienzo();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el archivo.\n" + e.getMessage(),
                    "Error al abrir", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        JFileChooser dialogo = new JFileChooser();
        dialogo.setSelectedFile(new File(fachada.getRutaActual() != null
                ? fachada.getRutaActual() : "modelo.json"));
        if (dialogo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String ruta = dialogo.getSelectedFile().getAbsolutePath();
        if (!ruta.endsWith("." + fachada.getExtensionDeModelo())) {
            ruta = ruta + "." + fachada.getExtensionDeModelo();
        }
        try {
            fachada.guardar(ruta);
            setTitle("Modelador E-R - " + ruta);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar.\n" + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Exportar es generar y ademas escribir el archivo, con la extension que
    // corresponda al destino elegido.
    private void exportarCodigo() {
        Destino destino = (Destino) destinos.getSelectedItem();
        if (fachada.getEsquema() == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero convierte el diagrama E-R (F9).",
                    "Todavia no hay codigo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser dialogo = new JFileChooser();
        dialogo.setSelectedFile(new File(fachada.nombreDeArchivo(destino)));
        if (dialogo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String ruta = dialogo.getSelectedFile().getAbsolutePath();
        String extension = "." + destino.getExtension();
        if (!ruta.endsWith(extension)) {
            ruta = ruta + extension;
        }
        try {
            fachada.exportarCodigo(destino, ruta);
            JOptionPane.showMessageDialog(this, "Codigo exportado a\n" + ruta,
                    "Exportado", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo exportar.\n" + e.getMessage(),
                    "Error al exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validar() {
        List<Aviso> avisos = fachada.validar();
        if (avisos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El modelo es valido.",
                    "Validacion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Aviso aviso : avisos) {
            sb.append("- ").append(aviso.getMensaje()).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(),
                avisos.size() + " avisos", JOptionPane.WARNING_MESSAGE);
    }

    // Convierte el modelo actual en el esquema relacional, muestra el esquema
    // en su pestana y el codigo generado en la pestana de codigo.
    private void convertir() {
        List<Aviso> avisos = fachada.convertir();
        lienzoRelacional.setModelo(fachada.getModelo());
        lienzoRelacional.mostrar(fachada.getEsquema());
        vistaTraza.mostrar(fachada.getTraza(), avisos);
        generarCodigo();

        List<Aviso> errores = new java.util.ArrayList<>();
        List<Aviso> otros = new java.util.ArrayList<>();
        for (Aviso aviso : avisos) {
            if (aviso.esError()) {
                errores.add(aviso);
            } else {
                otros.add(aviso);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Aviso aviso : otros) {
            sb.append("[AVISO] ").append(aviso.getMensaje()).append("\n");
        }
        for (Aviso aviso : errores) {
            sb.append("[ERROR] ").append(aviso.getMensaje()).append("\n");
        }
        if (!errores.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    sb.length() == 0 ? "Conversion con errores." : sb.toString(),
                    errores.size() + " error(es) de conversion",
                    JOptionPane.ERROR_MESSAGE);
        } else if (!otros.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    sb.toString(),
                    otros.size() + " advertencia(s)",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            lienzoRelacional.setModelo(fachada.getModelo());
        lienzoRelacional.mostrar(fachada.getEsquema());
        vistaTraza.mostrar(fachada.getTraza(), avisos);
        }
    }

    // Refresca la vista al entrar en una pestana de resultado, por si el
    // diagrama cambió sin pulsar "Convertir".
    private void alCambiarPestana(ChangeEvent evento) {
        int indice = pestanas.getSelectedIndex();
        if (indice == 1 || indice == 2) {
            convertir();
        }
    }

    private void generarCodigo() {
        if (destinos == null) {
            return;
        }
        Destino destino = (Destino) destinos.getSelectedItem();
        if (destino == null) {
            return;
        }
        try {
            vistaCodigo.mostrar(fachada.generarCodigo(destino));
        } catch (IllegalStateException e) {
            vistaCodigo.mostrar("-- " + e.getMessage());
        }
    }

}
