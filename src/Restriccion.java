import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Restriccion {

    private TipoRestriccion tipo;
    private List<String> columnas;
    private String tablaReferida;
    private List<String> columnasReferidas;
    private AccionReferencial alActualizar;
    private AccionReferencial alBorrar;

    public Restriccion(TipoRestriccion tipo, List<String> columnas, String tablaReferida,
                        List<String> columnasReferidas, AccionReferencial alActualizar,
                        AccionReferencial alBorrar) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de restricción no puede ser nulo.");
        }
        if (columnas == null || columnas.isEmpty()) {
            throw new IllegalArgumentException("La restricción debe tener al menos una columna.");
        }
        if (tipo == TipoRestriccion.FORANEA) { //Casos para la restricción foranea
            if (tablaReferida == null || tablaReferida.isBlank()) {
                throw new IllegalArgumentException("Una restricción foránea debe indicar la tabla referida.");
            }
            if (columnasReferidas == null || columnasReferidas.isEmpty()) {
                throw new IllegalArgumentException("Una restricción foránea debe indicar las columnas referidas.");
            }
            if (columnasReferidas.size() != columnas.size()) {
                throw new IllegalArgumentException("La cantidad de columnas y columnas referidas debe coincidir.");
            }
        }

        this.tipo = tipo;
        this.columnas = new ArrayList<>(columnas);
        this.tablaReferida = tablaReferida;
        this.columnasReferidas = columnasReferidas == null ? new ArrayList<>() : new ArrayList<>(columnasReferidas);
        this.alActualizar = tipo == TipoRestriccion.FORANEA
                ? (alActualizar == null ? AccionReferencial.NINGUNA : alActualizar)
                : null;
        this.alBorrar = tipo == TipoRestriccion.FORANEA
                ? (alBorrar == null ? AccionReferencial.NINGUNA : alBorrar)
                : null;
    }

    /**
     * Indica si la restricción abarca (incluye) a la columna dada.
     */
    public boolean abarca(String nombreColumna) {
        return columnas.stream().anyMatch(c -> c.equalsIgnoreCase(nombreColumna));
    }

    public AccionReferencial getAlActualizar() {
        return alActualizar;
    }

    public AccionReferencial getAlBorrar() {
        return alBorrar;
    }

    public List<String> getColumnas() {
        return Collections.unmodifiableList(columnas);
    }

    public List<String> getColumnasReferidas() {
        return Collections.unmodifiableList(columnasReferidas);
    }

    public String getTablaReferida() {
        return tablaReferida;
    }

    public TipoRestriccion getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        return switch (tipo) {
            case PRIMARIA -> "PRIMARY KEY " + columnas;
            case UNICA -> "UNIQUE " + columnas;
            case FORANEA -> "FOREIGN KEY " + columnas + " -> " + tablaReferida + columnasReferidas;
        };
    }
}
