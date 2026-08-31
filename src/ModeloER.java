import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa el modelo Entidad-Relación que dibuja el usuario: el conjunto
 * de entidades y relaciones que lo componen, junto con su validación.
 */
public class ModeloER {

    private List<Entidad> entidades;
    private List<Relacion> relaciones;

    public ModeloER() {
        this.entidades = new ArrayList<>();
        this.relaciones = new ArrayList<>();
    }

    /**
     * Agrega una entidad al modelo, validando que no exista ya otra con el
     * mismo nombre. Devuelve el id de la entidad agregada.
     */
    public String agregarEntidad(Entidad entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula.");
        }
        if (buscarEntidad(entidad.getNombre()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una entidad llamada '" + entidad.getNombre() + "'.");
        }
        entidades.add(entidad);
        return entidad.getId();
    }

    /**
     * Agrega una relación al modelo, validando que no exista ya otra con el
     * mismo nombre. Devuelve el id de la relación agregada.
     */
    public String agregarRelacion(Relacion relacion) {
        if (relacion == null) {
            throw new IllegalArgumentException("La relación no puede ser nula.");
        }
        if (buscarRelacion(relacion.getNombre()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una relación llamada '" + relacion.getNombre() + "'.");
        }
        relaciones.add(relacion);
        return relacion.getId();
    }

    public Entidad buscarEntidad(String nombre) {
        return entidades.stream()
                .filter(e -> e.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }

    public Relacion buscarRelacion(String nombre) {
        return relaciones.stream()
                .filter(r -> r.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }

    public ElementoDelModelo buscarElemento(String nombre) {
        for (Entidad entidad : entidades) {
            if (entidad.getNombre().equalsIgnoreCase(nombre)) {
                return entidad;
            }
            Atributo atributo = entidad.buscarAtributo(nombre);
            if (atributo != null) {
                return atributo;
            }
        }
        return buscarRelacion(nombre);
    }

    /**
     * Devuelve la entidad que posee el atributo indicado, o {@code null}
     * si ningún atributo del modelo lleva ese nombre.
     */
    public Entidad propiedadDe(String nombreAtributo) {
        for (Entidad entidad : entidades) {
            if (entidad.buscarAtributo(nombreAtributo) != null) {
                return entidad;
            }
        }
        return null;
    }

    public List<Entidad> getEntidades() {
        return Collections.unmodifiableList(entidades);
    }

    public List<Relacion> getRelaciones() {
        return Collections.unmodifiableList(relaciones);
    }

    /**
     * Elimina una entidad del modelo por su id y, en cascada, retira todas
     * sus participaciones de las relaciones. Las relaciones que quedan sin
     * extremos suficientes también se eliminan.
     */
    public void quitarEntidad(String id) {
        boolean encontrada = entidades.removeIf(e -> e.getId().equals(id));
        if (!encontrada) {
            throw new IllegalArgumentException("No existe una entidad con el id indicado.");
        }
        for (Relacion relacion : new ArrayList<>(relaciones)) {
            relacion.quitarParticipacion(id);
            if (!relacionValida(relacion)) {
                relaciones.remove(relacion);
            }
        }
    }

    /**
     * Devuelve las relaciones en las que participa la entidad indicada.
     */
    public List<Relacion> relacionesDe(String idEntidad) {
        List<Relacion> resultado = new ArrayList<>();
        for (Relacion relacion : relaciones) {
            if (relacion.participa(idEntidad)) {
                resultado.add(relacion);
            }
        }
        return resultado;
    }

    /**
     * Indica si una relación conserva los extremos mínimos necesarios.
     */
    private boolean relacionValida(Relacion relacion) {
        return relacion.esRecursiva()
                ? relacion.grado() >= 1
                : relacion.grado() >= 2;
    }

    /**
     * Valida el modelo y devuelve la lista de avisos encontrados.
     */
    public List<Aviso> validar() {
        List<Aviso> avisos = new ArrayList<>();

        for (Entidad entidad : entidades) {
            if (entidad.getAtributos().isEmpty()) {
                avisos.add(new Aviso(Severidad.ERROR,
                        "La entidad debe tener al menos un atributo.",
                        entidad.getNombre()));
            }
            validarNombreYPosicion(entidad, avisos);

            boolean identificada = relaciones.stream()
                    .filter(Relacion::esIdentificadora)
                    .anyMatch(r -> r.participa(entidad.getId()));
            if (entidad.esDebil() && !identificada) {
                avisos.add(new Aviso(Severidad.ERROR,
                        "La entidad débil no está identificada por ninguna relación identificadora.",
                        entidad.getNombre()));
            }
        }

        for (Relacion relacion : relaciones) {
            if (!relacionValida(relacion)) {
                avisos.add(new Aviso(Severidad.ERROR,
                        "La relación debe tener al menos "
                                + (relacion.esRecursiva() ? 1 : 2)
                                + " participación(es).",
                        relacion.getNombre()));
            }
            validarNombreYPosicion(relacion, avisos);

            for (Participacion participacion : relacion.getParticipaciones()) {
                if (!existeEntidad(participacion.getEntidad())) {
                    avisos.add(new Aviso(Severidad.ERROR,
                            "La participación hace referencia a una entidad inexistente.",
                            relacion.getNombre()));
                }
            }
        }

        return avisos;
    }

    private boolean existeEntidad(String id) {
        return entidades.stream().anyMatch(e -> e.getId().equals(id));
    }

    private void validarNombreYPosicion(ElementoDelModelo elemento, List<Aviso> avisos) {
        if (elemento.getNombre() == null || elemento.getNombre().isBlank()) {
            avisos.add(new Aviso(Severidad.ADVERTENCIA,
                    "El elemento no tiene un nombre definido.", ""));
        }
        Punto posicion = elemento.getPosicion();
        if (posicion.getX() < 0 || posicion.getY() < 0) {
            avisos.add(new Aviso(Severidad.ADVERTENCIA,
                    "El elemento tiene una posición fuera del área válida.",
                    elemento.getNombre()));
        }
    }
}
