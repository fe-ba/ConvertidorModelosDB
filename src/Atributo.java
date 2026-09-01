import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Representa un atributo de una entidad del modelo Entidad-Relación.
 */
public class Atributo extends ElementoDelModelo {

    private TipoDato tipo;
    private Naturaleza naturaleza;
    private Set<Marca> marcas;
    private Punto desplazamiento;

    public Atributo(String nombre, TipoDato tipo, Naturaleza naturaleza,
                    Set<Marca> marcas, Punto posicion) {
        super(nombre, posicion);
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de dato del atributo no puede ser nulo.");
        }
        if (naturaleza == null) {
            throw new IllegalArgumentException("La naturaleza del atributo no puede ser nula.");
        }
        if (marcas == null) {
            marcas = EnumSet.noneOf(Marca.class);
        }
        this.tipo = tipo;
        this.naturaleza = naturaleza;
        this.marcas = EnumSet.copyOf(marcas);
        this.desplazamiento = posicion.desplazado(10, 10);
    }

    public TipoDato getTipo() {
        return tipo;
    }

    public void setTipo(TipoDato tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de dato del atributo no puede ser nulo.");
        }
        this.tipo = tipo;
    }

    public Naturaleza getNaturaleza() {
        return naturaleza;
    }

    public void setNaturaleza(Naturaleza naturaleza) {
        if (naturaleza == null) {
            throw new IllegalArgumentException("La naturaleza del atributo no puede ser nula.");
        }
        this.naturaleza = naturaleza;
    }

    public Set<Marca> getMarcas() {
        return Collections.unmodifiableSet(marcas);
    }

    /**
     * Añade o retira una marca del atributo.
     */
    public void marcar(Marca marca, boolean activa) {
        if (marca == null) {
            throw new IllegalArgumentException("La marca no puede ser nula.");
        }
        if (activa) {
            marcas.add(marca);
        } else {
            marcas.remove(marca);
        }
    }

    public boolean esClave() {
        return marcas.contains(Marca.CLAVE);
    }

    public boolean esObligatorio() {
        return marcas.contains(Marca.OBLIGATORIO);
    }

    public boolean esUnico() {
        return marcas.contains(Marca.UNICO);
    }

    public Punto getDesplazamiento() {
        return desplazamiento;
    }

    public void setDesplazamiento(Punto desplazamiento) {
        this.desplazamiento = desplazamiento;
    }

    /**
     * Un atributo individual no produce columnas almacenables propias.
     */
    @Override
    public List<Atributo> atributosAlmacenables() {
        return Collections.emptyList();
    }
}
