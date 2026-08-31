import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EsquemaRelacional {

    private List<Tabla> tablas;

    public EsquemaRelacional() {
        this.tablas = new ArrayList<>();
    }

    /**
     * Agrega una tabla al esquema, validando que no exista ya una con el
     * mismo nombre.
     */
    public Tabla agregarTabla(Tabla tabla) {
        if (tabla == null) {
            throw new IllegalArgumentException("La tabla no puede ser nula.");
        }
        if (buscarTabla(tabla.getNombre()) != null) {
            throw new IllegalArgumentException("Ya existe una tabla llamada '" + tabla.getNombre() + "'.");
        }
        tablas.add(tabla);
        return tabla;
    }

    /**
     * Busca una tabla por nombre (sin distinguir mayúsculas/minúsculas).
     * Devuelve {@code null} si no existe.
     */
    public Tabla buscarTabla(String nombre) {
        return tablas.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca la tabla que procede del elemento del modelo ER indicado
     * (comparando contra el atributo {@code procedeDe} de cada tabla).
     */
    public Tabla deOrigen(String procedeDe) {
        return tablas.stream()
                .filter(t -> t.getProcedeDe() != null && t.getProcedeDe().equalsIgnoreCase(procedeDe))
                .findFirst()
                .orElse(null);
    }

    public List<Tabla> getTablas() {
        return Collections.unmodifiableList(tablas);
    }

    /**
     * Genera una notación textual simple del esquema, con una línea por
     * tabla en el formato: NombreTabla(columna1, columna2, ...).
     */
    public List<String> notacionTextual() {
        return tablas.stream()
                .map(t -> t.getNombre() + "(" +
                        t.getColumnas().stream()
                                .map(Columna::getNombre)
                                .collect(Collectors.joining(", "))
                        + ")")
                .collect(Collectors.toList());
    }

    
    //Valida el esquema y devuelve la lista de avisos encontrados: tablas sin clave primaria y restricciones foráneas que apuntan a tablas inexistentes.
    public List<Aviso> validar() {
        List<Aviso> avisos = new ArrayList<>();

        for (Tabla tabla : tablas) {
            if (tabla.primaria() == null) {
                avisos.add(new Aviso(Severidad.ADVERTENCIA,
                        "La tabla no tiene clave primaria definida.",
                        tabla.getNombre()));
            }
            for (Restriccion restriccion : tabla.foraneas()) {
                if (buscarTabla(restriccion.getTablaReferida()) == null) {
                    avisos.add(new Aviso(Severidad.ERROR,
                            "La restricción foránea hace referencia a la tabla inexistente '"
                                    + restriccion.getTablaReferida() + "'.",
                            tabla.getNombre()));
                }
            }
        }

        return avisos;
    }
}
