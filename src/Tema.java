import java.awt.Color;
import java.awt.Font;

// Colores y fuentes de toda la aplicacion, en un solo sitio.
public final class Tema {

    private Tema() {
    }

    // --- fondos ---
    public static final Color FONDO = new Color(0x12, 0x17, 0x1A);
    public static final Color PANEL = new Color(0x17, 0x1D, 0x21);
    public static final Color FICHA = new Color(0x14, 0x1A, 0x1E);
    public static final Color REJILLA = new Color(0x21, 0x29, 0x30);

    // --- relleno de los simbolos ---
    public static final Color RELLENO_ENTIDAD = new Color(0x1B, 0x22, 0x26);
    public static final Color RELLENO_RELACION = new Color(0x23, 0x1C, 0x30);
    public static final Color RELLENO_ATRIBUTO = new Color(0x1A, 0x21, 0x25);
    public static final Color RELLENO_ICONO = new Color(0x1E, 0x26, 0x2B);

    // --- trazos ---
    public static final Color BORDE = new Color(0x28, 0x32, 0x38);
    public static final Color BORDE_ENTIDAD = new Color(0x7F, 0x8F, 0x99);
    public static final Color BORDE_RELACION = new Color(0x6C, 0x5B, 0x8A);
    public static final Color BORDE_ATRIBUTO = new Color(0x54, 0x63, 0x6B);
    public static final Color LINEA = new Color(0x61, 0x73, 0x7D);
    public static final Color BORDE_ICONO = new Color(0x41, 0x50, 0x5A);

    // --- acentos ---
    public static final Color ORO = new Color(0xF0, 0xB4, 0x29);
    public static final Color TEAL = new Color(0x4C, 0xC2, 0xD6);

    // --- texto ---
    public static final Color TEXTO = new Color(0xDF, 0xE6, 0xE4);
    public static final Color TENUE = new Color(0x8B, 0x9A, 0xA2);
    public static final Color TRAZO_ICONO = new Color(0xC9, 0xD5, 0xDA);

    public static final Font ETIQUETA = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font MENUDA = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
}
