import java.util.List;

// Punto de entrada unico de la interfaz: guarda el modelo abierto y coordina
// conversion, generacion y persistencia. La ventana habla solo con esta clase.
public class Fachada {

    private ModeloER modelo;
    private EsquemaRelacional esquema;
    private final IRepositorio repositorio;
    private String rutaActual;

    public Fachada() {
        this(new RepositorioJson());
    }

    public Fachada(IRepositorio repositorio) {
        this.modelo = new ModeloER();
        this.repositorio = repositorio;
    }

    public ModeloER getModelo() {
        return modelo;
    }

    public EsquemaRelacional getEsquema() {
        return esquema;
    }

    public String getRutaActual() {
        return rutaActual;
    }

    public List<Aviso> validar() {
        return modelo.validar();
    }

    public void nuevo() {
        modelo = new ModeloER();
        esquema = null;
        rutaActual = null;
    }

    // --- Persistencia ---

    public void guardar(String ruta) {
        repositorio.guardar(modelo, ruta);
        rutaActual = ruta;
    }

    public void abrir(String ruta) {
        modelo = repositorio.cargar(ruta);
        esquema = null;
        rutaActual = ruta;
    }

    public String getExtensionDeModelo() {
        return repositorio.getExtension();
    }

    // --- Conversion y generacion ---

    // El conversor todavia no esta implementado; cuando lo este, aqui se
    // guarda el esquema para que lo usen la pestana relacional y la de codigo.
    public boolean hayConversorDisponible() {
        return false;
    }

    public String generarCodigo(Destino destino) {
        if (esquema == null) {
            throw new IllegalStateException(
                    "Todavia no hay esquema: primero hay que convertir el modelo.");
        }
        return CatalogoGeneradores.para(destino).generar(esquema);
    }

    public String nombreDeArchivo(Destino destino) {
        return "esquema." + destino.getExtension();
    }
}
