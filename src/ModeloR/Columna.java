
public class Columna {

    private String nombre;
    private TipoDato tipo;
    private boolean admiteNulos;

    public Columna(String nombre, TipoDato tipo, boolean admiteNulos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la columna no puede ser vacío.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de dato de la columna no puede ser nulo.");
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.admiteNulos = admiteNulos;
    }

    public boolean admiteNulos() { //Aca no entiendo bien entonces se retorna y ya
        return admiteNulos;
    }

    public boolean esAutonumerica() {
        return tipo.esAutonumerico();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoDato getTipo() {
        return tipo;
    }

    public void setAdmiteNulos(boolean admiteNulos) {
        this.admiteNulos = admiteNulos;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la columna no puede ser vacío.");
        }
        this.nombre = nombre;
    }

}
