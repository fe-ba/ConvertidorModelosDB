// Una participacion junto a la relacion que la contiene, para poder dibujarla
// y seleccionarla. Participacion no guarda a quien pertenece.
public class EnlaceVista {

    private final Relacion relacion;
    private final Participacion parte;
    private final Entidad entidad;

    public EnlaceVista(Relacion relacion, Participacion parte, Entidad entidad) {
        this.relacion = relacion;
        this.parte = parte;
        this.entidad = entidad;
    }

    public Relacion getRelacion() {
        return relacion;
    }

    public Participacion getParte() {
        return parte;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public boolean es(Participacion otra) {
        return parte == otra;
    }
}
