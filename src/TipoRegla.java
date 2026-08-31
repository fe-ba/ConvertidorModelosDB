/**
 * Identifica qué regla de conversión de Entidad-Relación a relacional se
 * aplicó para producir una parte del esquema (usado en {@link Traza}).
 */
public enum TipoRegla {
    ENTIDAD_FUERTE,
    ENTIDAD_DEBIL,
    UNO_A_UNO,
    UNO_A_MUCHOS,
    MUCHOS_A_MUCHOS,
    N_ARIA,
    ATRIBUTO_MULTIVALUADO,
    ATRIBUTO_DERIVADO
}
