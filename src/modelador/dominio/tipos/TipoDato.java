package modelador.dominio.tipos;

/**
 * Tipos de dato básicos soportados por los atributos del modelo ER y por las
 * columnas del esquema relacional.
 */
public enum TipoDato {

    SERIAL(true),
    ENTERO(false),
    ENTERO_GRANDE(false),
    TEXTO_CORTO(false),
    TEXTO_MEDIO(false),
    TEXTO_LARGO(false),
    DECIMAL(false),
    REAL(false),
    BOOLEANO(false),
    FECHA(false),
    FECHA_HORA(false);

    private final boolean autonumerico;

    TipoDato(boolean autonumerico) {
        this.autonumerico = autonumerico;
    }

    /**
     * Devuelve el tipo de dato base sobre el que se construye este tipo.
     * SERIAL tiene como base ENTERO; el resto se toma a sí mismo.
     */
    public TipoDato getBase() {
        return this == SERIAL ? ENTERO : this;
    }

    /**
     * Indica si el tipo de dato se autogenera (autonumérico), como SERIAL.
     */
    public boolean esAutonumerico() {
        return autonumerico;
    }
}
