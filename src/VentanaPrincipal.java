import java.awt.BorderLayout;
import java.io.File;
import java.util.List;
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
import javax.swing.filechooser.FileNameExtensionFilter;

// Ventana principal: menu, tres pestanas y el lienzo.
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient Fachada fachada;
    private transient Tablero tablero;
    private LienzoER lienzo;
    private JPanel paginaER;
    private final JTextArea areaCodigo;

    public VentanaPrincipal() {
        super("Modelador E-R");
        this.fachada = new Fachada();
        this.areaCodigo = new JTextArea(
                "-- El codigo aparecera aqui cuando el conversor este listo.");
        areaCodigo.setEditable(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setJMenuBar(construirMenu());

        JTabbedPane pestanas = new JTabbedPane();
        paginaER = new JPanel(new BorderLayout());
        montarLienzo();
        pestanas.addTab("Diagrama E-R", paginaER);

        JPanel paginaRelacional = new JPanel(new BorderLayout());
        paginaRelacional.setBackground(Tema.FONDO);
        pestanas.addTab("Diagrama relacional", paginaRelacional);
        pestanas.addTab("Codigo", new JScrollPane(areaCodigo));
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
}
