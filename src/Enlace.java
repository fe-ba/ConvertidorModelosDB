// TEMPORAL: luego sera una Participacion, con cardinalidad y modalidad.
public class Enlace {

    private NodoVista origen;
    private NodoVista destino;

    public Enlace(NodoVista origen, NodoVista destino) {
        this.origen = origen;
        this.destino = destino;
    }

    public boolean une(NodoVista a, NodoVista b) {
        return (origen == a && destino == b) || (origen == b && destino == a);
    }

    public boolean toca(NodoVista nodo) {
        return origen == nodo || destino == nodo;
    }

    public NodoVista getOrigen() {
        return origen;
    }

    public NodoVista getDestino() {
        return destino;
    }
}
