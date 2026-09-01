package modelador.dominio.er;

/**
 * Representa un extremo de una relación, es decir, la participación de una
 * entidad en una relación del modelo Entidad-Relación.
 */
public class Participacion {

    private Cardinalidad cardinalidad;
    private Modalidad modalidad;
    private String entidad;
    private String rol;

    public Participacion(String entidad, Cardinalidad cardinalidad,
                         Modalidad modalidad, String rol) {
        if (entidad == null || entidad.isBlank()) {
            throw new IllegalArgumentException("La participación debe referenciar una entidad.");
        }
        if (cardinalidad == null) {
            throw new IllegalArgumentException("La cardinalidad no puede ser nula.");
        }
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad no puede ser nula.");
        }
        this.entidad = entidad;
        this.cardinalidad = cardinalidad;
        this.modalidad = modalidad;
        this.rol = rol;
    }

    public Cardinalidad getCardinalidad() {
        return cardinalidad;
    }

    public void setCardinalidad(Cardinalidad cardinalidad) {
        if (cardinalidad == null) {
            throw new IllegalArgumentException("La cardinalidad no puede ser nula.");
        }
        this.cardinalidad = cardinalidad;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad no puede ser nula.");
        }
        this.modalidad = modalidad;
    }

    /**
     * Devuelve el id de la entidad que participa en la relación.
     */
    public String getEntidad() {
        return entidad;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean esLadoMuchos() {
        return cardinalidad == Cardinalidad.MUCHOS;
    }

    public boolean esObligatoria() {
        return modalidad == Modalidad.TOTAL;
    }
}
