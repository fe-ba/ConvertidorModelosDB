package modelador.generacion;

// Fabrica de generadores. La interfaz llena su desplegable con Destino.values().
public final class CatalogoGeneradores {

    private CatalogoGeneradores() {
    }

    public static IGeneradorDeCodigo para(Destino destino) {
        return destino.esSQL() ? new GeneradorSQL(destino) : new GeneradorObjetos(destino);
    }
}
