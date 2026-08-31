import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Genera el DDL de los cuatro motores SQL. Aqui vive toda la sintaxis SQL del programa.
public class GeneradorSQL implements IGeneradorDeCodigo {

    // Nombre de cada tipo en cada motor. Lo que cambia entre motores son datos, no clases.
    private static final Map<Destino, Map<TipoDato, String>> TIPOS = new EnumMap<>(Destino.class);
    // Par de delimitadores de identificadores: comillas, acentos graves o corchetes.
    private static final Map<Destino, String[]> DELIMITADORES = new EnumMap<>(Destino.class);
    // Palabra de cada accion referencial en el DDL.
    private static final Map<AccionReferencial, String> ACCIONES =
            new EnumMap<>(AccionReferencial.class);
    // Prefijo del nombre de cada CONSTRAINT.
    private static final Map<TipoRestriccion, String> PREFIJOS =
            new EnumMap<>(TipoRestriccion.class);

    static {
        TIPOS.put(Destino.POSTGRESQL, tipos("SERIAL", "INTEGER", "BIGINT", "VARCHAR(50)",
                "VARCHAR(120)", "TEXT", "NUMERIC(10,2)", "DOUBLE PRECISION", "BOOLEAN",
                "DATE", "TIMESTAMP"));
        TIPOS.put(Destino.MYSQL, tipos("INT AUTO_INCREMENT", "INT", "BIGINT", "VARCHAR(50)",
                "VARCHAR(120)", "TEXT", "DECIMAL(10,2)", "DOUBLE", "TINYINT(1)",
                "DATE", "DATETIME"));
        TIPOS.put(Destino.SQLSERVER, tipos("INT IDENTITY(1,1)", "INT", "BIGINT", "NVARCHAR(50)",
                "NVARCHAR(120)", "NVARCHAR(MAX)", "DECIMAL(10,2)", "FLOAT", "BIT",
                "DATE", "DATETIME2"));
        // SQLite no tiene autonumerico propio: la clave primaria entera ya lo es
        TIPOS.put(Destino.SQLITE, tipos("INTEGER", "INTEGER", "INTEGER", "TEXT",
                "TEXT", "TEXT", "REAL", "REAL", "INTEGER", "TEXT", "TEXT"));

        DELIMITADORES.put(Destino.POSTGRESQL, new String[] {"\"", "\""});
        DELIMITADORES.put(Destino.MYSQL, new String[] {"`", "`"});
        DELIMITADORES.put(Destino.SQLSERVER, new String[] {"[", "]"});
        DELIMITADORES.put(Destino.SQLITE, new String[] {"\"", "\""});

        ACCIONES.put(AccionReferencial.CASCADA, "CASCADE");
        ACCIONES.put(AccionReferencial.RESTRINGIR, "RESTRICT");
        ACCIONES.put(AccionReferencial.ANULAR, "SET NULL");
        ACCIONES.put(AccionReferencial.NINGUNA, "NO ACTION");

        PREFIJOS.put(TipoRestriccion.PRIMARIA, "pk");
        PREFIJOS.put(TipoRestriccion.FORANEA, "fk");
        PREFIJOS.put(TipoRestriccion.UNICA, "uq");
    }

    // Los tipos van en el mismo orden que las constantes de TipoDato.
    private static Map<TipoDato, String> tipos(String... nombres) {
        TipoDato[] valores = TipoDato.values();
        if (nombres.length != valores.length) {
            throw new IllegalStateException("Hay " + valores.length + " tipos de dato y "
                    + nombres.length + " traducciones: revisa GeneradorSQL");
        }
        Map<TipoDato, String> mapa = new EnumMap<>(TipoDato.class);
        for (int i = 0; i < valores.length; i++) {
            mapa.put(valores[i], nombres[i]);
        }
        return mapa;
    }

    private final Destino destino;

    public GeneradorSQL(Destino destino) {
        if (!destino.esSQL()) {
            throw new IllegalArgumentException(destino + " no es un motor SQL");
        }
        this.destino = destino;
    }

    @Override
    public Destino getDestino() {
        return destino;
    }

    @Override
    public String generar(EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Esquema generado desde el modelo entidad-relacion\n");
        sb.append("-- Destino: ").append(destino.getEtiqueta()).append("\n");
        if (destino == Destino.SQLITE) {
            sb.append("PRAGMA foreign_keys = ON;\n");
        }
        sb.append("\n");
        for (Tabla tabla : esquema.getTablas()) {
            sb.append(sentenciaCrear(tabla, esquema)).append("\n");
        }
        List<Aviso> avisos = esquema.validar();
        if (!avisos.isEmpty()) {
            sb.append("-- Revisar:\n");
            for (Aviso aviso : avisos) {
                sb.append("--   ").append(aviso.getMensaje()).append("\n");
            }
        }
        return sb.toString();
    }

    // Una sentencia CREATE TABLE completa, con sus columnas y sus restricciones.
    private String sentenciaCrear(Tabla tabla, EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- ").append(descripcion(tabla.getOrigen())).append("\n");
        if (tabla.getColumnas().isEmpty()) {
            // un CREATE TABLE sin columnas no es SQL valido en ningun motor
            return sb.append("-- La tabla ").append(tabla.nombreNormalizado())
                     .append(" no tiene columnas: se omite.\n").toString();
        }
        sb.append("CREATE TABLE ").append(delimitar(tabla.nombreNormalizado())).append(" (\n");

        List<String> lineas = new ArrayList<>();
        for (Columna columna : tabla.getColumnas()) {
            lineas.add("  " + definicion(columna, tabla));
        }
        for (Restriccion restriccion : tabla.getRestricciones()) {
            String clausula = clausula(restriccion, tabla, esquema);
            if (clausula != null) {
                lineas.add("  " + clausula);
            }
        }
        sb.append(String.join(",\n", lineas));
        sb.append("\n);\n");
        return sb.toString();
    }

