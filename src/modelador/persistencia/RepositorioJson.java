package modelador.persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Marca;
import modelador.dominio.er.Modalidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Naturaleza;
import modelador.dominio.er.Participacion;
import modelador.dominio.er.Punto;
import modelador.dominio.er.Relacion;
import modelador.dominio.tipos.TipoDato;

// Persistencia del modelo E-R en un archivo JSON.
// Toda la conversion vive aqui y no repartida en el dominio: asi el modelo no
// sabe en que formato se guarda, y anadir otro formato no le afecta.
public class RepositorioJson implements IRepositorio {

    private static final int VERSION = 1;

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public void guardar(ModeloER modelo, String ruta) {
        try {
            Files.writeString(Path.of(ruta), serializar(modelo), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar en " + ruta, e);
        }
    }

    @Override
    public ModeloER cargar(String ruta) {
        try {
            return reconstruir(Files.readString(Path.of(ruta), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }

    // --- Escritura ---

    public String serializar(ModeloER modelo) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"version\": ").append(VERSION).append(",\n");
        sb.append("  \"entidades\": [\n");
        for (int i = 0; i < modelo.getEntidades().size(); i++) {
            sb.append(entidad(modelo.getEntidades().get(i)));
            sb.append(i < modelo.getEntidades().size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ],\n  \"relaciones\": [\n");
        for (int i = 0; i < modelo.getRelaciones().size(); i++) {
            sb.append(relacion(modelo.getRelaciones().get(i), modelo));
            sb.append(i < modelo.getRelaciones().size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private String entidad(Entidad entidad) {
        StringBuilder sb = new StringBuilder("    {\n");
        sb.append("      \"nombre\": ").append(Json.comillas(entidad.getNombre()))
          .append(",\n");
        sb.append("      \"esDebil\": ").append(entidad.esDebil()).append(",\n");
        sb.append("      \"posicion\": ").append(punto(entidad.getPosicion()))
          .append(",\n");
        sb.append("      \"atributos\": [\n");
        List<Atributo> atributos = entidad.getAtributos();
        for (int i = 0; i < atributos.size(); i++) {
            sb.append(atributo(atributos.get(i)));
            sb.append(i < atributos.size() - 1 ? ",\n" : "\n");
        }
        sb.append("      ]\n    }");
        return sb.toString();
    }

    private String atributo(Atributo atributo) {
        StringBuilder sb = new StringBuilder("        {");
        sb.append("\"nombre\": ").append(Json.comillas(atributo.getNombre()));
        sb.append(", \"tipo\": ").append(Json.comillas(atributo.getTipo().name()));
        sb.append(", \"naturaleza\": ")
          .append(Json.comillas(atributo.getNaturaleza().name()));
        sb.append(", \"marcas\": [");
        List<String> marcas = new ArrayList<>();
        for (Marca marca : atributo.getMarcas()) {
            marcas.add(Json.comillas(marca.name()));
        }
        sb.append(String.join(", ", marcas)).append("]");
        sb.append(", \"desplazamiento\": ").append(punto(atributo.getDesplazamiento()));
        return sb.append("}").toString();
    }

    private String relacion(Relacion relacion, ModeloER modelo) {
        StringBuilder sb = new StringBuilder("    {\n");
        sb.append("      \"nombre\": ").append(Json.comillas(relacion.getNombre()))
          .append(",\n");
        sb.append("      \"esIdentificadora\": ").append(relacion.esIdentificadora())
          .append(",\n");
        sb.append("      \"posicion\": ").append(punto(relacion.getPosicion()))
          .append(",\n");
        sb.append("      \"atributos\": [\n");
        List<Atributo> atributos = relacion.getAtributos();
        for (int i = 0; i < atributos.size(); i++) {
            sb.append(atributo(atributos.get(i)));
            sb.append(i < atributos.size() - 1 ? ",\n" : "\n");
        }
        sb.append("      ],\n");
        sb.append("      \"participaciones\": [\n");
        List<Participacion> partes = relacion.getParticipaciones();
        for (int i = 0; i < partes.size(); i++) {
            sb.append(participacion(partes.get(i), modelo));
            sb.append(i < partes.size() - 1 ? ",\n" : "\n");
        }
        sb.append("      ]\n    }");
        return sb.toString();
    }

    private String participacion(Participacion parte, ModeloER modelo) {
        StringBuilder sb = new StringBuilder("        {");
        sb.append("\"entidad\": ").append(Json.comillas(nombreDe(parte, modelo)));
        sb.append(", \"cardinalidad\": ")
          .append(Json.comillas(parte.getCardinalidad().name()));
        sb.append(", \"modalidad\": ")
          .append(Json.comillas(parte.getModalidad().name()));
        sb.append(", \"rol\": ").append(Json.comillas(parte.getRol()));
        return sb.append("}").toString();
    }

    // El archivo referencia por nombre y no por id: es legible, sobrevive a
    // que los identificadores se regeneren al cargar, y el modelo ya obliga a
    // que los nombres sean unicos.
    private String nombreDe(Participacion parte, ModeloER modelo) {
        for (Entidad entidad : modelo.getEntidades()) {
            if (entidad.getId().equals(parte.getEntidad())) {
                return entidad.getNombre();
            }
        }
        return "";
    }

    private String punto(Punto punto) {
        return "{\"x\": " + punto.getX() + ", \"y\": " + punto.getY() + "}";
    }

    // --- Lectura ---

    public ModeloER reconstruir(String texto) {
        Map<String, Object> raiz = Json.comoObjeto(Json.leer(texto));
        ModeloER modelo = new ModeloER();

        // El archivo referencia las entidades por nombre; aqui se traducen al
        // identificador que acaba de generarse.
        Map<String, String> porNombre = new HashMap<>();
        for (Object crudo : Json.comoLista(raiz.get("entidades"))) {
            Map<String, Object> datos = Json.comoObjeto(crudo);
            Entidad entidad = new Entidad(
                    Json.comoTexto(datos.get("nombre"), "SinNombre"),
                    punto(datos.get("posicion")),
                    Json.comoBooleano(datos.get("esDebil"), false));
            for (Object crudoAtributo : Json.comoLista(datos.get("atributos"))) {
                entidad.agregarAtributo(atributo(Json.comoObjeto(crudoAtributo)));
            }
            modelo.agregarEntidad(entidad);
            porNombre.put(entidad.getNombre(), entidad.getId());
        }

        for (Object crudo : Json.comoLista(raiz.get("relaciones"))) {
            Map<String, Object> datos = Json.comoObjeto(crudo);
            Relacion relacion = new Relacion(
                    Json.comoTexto(datos.get("nombre"), "SinNombre"),
                    punto(datos.get("posicion")),
                    Json.comoBooleano(datos.get("esIdentificadora"), false));
            for (Object crudoAtributo : Json.comoLista(datos.get("atributos"))) {
                relacion.agregarAtributo(atributo(Json.comoObjeto(crudoAtributo)));
            }
            for (Object crudoParte : Json.comoLista(datos.get("participaciones"))) {
                Map<String, Object> parte = Json.comoObjeto(crudoParte);
                String entidad = porNombre.get(
                        Json.comoTexto(parte.get("entidad"), ""));
                if (entidad == null) {
                    continue;   // participacion huerfana: se descarta
                }
                relacion.agregarParticipacion(new Participacion(entidad,
                        valorDe(Cardinalidad.class, parte.get("cardinalidad"),
                                Cardinalidad.MUCHOS),
                        valorDe(Modalidad.class, parte.get("modalidad"),
                                Modalidad.PARCIAL),
                        Json.comoTexto(parte.get("rol"), "")));
            }
            modelo.agregarRelacion(relacion);
        }
        return modelo;
    }

    private Atributo atributo(Map<String, Object> datos) {
        Set<Marca> marcas = EnumSet.noneOf(Marca.class);
        for (Object marca : Json.comoLista(datos.get("marcas"))) {
            Marca valor = valorDe(Marca.class, marca, null);
            if (valor != null) {
                marcas.add(valor);
            }
        }
        Punto desplazamiento = punto(datos.get("desplazamiento"));
        return new Atributo(
                Json.comoTexto(datos.get("nombre"), "atributo"),
                valorDe(TipoDato.class, datos.get("tipo"), TipoDato.TEXTO_CORTO),
                valorDe(Naturaleza.class, datos.get("naturaleza"), Naturaleza.SIMPLE),
                marcas, desplazamiento);
    }

    private Punto punto(Object crudo) {
        Map<String, Object> datos = Json.comoObjeto(crudo);
        return new Punto(Json.comoNumero(datos.get("x"), 0),
                Json.comoNumero(datos.get("y"), 0));
    }

    // Un valor desconocido no rompe la carga: se usa el de por defecto.
    private <T extends Enum<T>> T valorDe(Class<T> tipo, Object crudo, T pordefecto) {
        String nombre = Json.comoTexto(crudo, null);
        if (nombre == null) {
            return pordefecto;
        }
        try {
            return Enum.valueOf(tipo, nombre);
        } catch (IllegalArgumentException e) {
            return pordefecto;
        }
    }
}
