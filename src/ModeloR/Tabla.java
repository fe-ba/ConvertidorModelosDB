import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Representa una tabla del esquema relacional, con sus columnas y
 * restricciones asociadas.
 */
public class Tabla {

    private String id;
    private String nombre;
    private OrigenTabla origen;
    private String procedeDe;
    private List<Columna> columnas;
    private List<Restriccion> restricciones;

    public Tabla(String nombre, OrigenTabla origen, String procedeDe) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tabla no puede ser vacío.");
        }
        if (origen == null) {
            throw new IllegalArgumentException("El origen de la tabla no puede ser nulo.");
        }
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.origen = origen;
        this.procedeDe = procedeDe;
        this.columnas = new ArrayList<>();
        this.restricciones = new ArrayList<>();
    }

    //Agrega una columna a la tabla, validando que no exista ya una con el mismo nombre
    public Columna agregarColumna(Columna columna) {
        if (columna == null) {
            throw new IllegalArgumentException("La columna no puede ser nula.");
        }
        if (buscarColumna(columna.getNombre()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una columna llamada '" + columna.getNombre() + "' en la tabla " + nombre + ".");
        }
        columnas.add(columna);
        return columna;
    }

    public Columna buscarColumna(String nombreColumna) {
        return columnas.stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombreColumna))
                .findFirst()
                .orElse(null);
    }

   
     // Devuelve las columnas que forman parte de la clave primaria de la tabla
    
    public List<Columna> clave() {
        Restriccion pk = primaria();
        if (pk == null) {
            return Collections.emptyList();
        }
        return pk.getColumnas().stream()
                .map(this::buscarColumna)
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    //Determina la clave primaria pero primero mira que no este ya asignada
    public Restriccion definirClave(List<String> nombresColumnas) {
        if (primaria() != null) {
            throw new IllegalStateException("La tabla " + nombre + " ya tiene una clave primaria definida.");
        }
        Restriccion clave = new Restriccion(TipoRestriccion.PRIMARIA, nombresColumnas, null, null, null, null);
        return restringir(clave);
    }

    
    // Devuelve todas las restricciones de un tipo determinado.
     
    public List<Restriccion> deTipo(TipoRestriccion tipo) {
        return restricciones.stream()
                .filter(r -> r.getTipo() == tipo)
                .collect(Collectors.toList());
    }

    //Dice si la columna es la clave primaria
    public boolean esClave(String nombreColumna) {
        Restriccion pk = primaria();
        return pk != null && pk.abarca(nombreColumna);
    }

    //indica si la columna hace parte de una foranea
    public boolean esForanea(String nombreColumna) {
        return foraneas().stream().anyMatch(r -> r.abarca(nombreColumna));
    }

    /**
     * Devuelve todas las restricciones de clave foránea de la tabla.
     */
    public List<Restriccion> foraneas() {
        return deTipo(TipoRestriccion.FORANEA);
    }

    public List<Columna> getColumnas() {
        return Collections.unmodifiableList(columnas);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public OrigenTabla getOrigen() {
        return origen;
    }

    public String getProcedeDe() {
        return procedeDe;
    }

    public List<Restriccion> getRestricciones() {
        return Collections.unmodifiableList(restricciones);
    }

    //Esto es para tener el nombre de la tabla normalizado y posteriormente facilitar la secuencia SQL
    public String nombreNormalizado() {
        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    /**
     * Devuelve la restricción de clave primaria de la tabla, o {@code null}
     * si no tiene una definida.
     */
    public Restriccion primaria() {
        Optional<Restriccion> pk = deTipo(TipoRestriccion.PRIMARIA).stream().findFirst();
        return pk.orElse(null);
    }

    /**
     * Agrega una restricción a la tabla.
     */
    public Restriccion restringir(Restriccion restriccion) {
        if (restriccion == null) {
            throw new IllegalArgumentException("La restricción no puede ser nula.");0
        }
        restricciones.add(restriccion);
        return restriccion;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
