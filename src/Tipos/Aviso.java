/**
 * Aviso producido durante la validación de un modelo. Contiene la severidad,
 * el mensaje descriptivo y el elemento del modelo al que se refiere.
 */
public class Aviso {

    private Severidad severidad;
    private String mensaje;
    private String elemento;

    public Aviso(Severidad severidad, String mensaje, String elemento) {
        if (severidad == null) {
            throw new IllegalArgumentException("La severidad no puede ser nula.");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje del aviso no puede ser vacío.");
        }
        this.severidad = severidad;
        this.mensaje = mensaje;
        this.elemento = elemento;
    }

    public boolean esError() {
        return severidad == Severidad.ERROR;
    }

    public String getElemento() {
        return elemento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Severidad getSeveridad() {
        return severidad;
    }
}
