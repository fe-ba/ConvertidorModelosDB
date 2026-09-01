package modelador.conversion;

import modelador.dominio.er.ModeloER;

/**
 * Contrato de un conversor de modelos Entidad-Relación a esquemas
 * relacionales. Permite que la Fachada dependa de una abstracción en vez de
 * la implementación concreta ({@link Conversor}).
 */
public interface IConversor {

    ResultadoConversion convertir(ModeloER modelo);
}
