/**
 * Acción a ejecutar sobre una restricción de clave foránea cuando la fila
 * referida es actualizada o eliminada (ON UPDATE / ON DELETE).
 */

public enum AccionReferencial {
    CASCADA,
    RESTRINGIR,
    ANULAR,
    NINGUNA
}
