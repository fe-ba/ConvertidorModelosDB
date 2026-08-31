
public class Conversor implements IConversor {

    @Override
    public ResultadoConversion convertir(ModeloER modelo) {
        throw new UnsupportedOperationException();
    }

    public void convertirEntidades(ModeloER modelo, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void propagarClavesDebiles(ModeloER modelo, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void convertirRelaciones(ModeloER modelo, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void aplicarRegla(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public TipoRegla firma(Relacion relacion) {
        throw new UnsupportedOperationException();
    }

    public Participacion ladoUno(Relacion relacion) {
        throw new UnsupportedOperationException();
    }

    public Participacion ladoMuchos(Relacion relacion) {
        throw new UnsupportedOperationException();
    }

    public Participacion ladoDestino(Relacion relacion) {
        throw new UnsupportedOperationException();
    }

    public void reglaUnoAUno(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void reglaUnoAMuchos(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void reglaMuchosAMuchos(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void reglaNAria(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public void extraerMultivaluados(ModeloER modelo, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public Tabla crearTabla(ElementoDelModelo elemento, OrigenTabla origen, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }

    public Tabla tablaDe(String idElemento, ResultadoConversion resultado) {
        throw new UnsupportedOperationException();
    }
}
