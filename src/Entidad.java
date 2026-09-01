import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una entidad del modelo Entidad-Relación con sus atributos.
 */
public class Entidad extends ElementoDelModelo {

    private List<Atributo> atributos;
    private boolean esDebil;

    public Entidad(String nombre, Punto posicion, boolean esDebil) {
        super(nombre, posicion);
        this.atributos = new ArrayList<>();
        this.esDebil = esDebil;
    }

    public Atributo agregarAtributo(Atributo atributo) {
        if (atributo == null) {
            throw new IllegalArgumentException("El atributo no puede ser nulo.");
        }
        if (buscarAtributo(atributo.getNombre()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un atributo llamado '" + atributo.getNombre()
                            + "' en la entidad " + getNombre() + ".");
        }
        atributos.add(atributo);
        return atributo;
    }

    public Atributo buscarAtributo(String nombreAtributo) {
        return atributos.stream()
                .filter(a -> a.getNombre().equalsIgnoreCase(nombreAtributo))
                .findFirst()
                .orElse(null);
    }

    public List<Atributo> getAtributos() {
        return Collections.unmodifiableList(atributos);
    }

    public boolean esDebil() {
        return esDebil;
    }

    public void setDebil(boolean esDebil) {
        this.esDebil = esDebil;
    }

    /**
     * Atributos que pueden almacenarse como columnas (excluye derivados).
     */
    @Override
    public List<Atributo> atributosAlmacenables() {
        return atributos.stream()
                .filter(a -> a.getNaturaleza() != Naturaleza.DERIVADO)
                .collect(Collectors.toList());
    }

    /**
     * Atributos cuya naturaleza es multivaluada.
     */
    public List<Atributo> atributosMultivaluados() {
        return atributos.stream()
                .filter(a -> a.getNaturaleza() == Naturaleza.MULTIVALUADO)
                .collect(Collectors.toList());
    }

    /**
     * Atributos marcados como clave de la entidad.
     */
    public List<Atributo> clave() {
        return atributos.stream()
                .filter(Atributo::esClave)
                .collect(Collectors.toList());
    }

    /**
     * Retira el atributo indicado de la entidad.
     */
    public void quitarAtributo(String nombreAtributo) {
        atributos.removeIf(a -> a.getNombre().equalsIgnoreCase(nombreAtributo));
    }
}
