import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Lector y escritor de JSON minimo, para no depender de ninguna biblioteca.
// Un objeto se lee como Map, un arreglo como List, los numeros como Double.
public final class Json {

    private final String texto;
    private int posicion;

    private Json(String texto) {
        this.texto = texto;
        this.posicion = 0;
    }

    // --- Lectura ---

    public static Object leer(String texto) {
        Json lector = new Json(texto);
        lector.saltarEspacios();
        Object valor = lector.valor();
        lector.saltarEspacios();
        if (lector.posicion < texto.length()) {
            throw new IllegalArgumentException(
                    "Sobra texto despues del JSON, en la posicion " + lector.posicion);
        }
        return valor;
    }

    private Object valor() {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new IllegalArgumentException("El JSON se corta antes de tiempo.");
        }
        char c = texto.charAt(posicion);
        switch (c) {
            case '{': return objeto();
            case '[': return arreglo();
            case '"': return cadena();
            case 't': return literal("true", Boolean.TRUE);
            case 'f': return literal("false", Boolean.FALSE);
            case 'n': return literal("null", null);
            default: return numero();
        }
    }

    private Map<String, Object> objeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        esperar('{');
        saltarEspacios();
        if (mirar() == '}') {
            posicion++;
            return mapa;
        }
        while (true) {
            saltarEspacios();
            String clave = cadena();
            saltarEspacios();
            esperar(':');
            mapa.put(clave, valor());
            saltarEspacios();
            char c = texto.charAt(posicion++);
            if (c == '}') {
                return mapa;
            }
            if (c != ',') {
                throw new IllegalArgumentException(
                        "Se esperaba , o } en la posicion " + (posicion - 1));
            }
        }
    }

    private List<Object> arreglo() {
        List<Object> lista = new ArrayList<>();
        esperar('[');
        saltarEspacios();
        if (mirar() == ']') {
            posicion++;
            return lista;
        }
        while (true) {
            lista.add(valor());
            saltarEspacios();
            char c = texto.charAt(posicion++);
            if (c == ']') {
                return lista;
            }
            if (c != ',') {
                throw new IllegalArgumentException(
                        "Se esperaba , o ] en la posicion " + (posicion - 1));
            }
        }
    }

    private String cadena() {
        esperar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = texto.charAt(posicion++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char escape = texto.charAt(posicion++);
            switch (escape) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(
                            texto.substring(posicion, posicion + 4), 16));
                    posicion += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Escape desconocido: \\" + escape);
            }
        }
    }

    private Double numero() {
        int inicio = posicion;
        while (posicion < texto.length()
                && "+-0123456789.eE".indexOf(texto.charAt(posicion)) >= 0) {
            posicion++;
        }
        if (inicio == posicion) {
            throw new IllegalArgumentException(
                    "No se esperaba '" + texto.charAt(posicion) + "' en la posicion "
                    + posicion);
        }
        return Double.valueOf(texto.substring(inicio, posicion));
    }

    private Object literal(String palabra, Object valor) {
        if (!texto.startsWith(palabra, posicion)) {
            throw new IllegalArgumentException(
                    "Se esperaba " + palabra + " en la posicion " + posicion);
        }
        posicion += palabra.length();
        return valor;
    }

    private char mirar() {
        return texto.charAt(posicion);
    }

    private void esperar(char c) {
        saltarEspacios();
        if (posicion >= texto.length() || texto.charAt(posicion) != c) {
            throw new IllegalArgumentException(
                    "Se esperaba '" + c + "' en la posicion " + posicion);
        }
        posicion++;
    }

    private void saltarEspacios() {
        while (posicion < texto.length()
                && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    // --- Escritura ---

    // Entrecomilla y escapa una cadena para meterla en el archivo.
    public static String comillas(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    // --- Ayudas de lectura, tolerantes con lo que falte ---

    @SuppressWarnings("unchecked")
    public static Map<String, Object> comoObjeto(Object valor) {
        return (valor instanceof Map) ? (Map<String, Object>) valor
                : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> comoLista(Object valor) {
        return (valor instanceof List) ? (List<Object>) valor : new ArrayList<>();
    }

    public static String comoTexto(Object valor, String pordefecto) {
        return (valor instanceof String) ? (String) valor : pordefecto;
    }

    public static double comoNumero(Object valor, double pordefecto) {
        return (valor instanceof Number) ? ((Number) valor).doubleValue() : pordefecto;
    }

    public static boolean comoBooleano(Object valor, boolean pordefecto) {
        return (valor instanceof Boolean) ? (Boolean) valor : pordefecto;
    }
}
