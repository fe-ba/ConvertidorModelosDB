import java.awt.Color;
import java.util.Set;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// Muestra el codigo generado con coloreado. Reconoce SQL, Python y TypeScript,
// que son los tres lenguajes que produce el generador.
public class VistaCodigo extends JTextPane {

    private static final long serialVersionUID = 1L;

    private static final Color COMENTARIO = new Color(0x6A, 0x7D, 0x87);
    private static final Color PALABRA = new Color(0xC7, 0x8D, 0xFF);
    private static final Color TIPO = new Color(0x4C, 0xC2, 0xD6);
    private static final Color CADENA = new Color(0xA8, 0xD8, 0x8A);
    private static final Color NUMERO = new Color(0xF0, 0xB4, 0x29);
    private static final Color NORMAL = new Color(0xDF, 0xE6, 0xE4);

    private static final Set<String> PALABRAS = Set.of(
            "CREATE", "TABLE", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT",
            "NOT", "NULL", "UNIQUE", "ON", "DELETE", "UPDATE", "CASCADE", "RESTRICT",
            "SET", "ACTION", "PRAGMA", "AUTOINCREMENT", "IDENTITY", "AUTO_INCREMENT",
            "class", "import", "from", "export", "interface", "def", "return",
            "True", "False", "None");

    private static final Set<String> TIPOS = Set.of(
            "INTEGER", "INT", "BIGINT", "SERIAL", "VARCHAR", "NVARCHAR", "TEXT",
            "NUMERIC", "DECIMAL", "DOUBLE", "PRECISION", "FLOAT", "REAL", "BOOLEAN",
            "TINYINT", "BIT", "DATE", "TIMESTAMP", "DATETIME", "DATETIME2",
            "Column", "String", "Integer", "Boolean", "Numeric", "ForeignKey",
            "UniqueConstraint", "Base", "number", "string", "boolean");

    public VistaCodigo() {
        setEditable(false);
        setFont(Tema.CODIGO);
        setBackground(Tema.FONDO);
        setForeground(NORMAL);
        setCaretColor(NORMAL);
    }

    // Sin ajuste de linea: una sentencia larga se lee mejor con barra
    // horizontal que partida por la mitad.
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null
                && getUI().getPreferredSize(this).width <= getParent().getSize().width;
    }

    public void mostrar(String codigo) {
        setText("");
        StyledDocument documento = getStyledDocument();
        for (String linea : codigo.split("\n", -1)) {
            colorearLinea(documento, linea + "\n");
        }
        setCaretPosition(0);
    }

    // Un comentario tine la linea entera; si no, se parte en piezas.
    private void colorearLinea(StyledDocument documento, String linea) {
        String limpia = linea.stripLeading();
        if (limpia.startsWith("--") || limpia.startsWith("//") || limpia.startsWith("#")) {
            anadir(documento, linea, COMENTARIO, true);
            return;
        }
        int i = 0;
        while (i < linea.length()) {
            char c = linea.charAt(i);
            if (c == '"' || c == '\'') {
                int fin = linea.indexOf(c, i + 1);
                fin = (fin < 0) ? linea.length() - 1 : fin;
                anadir(documento, linea.substring(i, fin + 1), CADENA, false);
                i = fin + 1;
            } else if (Character.isLetter(c) || c == '_') {
                int fin = i;
                while (fin < linea.length()
                        && (Character.isLetterOrDigit(linea.charAt(fin))
                            || linea.charAt(fin) == '_')) {
                    fin++;
                }
                String palabra = linea.substring(i, fin);
                anadir(documento, palabra, colorDe(palabra), PALABRAS.contains(palabra));
                i = fin;
            } else if (Character.isDigit(c)) {
                int fin = i;
                while (fin < linea.length()
                        && (Character.isDigit(linea.charAt(fin))
                            || linea.charAt(fin) == '.')) {
                    fin++;
                }
                anadir(documento, linea.substring(i, fin), NUMERO, false);
                i = fin;
            } else {
                anadir(documento, String.valueOf(c), NORMAL, false);
                i++;
            }
        }
    }

    private Color colorDe(String palabra) {
        if (PALABRAS.contains(palabra)) {
            return PALABRA;
        }
        if (TIPOS.contains(palabra)) {
            return TIPO;
        }
        return NORMAL;
    }

    private void anadir(StyledDocument documento, String texto, Color color,
                        boolean negrita) {
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, color);
        StyleConstants.setBold(estilo, negrita);
        try {
            documento.insertString(documento.getLength(), texto, estilo);
        } catch (BadLocationException e) {
            throw new IllegalStateException("No se pudo pintar el codigo", e);
        }
    }
}
