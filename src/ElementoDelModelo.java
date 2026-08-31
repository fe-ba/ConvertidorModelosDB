import java.util.UUID;

/**
 * Clase base de todos los elementos que forman parte de un modelo
 * Entidad-Relación (entidades, relaciones y atributos).
 */
public abstract class ElementoDelModelo {

    private String id;
    private String nombre;
    private Punto posicion;

    protected ElementoDelModelo(String nombre, Punto posicion) {
        if (posicion == null) {
            throw new IllegalArgumentException("La posición del elemento no puede ser nula.");
        }
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.posicion = posicion;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Punto getPosicion() {
        return posicion;
    }

    public void setPosicion(Punto posicion) {
        if (posicion == null) {
            throw new IllegalArgumentException("La posición del elemento no puede ser nula.");
        }
        this.posicion = posicion;
    }
}
