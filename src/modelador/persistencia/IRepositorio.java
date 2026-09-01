package modelador.persistencia;

import modelador.dominio.er.ModeloER;

// Guarda y recupera modelos E-R. Es interfaz porque exportar a otro formato
// (XMI para otra herramienta CASE) es una extension previsible.
public interface IRepositorio {

    // Extension de archivo sugerida, sin el punto.
    String getExtension();

    void guardar(ModeloER modelo, String ruta);

    ModeloER cargar(String ruta);
}