    private String definicion(Columna columna, Tabla tabla) {
        StringBuilder sb = new StringBuilder();
        sb.append(delimitar(columna.getNombre())).append(" ").append(tipoDe(columna));
        // en PostgreSQL el SERIAL ya implica NOT NULL
        boolean serialPostgres = (destino == Destino.POSTGRESQL && columna.esAutonumerica());
        if (!columna.admiteNulos() && !serialPostgres) {
            sb.append(" NOT NULL");
        }
        // SQLite exige declarar la clave en la propia columna para que autoincremente
        if (destino == Destino.SQLITE && columna.esAutonumerica()
                && esClaveSimple(tabla, columna)) {
            sb.append(" PRIMARY KEY AUTOINCREMENT");
        }
        return sb.toString();
    }

    // Despacha por TipoRestriccion: una sola cara para las tres clausulas.
    private String clausula(Restriccion restriccion, Tabla tabla,
                            EsquemaRelacional esquema) {
        String nombre = delimitar(nombreRestriccion(restriccion, tabla, esquema));
        String columnas = listaDelimitada(restriccion.getColumnas());
        switch (restriccion.getTipo()) {
            case PRIMARIA:
                // en SQLite ya se declaro en la columna, no se repite
                if (destino == Destino.SQLITE && claveDeclaradaEnLaColumna(tabla)) {
                    return null;
                }
                return "CONSTRAINT " + nombre + " PRIMARY KEY (" + columnas + ")";
            case UNICA:
                return "CONSTRAINT " + nombre + " UNIQUE (" + columnas + ")";
            case FORANEA:
                return "CONSTRAINT " + nombre + " FOREIGN KEY (" + columnas + ")"
                        + " REFERENCES " + delimitar(nombreDe(restriccion.getTablaReferida(), esquema))
                        + " (" + listaDelimitada(restriccion.getColumnasReferidas()) + ")"
                        + " ON UPDATE " + ACCIONES.get(restriccion.getAlActualizar())
                        + " ON DELETE " + ACCIONES.get(restriccion.getAlBorrar());
            default:
                return null;
        }
    }

    // Nombre determinista del CONSTRAINT: pk_tabla, fk_tabla_referida, uq_tabla_columnas.
    private String nombreRestriccion(Restriccion restriccion, Tabla tabla,
                                     EsquemaRelacional esquema) {
        StringBuilder sb = new StringBuilder(PREFIJOS.get(restriccion.getTipo()));
        sb.append("_").append(tabla.nombreNormalizado());
        if (restriccion.getTipo() == TipoRestriccion.FORANEA
                && restriccion.getTablaReferida() != null) {
            sb.append("_").append(nombreDe(restriccion.getTablaReferida(), esquema));
        } else if (restriccion.getTipo() == TipoRestriccion.UNICA) {
            for (String columna : restriccion.getColumnas()) {
                sb.append("_").append(columna);
            }
        }
        return sb.toString();
    }

    private String tipoDe(Columna columna) {
        String tipo = TIPOS.get(destino).get(columna.getTipo());
        if (tipo == null) {
            // salta si alguien anade un TipoDato y no lo traduce aqui
            throw new IllegalStateException("Falta la traduccion de "
                    + columna.getTipo() + " para " + destino);
        }
        return tipo;
    }

    private String delimitar(String identificador) {
        String[] par = DELIMITADORES.get(destino);
        return par[0] + identificador + par[1];
    }

    private String listaDelimitada(List<String> columnas) {
        List<String> partes = new ArrayList<>();
        for (String columna : columnas) {
            partes.add(delimitar(columna));
        }
        return String.join(", ", partes);
    }

    // La foranea guarda el nombre visible de la tabla; se lo pedimos normalizado a ella
    // misma para no duplicar aqui las reglas de normalizacion.
    private String nombreDe(String nombreVisible, EsquemaRelacional esquema) {
        Tabla referida = esquema.buscarTabla(nombreVisible);
        if (referida != null) {
            return referida.nombreNormalizado();
        }
        // la foranea apunta fuera del esquema: validar() ya lo denuncia, pero el
        // nombre se normaliza igual para no mezclar dos estilos en el mismo DDL
        if (nombreVisible == null || nombreVisible.isBlank()) {
            return "sin_nombre";
        }
        return new Tabla(nombreVisible, OrigenTabla.ENTIDAD_FUERTE, null).nombreNormalizado();
    }

    private boolean esClaveSimple(Tabla tabla, Columna columna) {
        Restriccion primaria = tabla.primaria();
        return primaria != null && primaria.getColumnas().size() == 1
                && primaria.getColumnas().get(0).equals(columna.getNombre());
    }

    private boolean claveDeclaradaEnLaColumna(Tabla tabla) {
        Restriccion primaria = tabla.primaria();
        if (primaria == null || primaria.getColumnas().size() != 1) {
            return false;
        }
        Columna columna = tabla.buscarColumna(primaria.getColumnas().get(0));
        return columna != null && columna.esAutonumerica();
    }

    private String descripcion(OrigenTabla origen) {
        switch (origen) {
            case ENTIDAD_FUERTE: return "Entidad fuerte";
            case ENTIDAD_DEBIL: return "Entidad debil: su clave incluye la de la propietaria";
            case RELACION_NM: return "Relacion N:M: la clave es la union de las foraneas";
            case RELACION_NARIA: return "Relacion n-aria: una foranea por participante";
            case ATRIBUTO_MULTIVALUADO: return "Atributo multivaluado extraido a tabla propia";
            default: return "Tabla";
        }
    }
}
