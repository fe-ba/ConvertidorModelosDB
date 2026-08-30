import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

// Tres pestanas; las dos ultimas esperan al conversor y al generador.
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private transient Tablero tablero;
    private LienzoER lienzo;
    private JTextArea areaCodigo;

    public VentanaPrincipal() {
        super("Modelador E-R");
        this.tablero = new Tablero();
        this.lienzo = new LienzoER(tablero);
        this.areaCodigo = new JTextArea(
                "-- Aqui saldra el DDL cuando esten listos el conversor\n"
                + "-- y el generador de codigo.");
        areaCodigo.setEditable(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);

        JPanel paginaER = new JPanel(new BorderLayout());
        paginaER.add(new Paleta(lienzo), BorderLayout.WEST);
        paginaER.add(lienzo, BorderLayout.CENTER);
        paginaER.add(new PanelPropiedades(lienzo), BorderLayout.EAST);

        JPanel paginaRelacional = new JPanel(new BorderLayout());
        paginaRelacional.setBackground(Tema.FONDO);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Diagrama E-R", paginaER);
        pestanas.addTab("Diagrama relacional", paginaRelacional);
        pestanas.addTab("Codigo", new JScrollPane(areaCodigo));

        add(pestanas, BorderLayout.CENTER);
    }
}
