package modelador.dominio.relacional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.Severidad;


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
                revisarForanea(tabla, restriccion, avisos);
            }
            revisarNombresDeRestriccion(tabla, avisos);
        }

        return avisos;
    }

    /**
     * Comprueba que una clave foránea apunte a algo que exista de verdad: la
     * tabla, las columnas de ambos lados y el mismo número a cada lado. Sin
     * esto, el DDL se crea pero falla en el primer INSERT.
     */
    private void revisarForanea(Tabla tabla, Restriccion foranea, List<Aviso> avisos) {
        Tabla referida = buscarTabla(foranea.getTablaReferida());
        if (referida == null) {
            avisos.add(new Aviso(Severidad.ERROR,
                    "La restricción foránea hace referencia a la tabla inexistente '"
                            + foranea.getTablaReferida() + "'.", tabla.getNombre()));
            return;
        }
        if (foranea.getColumnas().size() != foranea.getColumnasReferidas().size()) {
            avisos.add(new Aviso(Severidad.ERROR,
                    "La clave foránea hacia '" + referida.getNombre()
                            + "' tiene distinto número de columnas a cada lado.",
                    tabla.getNombre()));
            return;
        }
        for (String columna : foranea.getColumnas()) {
            if (tabla.buscarColumna(columna) == null) {
                avisos.add(new Aviso(Severidad.ERROR,
                        "La clave foránea usa la columna '" + columna
                                + "', que no existe en esta tabla.", tabla.getNombre()));
            }
        }
        for (String columna : foranea.getColumnasReferidas()) {
            if (referida.buscarColumna(columna) == null) {
                avisos.add(new Aviso(Severidad.ERROR,
                        "La clave foránea referencia la columna '" + columna
                                + "', que no existe en '" + referida.getNombre() + "'.",
                        tabla.getNombre()));
            }
        }
    }

    /**
     * Dos restricciones con el mismo nombre en la misma tabla son un error de
     * sintaxis en varios motores.
     */
    private void revisarNombresDeRestriccion(Tabla tabla, List<Aviso> avisos) {
        List<String> vistos = new ArrayList<>();
        for (Restriccion restriccion : tabla.getRestricciones()) {
            String nombre = String.valueOf(restriccion.getTipo())
                    + String.valueOf(restriccion.getTablaReferida());
            if (restriccion.getTipo() == TipoRestriccion.FORANEA
                    && vistos.contains(nombre)) {
                avisos.add(new Aviso(Severidad.ADVERTENCIA,
                        "Hay dos claves foráneas hacia '" + restriccion.getTablaReferida()
                                + "': sus nombres chocarán en el DDL.", tabla.getNombre()));
            }
            vistos.add(nombre);
        }
    }
}
