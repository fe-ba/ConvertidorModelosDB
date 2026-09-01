package modelador.dominio.er;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Representa una relación del modelo Entidad-Relación, compuesta por las
 * participaciones (extremos) de las entidades que la componen.
 */
public class Relacion extends ElementoDelModelo {

    private List<Participacion> participaciones;
    private List<Atributo> atributos;
    private boolean esIdentificadora;

    public Relacion(String nombre, Punto posicion, boolean esIdentificadora) {
        super(nombre, posicion);
        this.participaciones = new ArrayList<>();
        this.atributos = new ArrayList<>();
        this.esIdentificadora = esIdentificadora;
    }

    public Participacion agregarParticipacion(Participacion participacion) {
        if (participacion == null) {
            throw new IllegalArgumentException("La participación no puede ser nula.");
        }
        participaciones.add(participacion);
        return participacion;
    }

    public List<Participacion> getParticipaciones() {
        return Collections.unmodifiableList(participaciones);
    }

    public boolean esIdentificadora() {
        return esIdentificadora;
    }

    public void setIdentificadora(boolean esIdentificadora) {
        this.esIdentificadora = esIdentificadora;
    }

    /**
     * Número de participaciones (extremos) de la relación.
     */
    public int grado() {
        return participaciones.size();
    }

    /**
     * Cuenta cuántas participaciones tienen la cardinalidad indicada.
     */
    public int contar(Cardinalidad cardinalidad) {
        return (int) participaciones.stream()
                .filter(p -> p.getCardinalidad() == cardinalidad)
                .count();
    }

    /**
     * Indica si la relación es recursiva, es decir, si una misma entidad
     * participa más de una vez en ella.
     */
    public boolean esRecursiva() {
        return participaciones.stream()
                .map(Participacion::getEntidad)
                .collect(Collectors.toSet())
                .size() < participaciones.size();
    }

    /**
     * Indica si la entidad indicada participa en la relación.
     */
    public boolean participa(String entidad) {
        return participaciones.stream()
                .anyMatch(p -> p.getEntidad().equals(entidad));
    }

    /**
     * Agrupa las participaciones por id de entidad.
     */
    public Map<String, List<Participacion>> porEntidad() {
        return participaciones.stream()
                .collect(Collectors.groupingBy(Participacion::getEntidad));
    }

    /**
     * Retira todas las participaciones de la entidad indicada.
     */
    public void quitarParticipacion(String entidad) {
        participaciones.removeIf(p -> p.getEntidad().equals(entidad));
    }

    /**
     * Añade un atributo propio de la relación.
     *
     * Una relación N:M puede llevar datos que no pertenecen a ninguna de las
     * dos entidades, como la nota de una inscripción.
     *
     * @param atributo atributo a añadir
     * @throws IllegalArgumentException si ya hay otro con el mismo nombre
     */
    public void agregarAtributo(Atributo atributo) {
        if (atributo == null) {
            throw new IllegalArgumentException("El atributo no puede ser nulo.");
        }
        if (buscarAtributo(atributo.getNombre()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un atributo llamado '" + atributo.getNombre() + "'.");
        }
        atributos.add(atributo);
    }

    public void quitarAtributo(String nombre) {
        atributos.removeIf(a -> a.getNombre().equals(nombre));
    }

    public Atributo buscarAtributo(String nombre) {
        return atributos.stream()
                .filter(a -> a.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    public List<Atributo> getAtributos() {
        return Collections.unmodifiableList(atributos);
    }

    /**
     * Los atributos que se vuelcan en la tabla que reciba la clave.
     *
     * Ni los derivados ni los multivaluados: los primeros se calculan y los
     * segundos irían a una tabla propia.
     */
    @Override
    public List<Atributo> atributosAlmacenables() {
        return atributos.stream()
                .filter(a -> a.getNaturaleza() == Naturaleza.SIMPLE)
                .collect(Collectors.toList());
    }
}
