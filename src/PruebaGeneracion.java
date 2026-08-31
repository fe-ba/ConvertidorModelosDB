import java.util.Arrays;
import java.util.List;

// Pruebas de la rama de generacion de codigo.
public class PruebaGeneracion {

    private static int fallos = 0;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("PRUEBAS DE GENERACION DE CODIGO");
        System.out.println("==========================================");
        EsquemaRelacional esquema = esquemaDePrueba();

        probarDestinos();
        probarSQL(esquema);
        probarObjetos(esquema);
        probarCatalogo(esquema);
        probarEsquemaVacio();
        probarUnicaCompuesta();
        probarCasosLimite();

        System.out.println(fallos == 0 ? "\nTODO CORRECTO" : "\n" + fallos + " FALLOS");
        System.exit(fallos);
    }

    // Esquema con clave compuesta, foraneas, unica y tabla intermedia N:M.
    public static EsquemaRelacional esquemaDePrueba() {
        EsquemaRelacional e = new EsquemaRelacional();

        Tabla profesor = new Tabla("Profesor", OrigenTabla.ENTIDAD_FUERTE, "e1");
        profesor.agregarColumna(new Columna("id_profesor", TipoDato.SERIAL, false));
        profesor.agregarColumna(new Columna("nombre", TipoDato.TEXTO_MEDIO, false));
        profesor.agregarColumna(new Columna("email", TipoDato.TEXTO_MEDIO, true));
        profesor.definirClave(Arrays.asList("id_profesor"));
        profesor.restringir(new Restriccion(TipoRestriccion.UNICA, Arrays.asList("email"),
                null, null, AccionReferencial.NINGUNA, AccionReferencial.NINGUNA));
        e.agregarTabla(profesor);

        Tabla familiar = new Tabla("Familiar", OrigenTabla.ENTIDAD_DEBIL, "e2");
        familiar.agregarColumna(new Columna("nombre", TipoDato.TEXTO_MEDIO, false));
        familiar.agregarColumna(new Columna("profesor_id_profesor", TipoDato.ENTERO, false));
        familiar.definirClave(Arrays.asList("nombre", "profesor_id_profesor"));
        familiar.restringir(new Restriccion(TipoRestriccion.FORANEA,
                Arrays.asList("profesor_id_profesor"), "Profesor",
                Arrays.asList("id_profesor"),
                AccionReferencial.CASCADA, AccionReferencial.CASCADA));
        e.agregarTabla(familiar);

        Tabla curso = new Tabla("Curso", OrigenTabla.ENTIDAD_FUERTE, "e3");
        curso.agregarColumna(new Columna("id_curso", TipoDato.SERIAL, false));
        curso.agregarColumna(new Columna("titulo", TipoDato.TEXTO_MEDIO, false));
        curso.definirClave(Arrays.asList("id_curso"));
        e.agregarTabla(curso);

        Tabla inscribe = new Tabla("Inscribe", OrigenTabla.RELACION_NM, "r1");
        inscribe.agregarColumna(new Columna("profesor_id_profesor", TipoDato.ENTERO, false));
        inscribe.agregarColumna(new Columna("curso_id_curso", TipoDato.ENTERO, false));
        inscribe.agregarColumna(new Columna("nota", TipoDato.DECIMAL, true));
        inscribe.definirClave(Arrays.asList("profesor_id_profesor", "curso_id_curso"));
        inscribe.restringir(new Restriccion(TipoRestriccion.FORANEA,
                Arrays.asList("profesor_id_profesor"), "Profesor",
                Arrays.asList("id_profesor"), AccionReferencial.RESTRINGIR,
                AccionReferencial.CASCADA));
        inscribe.restringir(new Restriccion(TipoRestriccion.FORANEA,
                Arrays.asList("curso_id_curso"), "Curso", Arrays.asList("id_curso"),
                AccionReferencial.RESTRINGIR, AccionReferencial.CASCADA));
        e.agregarTabla(inscribe);
        return e;
    }

    private static void probarDestinos() {
        System.out.println("\n--- Destinos ---");
        debe("seis destinos", Destino.values().length == 6);
        debe("cuatro son SQL", contarSQL() == 4);
        debe("PostgreSQL exporta .sql",
                Destino.POSTGRESQL.getExtension().equals("sql"));
        debe("SQLAlchemy exporta .py",
                Destino.SQLALCHEMY.getExtension().equals("py"));
        debe("GeneradorSQL rechaza un destino que no es SQL",
                rechaza(() -> new GeneradorSQL(Destino.TYPESCRIPT)));
        debe("GeneradorObjetos rechaza un motor SQL",
                rechaza(() -> new GeneradorObjetos(Destino.MYSQL)));
    }

    private static void probarSQL(EsquemaRelacional esquema) {
        System.out.println("\n--- SQL ---");
        String postgres = new GeneradorSQL(Destino.POSTGRESQL).generar(esquema);
        debe("cuatro CREATE TABLE", contar(postgres, "CREATE TABLE") == 4);
        debe("clave compuesta en una sola clausula",
                postgres.contains("PRIMARY KEY (\"nombre\", \"profesor_id_profesor\")"));
        debe("SERIAL sin NOT NULL en PostgreSQL",
                postgres.contains("\"id_profesor\" SERIAL,"));
        debe("las acciones referenciales salen",
                postgres.contains("ON UPDATE CASCADE ON DELETE CASCADE"));
        debe("nombres de constraint deterministas",
                postgres.contains("\"fk_familiar_profesor\"")
                        && postgres.contains("\"uq_profesor_email\""));

        String mysql = new GeneradorSQL(Destino.MYSQL).generar(esquema);
        debe("MySQL usa acentos graves", mysql.contains("`profesor`"));
        debe("MySQL usa AUTO_INCREMENT", mysql.contains("INT AUTO_INCREMENT"));

        String sqlserver = new GeneradorSQL(Destino.SQLSERVER).generar(esquema);
        debe("SQL Server usa corchetes", sqlserver.contains("[profesor]"));
        debe("SQL Server usa IDENTITY", sqlserver.contains("IDENTITY(1,1)"));

        String sqlite = new GeneradorSQL(Destino.SQLITE).generar(esquema);
        debe("SQLite declara la clave en la columna",
                sqlite.contains("PRIMARY KEY AUTOINCREMENT"));
        debe("SQLite no repite la clave simple como constraint",
                !sqlite.contains("CONSTRAINT \"pk_profesor\""));
        debe("SQLite si declara la clave compuesta",
                sqlite.contains("CONSTRAINT \"pk_familiar\""));
        debe("SQLite activa las foraneas", sqlite.contains("PRAGMA foreign_keys = ON"));
    }

    private static void probarObjetos(EsquemaRelacional esquema) {
        System.out.println("\n--- SQLAlchemy y TypeScript ---");
        String python = new GeneradorObjetos(Destino.SQLALCHEMY).generar(esquema);
        debe("una clase por tabla", contar(python, "(Base):") == 4);
        debe("el nombre de clase se normaliza", python.contains("class Profesor(Base):"));
        debe("la foranea apunta a tabla.columna",
                python.contains("ForeignKey(\"profesor.id_profesor\")"));
        debe("la clave compuesta marca las dos columnas",
                contar(python.substring(python.indexOf("class Familiar")),
                        "primary_key=True") >= 2);
        debe("el unico se traduce", python.contains("unique=True"));

        String ts = new GeneradorObjetos(Destino.TYPESCRIPT).generar(esquema);
        debe("una interfaz por tabla", contar(ts, "export interface") == 4);
        debe("los tipos numericos se traducen", ts.contains("id_profesor: number;"));
        debe("las columnas nulables son opcionales", ts.contains("email?: string;"));
        debe("la clave no es opcional", !ts.contains("id_profesor?"));
    }

    private static void probarCatalogo(EsquemaRelacional esquema) {
        System.out.println("\n--- Catalogo ---");
        for (Destino destino : Destino.values()) {
            IGeneradorDeCodigo generador = CatalogoGeneradores.para(destino);
            String salida = generador.generar(esquema);
            debe("genera para " + destino.getEtiqueta(),
                    salida != null && salida.length() > 100
                            && generador.getDestino() == destino);
        }
    }

    private static void probarEsquemaVacio() {
        System.out.println("\n--- Esquema vacio ---");
        EsquemaRelacional vacio = new EsquemaRelacional();
        for (Destino destino : Destino.values()) {
            try {
                CatalogoGeneradores.para(destino).generar(vacio);
            } catch (RuntimeException e) {
                debe("no revienta con " + destino, false);
                return;
            }
        }
        debe("ningun generador revienta con un esquema vacio", true);
    }

    // Una unica de varias columnas no cabe en la columna: necesita clausula propia.
    private static void probarUnicaCompuesta() {
        System.out.println("\n--- Restriccion unica compuesta ---");
        EsquemaRelacional e = new EsquemaRelacional();
        Tabla horario = new Tabla("Horario", OrigenTabla.ENTIDAD_FUERTE, "x1");
        horario.agregarColumna(new Columna("id", TipoDato.SERIAL, false));
        horario.agregarColumna(new Columna("aula", TipoDato.TEXTO_CORTO, false));
        horario.agregarColumna(new Columna("hora", TipoDato.FECHA_HORA, false));
        horario.definirClave(Arrays.asList("id"));
        horario.restringir(new Restriccion(TipoRestriccion.UNICA,
                Arrays.asList("aula", "hora"), null, null, null, null));
        e.agregarTabla(horario);

        String sql = new GeneradorSQL(Destino.POSTGRESQL).generar(e);
        debe("el SQL agrupa las dos columnas en una UNIQUE",
                sql.contains("UNIQUE (\"aula\", \"hora\")"));
        String python = new GeneradorObjetos(Destino.SQLALCHEMY).generar(e);
        debe("SQLAlchemy la saca como __table_args__",
                python.contains("__table_args__ = (UniqueConstraint(\"aula\", \"hora\"),)"));
    }

    // Casos que no deberian llegar del conversor, pero que no pueden romper el generador.
    private static void probarCasosLimite() {
        System.out.println("\n--- Casos limite ---");

        EsquemaRelacional sinColumnas = new EsquemaRelacional();
        sinColumnas.agregarTabla(new Tabla("Vacia", OrigenTabla.ENTIDAD_FUERTE, null));
        String sql = new GeneradorSQL(Destino.POSTGRESQL).generar(sinColumnas);
        debe("una tabla sin columnas no produce un CREATE TABLE invalido",
                !sql.contains("CREATE TABLE"));
        debe("y tampoco una clase de SQLAlchemy",
                !new GeneradorObjetos(Destino.SQLALCHEMY).generar(sinColumnas)
                        .contains("(Base):"));

        EsquemaRelacional rota = new EsquemaRelacional();
        Tabla hija = new Tabla("Hija", OrigenTabla.ENTIDAD_FUERTE, null);
        hija.agregarColumna(new Columna("padre_id", TipoDato.ENTERO, false));
        hija.definirClave(Arrays.asList("padre_id"));
        hija.restringir(new Restriccion(TipoRestriccion.FORANEA, Arrays.asList("padre_id"),
                "Tabla Ausente", Arrays.asList("id"),
                AccionReferencial.CASCADA, AccionReferencial.CASCADA));
        rota.agregarTabla(hija);
        String sqlRoto = new GeneradorSQL(Destino.POSTGRESQL).generar(rota);
        debe("una foranea rota se normaliza igual",
                sqlRoto.contains("REFERENCES \"tabla_ausente\""));
        debe("y queda denunciada en los comentarios",
                sqlRoto.contains("-- Revisar:"));

        EsquemaRelacional acentos = new EsquemaRelacional();
        Tabla anio = new Tabla("Anio Academico", OrigenTabla.ENTIDAD_FUERTE, null);
        anio.agregarColumna(new Columna("id", TipoDato.SERIAL, false));
        anio.definirClave(Arrays.asList("id"));
        acentos.agregarTabla(anio);
        debe("los espacios del nombre pasan a guion bajo",
                new GeneradorSQL(Destino.MYSQL).generar(acentos)
                        .contains("`anio_academico`"));
        debe("y la clase queda en mayusculas de camello",
                new GeneradorObjetos(Destino.TYPESCRIPT).generar(acentos)
                        .contains("interface AnioAcademico"));

        EsquemaRelacional todos = new EsquemaRelacional();
        Tabla completa = new Tabla("Todos", OrigenTabla.ENTIDAD_FUERTE, null);
        for (TipoDato tipo : TipoDato.values()) {
            completa.agregarColumna(new Columna("c_" + tipo.name().toLowerCase(), tipo, true));
        }
        completa.definirClave(Arrays.asList("c_serial"));
        todos.agregarTabla(completa);
        boolean limpio = true;
        for (Destino destino : Destino.values()) {
            // ojo: "nullable=False" contiene la palabra, hay que buscar el null suelto
            if (CatalogoGeneradores.para(destino).generar(todos)
                    .matches("(?s).*\\bnull\\b(?!able).*")) {
                limpio = false;
            }
        }
        debe("los once tipos se traducen en los seis destinos, sin ningun null", limpio);
    }

    // --- utilidades ---
    private static int contarSQL() {
        int n = 0;
        for (Destino d : Destino.values()) {
            if (d.esSQL()) {
                n++;
            }
        }
        return n;
    }

    private static int contar(String texto, String aguja) {
        int n = 0;
        int i = texto.indexOf(aguja);
        while (i >= 0) {
            n++;
            i = texto.indexOf(aguja, i + aguja.length());
        }
        return n;
    }

    private static boolean rechaza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void debe(String que, boolean condicion) {
        System.out.println((condicion ? "  [OK]   " : "  [FALLA]") + " " + que);
        if (!condicion) {
            fallos++;
        }
    }
}
