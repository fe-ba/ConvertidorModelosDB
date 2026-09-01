import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Genera modelos de SQLAlchemy e interfaces de TypeScript.
// Van aparte de GeneradorSQL porque no cambian unos valores: cambia la forma del texto.
public class GeneradorObjetos implements IGeneradorDeCodigo {

    private static final Map<TipoDato, String> PYTHON = new EnumMap<>(TipoDato.class);
    private static final Map<TipoDato, String> TYPESCRIPT = new EnumMap<>(TipoDato.class);

    static {
        PYTHON.put(TipoDato.SERIAL, "Integer");
        PYTHON.put(TipoDato.ENTERO, "Integer");
        PYTHON.put(TipoDato.ENTERO_GRANDE, "BigInteger");
        PYTHON.put(TipoDato.TEXTO_CORTO, "String(50)");
        PYTHON.put(TipoDato.TEXTO_MEDIO, "String(120)");
        PYTHON.put(TipoDato.TEXTO_LARGO, "Text");
        PYTHON.put(TipoDato.DECIMAL, "Numeric(10, 2)");
        PYTHON.put(TipoDato.REAL, "Float");
        PYTHON.put(TipoDato.BOOLEANO, "Boolean");
        PYTHON.put(TipoDato.FECHA, "Date");
        PYTHON.put(TipoDato.FECHA_HORA, "DateTime");

        for (TipoDato tipo : TipoDato.values()) {
            TYPESCRIPT.put(tipo, "string");
        }
        TYPESCRIPT.put(TipoDato.SERIAL, "number");
        TYPESCRIPT.put(TipoDato.ENTERO, "number");
        TYPESCRIPT.put(TipoDato.ENTERO_GRANDE, "number");
        TYPESCRIPT.put(TipoDato.DECIMAL, "number");
        TYPESCRIPT.put(TipoDato.REAL, "number");
        TYPESCRIPT.put(TipoDato.BOOLEANO, "boolean");
    }

    private final Destino destino;

    public GeneradorObjetos(Destino destino) {
        if (destino.esSQL()) {
            throw new IllegalArgumentException(destino + " es un motor SQL");
        }
        this.destino = destino;
    }

    @Override
    public Destino getDestino() {
        return destino;
    }

    @Override
    public String generar(EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder(encabezado());
        for (Tabla tabla : esquema.getTablas()) {
            if (tabla.getColumnas().isEmpty()) {
                continue;   // sin columnas no hay clase ni interfaz que generar
            }
            sb.append(destino == Destino.SQLALCHEMY
                    ? claseSQLAlchemy(tabla, esquema)
                    : interfazTypeScript(tabla, esquema));
        }
        return sb.toString();
    }

    private String encabezado() {
        if (destino == Destino.SQLALCHEMY) {
            return "from sqlalchemy import (Column, Integer, BigInteger, String, Text,\n"
                    + "                        Numeric, Float, Boolean, Date, DateTime,\n"
                    + "                        ForeignKey, UniqueConstraint)\n"
                    + "from sqlalchemy.orm import declarative_base\n\n"
                    + "Base = declarative_base()\n\n\n";
        }
        return "// Tipos generados desde el modelo relacional\n\n";
    }

    private String claseSQLAlchemy(Tabla tabla, EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(nombreDeClase(tabla)).append("(Base):\n");
        sb.append("    __tablename__ = \"").append(tabla.nombreNormalizado()).append("\"\n");
        for (Columna columna : tabla.getColumnas()) {
            sb.append("    ").append(columna.getNombre()).append(" = Column(")
              .append(String.join(", ", argumentosColumna(columna, tabla, esquema)))
              .append(")\n");
        }
        for (Restriccion unica : tabla.deTipo(TipoRestriccion.UNICA)) {
            if (unica.getColumnas().size() > 1) {
                // las unicas de una sola columna ya van con unique=True en la columna
                List<String> entrecomilladas = new ArrayList<>();
                for (String columna : unica.getColumnas()) {
                    entrecomilladas.add("\"" + columna + "\"");
                }
                sb.append("    __table_args__ = (UniqueConstraint(")
                  .append(String.join(", ", entrecomilladas)).append("),)\n");
            }
        }
        sb.append("\n\n");
        return sb.toString();
    }

    private List<String> argumentosColumna(Columna columna, Tabla tabla,
                                           EsquemaRelacional esquema) {
        List<String> args = new ArrayList<>();
        args.add(PYTHON.get(columna.getTipo()));
        Restriccion foranea = foraneaDe(tabla, columna.getNombre());
        if (foranea != null) {
            int i = foranea.getColumnas().indexOf(columna.getNombre());
            String referida = nombreDe(foranea.getTablaReferida(), esquema) + "."
                    + foranea.getColumnasReferidas().get(i);
            args.add("ForeignKey(\"" + referida + "\")");
        }
        if (tabla.esClave(columna.getNombre())) {
            args.add("primary_key=True");
        } else if (!columna.admiteNulos()) {
            args.add("nullable=False");
        }
        if (esUnica(tabla, columna.getNombre())) {
            args.add("unique=True");
        }
        return args;
    }

    private String interfazTypeScript(Tabla tabla, EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder();
        sb.append("export interface ").append(nombreDeClase(tabla)).append(" {\n");
        for (Columna columna : tabla.getColumnas()) {
            boolean obligatoria = !columna.admiteNulos() || tabla.esClave(columna.getNombre());
            sb.append("  ").append(columna.getNombre()).append(obligatoria ? "" : "?")
              .append(": ").append(TYPESCRIPT.get(columna.getTipo())).append(";");
            if (tabla.esClave(columna.getNombre())) {
                sb.append("  // clave primaria");
            } else {
                Restriccion foranea = foraneaDe(tabla, columna.getNombre());
                if (foranea != null) {
                    int i = foranea.getColumnas().indexOf(columna.getNombre());
                    sb.append("  // -> ").append(nombreDe(foranea.getTablaReferida(), esquema))
                      .append(".").append(foranea.getColumnasReferidas().get(i));
                }
            }
            sb.append("\n");
        }
        sb.append("}\n\n");
        return sb.toString();
    }

    private Restriccion foraneaDe(Tabla tabla, String columna) {
        for (Restriccion restriccion : tabla.foraneas()) {
            if (restriccion.abarca(columna)) {
                return restriccion;
            }
        }
        return null;
    }

    private boolean esUnica(Tabla tabla, String columna) {
        for (Restriccion restriccion : tabla.deTipo(TipoRestriccion.UNICA)) {
            if (restriccion.abarca(columna) && restriccion.getColumnas().size() == 1) {
                return true;
            }
        }
        return false;
    }

    // usuario_pedido -> UsuarioPedido
    private String nombreDeClase(Tabla tabla) {
        StringBuilder sb = new StringBuilder();
        for (String parte : tabla.nombreNormalizado().split("_")) {
            if (!parte.isEmpty()) {
                sb.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1));
            }
        }
        return sb.length() == 0 ? "Tabla" : sb.toString();
    }

    // Se lo pedimos normalizado a la propia tabla, para no duplicar las reglas.
    private String nombreDe(String nombreVisible, EsquemaRelacional esquema) {
        Tabla referida = esquema.buscarTabla(nombreVisible);
        if (referida != null) {
            return referida.nombreNormalizado();
        }
        if (nombreVisible == null || nombreVisible.isBlank()) {
            return "sin_nombre";
        }
        return new Tabla(nombreVisible, OrigenTabla.ENTIDAD_FUERTE, null).nombreNormalizado();
    }
}
