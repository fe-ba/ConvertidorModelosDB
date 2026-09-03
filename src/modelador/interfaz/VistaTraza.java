package modelador.interfaz;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import modelador.conversion.Traza;
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.Severidad;

// Explica que regla se aplico a cada elemento y que avisos dejo la conversion.
// Sin esto, el conversor era una caja negra: producia tablas sin decir por que.
public class VistaTraza extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextPane texto;

    public VistaTraza() {
        setLayout(new BorderLayout());
        setBackground(Tema.FONDO);
        texto = new JTextPane();
        texto.setEditable(false);
        texto.setBackground(Tema.FONDO);
        texto.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel titulo = new JLabel("Reglas aplicadas en la ultima conversion");
        titulo.setForeground(Tema.TENUE);
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        add(titulo, BorderLayout.NORTH);
        add(new JScrollPane(texto), BorderLayout.CENTER);
    }

    public void mostrar(List<Traza> traza, List<Aviso> avisos) {
        texto.setText("");
        StyledDocument documento = texto.getStyledDocument();
        if (traza.isEmpty() && avisos.isEmpty()) {
            escribir(documento, "Convierte el diagrama E-R (F9) para ver aqui el "
                    + "razonamiento de la conversion.\n", Tema.TENUE, false);
            return;
        }
        for (Traza paso : traza) {
            escribir(documento, etiqueta(paso) + "  ", Tema.TEAL, true);
            escribir(documento, paso.getExplicacion() + "\n", Tema.TEXTO, false);
        }
        if (avisos.isEmpty()) {
            return;
        }
        escribir(documento, "\nAvisos\n", Tema.TENUE, true);
        for (Aviso aviso : avisos) {
            boolean error = aviso.getSeveridad() == Severidad.ERROR;
            escribir(documento, (error ? "error" : "aviso") + "  ",
                    error ? Tema.ORO : Tema.TENUE, true);
            escribir(documento, aviso.getElemento() + ": " + aviso.getMensaje() + "\n",
                    Tema.TEXTO, false);
        }
    }

    // MUCHOS_A_MUCHOS -> "muchos a muchos"
    private String etiqueta(Traza paso) {
        return paso.getRegla().name().toLowerCase().replace('_', ' ');
    }

    private void escribir(StyledDocument documento, String contenido,
                          java.awt.Color color, boolean negrita) {
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, color);
        StyleConstants.setBold(estilo, negrita);
        StyleConstants.setFontFamily(estilo, Tema.CODIGO.getFamily());
        try {
            documento.insertString(documento.getLength(), contenido, estilo);
        } catch (BadLocationException e) {
            throw new IllegalStateException("No se pudo escribir la traza", e);
        }
    }
}
