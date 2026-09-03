package modelador.aplicacion;

import java.util.List;
import modelador.conversion.Conversor;
import modelador.conversion.ResultadoConversion;
import modelador.conversion.Traza;
import modelador.dominio.er.ModeloER;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.tipos.Aviso;
import modelador.generacion.CatalogoGeneradores;
import modelador.generacion.Destino;
import modelador.persistencia.IRepositorio;
import modelador.persistencia.RepositorioJson;

// Punto de entrada unico de la interfaz: guarda el modelo abierto y coordina
// conversion, generacion y persistencia. La ventana habla solo con esta clase.
public class Fachada {

    private ModeloER modelo;
    private EsquemaRelacional esquema;
    // Se guarda entero: la traza explica por que el esquema salio asi.
    private ResultadoConversion resultado;
    private final IRepositorio repositorio;
    private final Conversor conversor;
    private String rutaActual;

    public Fachada() {
        this(new RepositorioJson());
    }

    public Fachada(IRepositorio repositorio) {
        this.modelo = new ModeloER();
        this.repositorio = repositorio;
        this.conversor = new Conversor();
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
        resultado = null;
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
        resultado = null;
        rutaActual = ruta;
    }

    public String getExtensionDeModelo() {
        return repositorio.getExtension();
    }

    // --- Conversion y generacion ---

    // El conversor ya esta operativo: aqui se guarda el esquema para que
    // lo usen la pestana relacional y la de codigo.
    public boolean hayConversorDisponible() {
        return true;
    }

    // Convierte el modelo abierto en el esquema relacional y devuelve los
    // avisos de la conversion (errores, advertencias e informacion).
    public List<Aviso> convertir() {
        resultado = conversor.convertir(modelo);
        esquema = resultado.getEsquema();
        return resultado.getAvisos();
    }

    public String generarCodigo(Destino destino) {
        if (esquema == null) {
            throw new IllegalStateException(
                    "Todavia no hay esquema: primero hay que convertir el modelo.");
        }
        return CatalogoGeneradores.para(destino).generar(esquema);
    }

    /** Explicación de qué regla se aplicó a cada elemento en la conversión. */
    public List<Traza> getTraza() {
        return (resultado == null) ? List.of() : resultado.getTraza();
    }

    /** Escribe en disco el código del destino indicado. */
    public void exportarCodigo(Destino destino, String ruta) {
        String codigo = generarCodigo(destino);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(ruta), codigo,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("No se pudo exportar a " + ruta, e);
        }
    }

    public String nombreDeArchivo(Destino destino) {
        return "esquema." + destino.getExtension();
    }
}
