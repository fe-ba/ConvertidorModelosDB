import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contenedor con el resultado de convertir un {@code ModeloER} en un
 * {@link EsquemaRelacional}: el esquema en sí, los avisos (errores o
 * advertencias) detectados durante el proceso, y la traza de qué regla
 * se aplicó en cada paso.
 *
 * Nota: {@code Aviso} es una clase de otro miembro del equipo; aquí solo se
 * usa su API pública tal como aparece en el diagrama, incluyendo
 * {@code esError(): boolean}.
 */
public class ResultadoConversion {

    private List<Aviso> avisos;
    private EsquemaRelacional esquema;
    private List<Traza> traza;

    public ResultadoConversion() {
        this.avisos = new ArrayList<>();
        this.esquema = new EsquemaRelacional();
        this.traza = new ArrayList<>();
    }

    public void advertir(Aviso aviso) {
        if (aviso == null) {
            throw new IllegalArgumentException("El aviso no puede ser nulo.");
        }
        avisos.add(aviso);
    }

    public void anotar(TipoRegla regla, String explicacion) {
        traza.add(new Traza(regla, explicacion));
    }

    public List<Aviso> getAvisos() {
        return Collections.unmodifiableList(avisos);
    }

    public EsquemaRelacional getEsquema() {
        return esquema;
    }

    public List<Traza> getTraza() {
        return Collections.unmodifiableList(traza);
    }

    /**
     * Indica si entre los avisos registrados hay al menos uno de severidad
     * de error (a diferencia de una simple advertencia).
     */
    public boolean hayErrores() {
        return avisos.stream().anyMatch(Aviso::esError);
    }
}
