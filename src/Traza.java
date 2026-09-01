/**
 * Registra qué regla de conversión se aplicó y por qué, para poder explicar
 * después cómo se llegó a una tabla/columna/restricción concreta del
 * esquema relacional resultante.
 */
public class Traza {

    private TipoRegla regla;
    private String explicacion;

    public Traza(TipoRegla regla, String explicacion) {
        if (regla == null) {
            throw new IllegalArgumentException("La regla de la traza no puede ser nula.");
        }
        if (explicacion == null || explicacion.isBlank()) {
            throw new IllegalArgumentException("La explicación de la traza no puede ser vacía.");
        }
        this.regla = regla;
        this.explicacion = explicacion;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public TipoRegla getRegla() {
        return regla;
    }

    @Override
    public String toString() {
        return "[" + regla + "] " + explicacion;
    }
}
