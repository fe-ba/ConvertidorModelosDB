import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Puente entre el lienzo y el modelo E-R: la unica fuente de verdad es ModeloER.
// Guarda solo lo que es estado de la vista: que hay seleccionado y los contadores.
public class Tablero {

    private static final int TOLERANCIA_LINEA = 7;
    private static final int ALCANCE_ATRIBUTO = 320;

    private final ModeloER modelo;
    private final List<Figura> seleccionados;
    private EnlaceVista enlaceSeleccionado;
    // Un contador por familia de simbolo, para que la numeracion no se mezcle.
    private final Map<String, Integer> contadores;
    private final List<Runnable> oyentes;
    // Limitaciones del modelo que la interfaz debe explicar al usuario.
    private String ultimoAviso;

    public Tablero() {
        this(new ModeloER());
    }

    public Tablero(ModeloER modelo) {
        this.modelo = modelo;
        this.seleccionados = new ArrayList<>();
        this.contadores = new HashMap<>();
        this.oyentes = new ArrayList<>();
    }

    public ModeloER getModelo() {
        return modelo;
    }

    // --- Figuras: proyeccion del modelo, recalculada al vuelo ---

    // Entidades y relaciones primero, atributos encima.
    public List<Figura> getFiguras() {
        List<Figura> figuras = new ArrayList<>();
        for (Entidad entidad : modelo.getEntidades()) {
            figuras.add(Figura.de(entidad));
        }
        for (Relacion relacion : modelo.getRelaciones()) {
            figuras.add(Figura.de(relacion));
        }
        for (Entidad entidad : modelo.getEntidades()) {
            for (Atributo atributo : entidad.getAtributos()) {
                figuras.add(Figura.deAtributo(atributo, entidad));
            }
        }
        return figuras;
    }

    public List<EnlaceVista> getEnlaces() {
        List<EnlaceVista> enlaces = new ArrayList<>();
        for (Relacion relacion : modelo.getRelaciones()) {
            for (Participacion parte : relacion.getParticipaciones()) {
                Entidad entidad = entidadPorId(parte.getEntidad());
                if (entidad != null) {
                    enlaces.add(new EnlaceVista(relacion, parte, entidad));
                }
            }
        }
        return enlaces;
    }

    public List<Figura> atributosDe(Figura figura) {
        List<Figura> lista = new ArrayList<>();
        if (figura.esEntidad()) {
            Entidad entidad = (Entidad) figura.getElemento();
            for (Atributo atributo : entidad.getAtributos()) {
                lista.add(Figura.deAtributo(atributo, entidad));
            }
        }
        return lista;
    }

    // Un atributo guarda su posicion relativa al duenno, asi que lo sigue solo.
    // Aqui solo hay que evitar moverlo dos veces cuando ambos estan
    // seleccionados: se mueve el duenno y el atributo se queda quieto.
    public List<Figura> arrastrablesCon(List<Figura> seleccion) {
        List<Figura> todos = new ArrayList<>();
        for (Figura figura : seleccion) {
            boolean duennoTambienSeleccionado = figura.esAtributo()
                    && contieneElemento(seleccion, figura.getDuenno());
            if (!duennoTambienSeleccionado) {
                todos.add(figura);
            }
        }
        return todos;
    }

    private boolean contieneElemento(List<Figura> figuras, ElementoDelModelo buscado) {
        for (Figura figura : figuras) {
            if (figura.getElemento() == buscado) {
                return true;
            }
        }
        return false;
    }

    public List<EnlaceVista> enlacesDe(Figura figura) {
        List<EnlaceVista> suyos = new ArrayList<>();
        for (EnlaceVista enlace : getEnlaces()) {
            boolean toca = enlace.getRelacion() == figura.getElemento()
                    || enlace.getEntidad() == figura.getElemento();
            if (toca) {
                suyos.add(enlace);
            }
        }
        return suyos;
    }

    public Figura otroExtremo(EnlaceVista enlace, Figura figura) {
        return enlace.getRelacion() == figura.getElemento()
                ? Figura.de(enlace.getEntidad()) : Figura.de(enlace.getRelacion());
    }

    // --- Alta ---

    public Figura agregar(TipoNodo tipo, int x, int y) {
        String nombre = nombrePorDefecto(tipo);
        switch (tipo) {
            case ENTIDAD:
            case ENTIDAD_DEBIL: {
                Entidad entidad = new Entidad(nombre, new Punto(x, y),
                        tipo == TipoNodo.ENTIDAD_DEBIL);
                modelo.agregarEntidad(entidad);
                Figura figura = Figura.de(entidad);
                seleccionarSolo(figura);
                return figura;
            }
            case RELACION:
            case RELACION_IDENTIFICADORA: {
                Relacion relacion = new Relacion(nombre, new Punto(x, y),
                        tipo == TipoNodo.RELACION_IDENTIFICADORA);
                modelo.agregarRelacion(relacion);
                Figura figura = Figura.de(relacion);
                seleccionarSolo(figura);
                return figura;
            }
            default: {
                Entidad duenno = entidadMasCercana(x, y);
                if (duenno == null) {
                    return null;
                }
                // EnumSet y no HashSet: el constructor de Atributo hace
                // EnumSet.copyOf, que revienta con una coleccion vacia normal
                Atributo atributo = new Atributo(nombre, TipoDato.TEXTO_CORTO,
                        Naturaleza.SIMPLE, java.util.EnumSet.noneOf(Marca.class),
                        new Punto(x - duenno.getPosicion().getX(),
                                  y - duenno.getPosicion().getY()));
                if (tipo == TipoNodo.ATRIBUTO_CLAVE) {
                    atributo.marcar(Marca.CLAVE, true);
                }
                // el constructor de Atributo suma (10,10) al desplazamiento
                // que recibe, asi que se fija despues para que caiga donde se solto
                atributo.setDesplazamiento(new Punto(
                        x - duenno.getPosicion().getX(),
                        y - duenno.getPosicion().getY()));
                duenno.agregarAtributo(atributo);
                Figura figura = Figura.deAtributo(atributo, duenno);
                seleccionarSolo(figura);
                return figura;
            }
        }
    }

    // Un atributo solo puede colgar de una entidad: el modelo no permite
    // atributos en las relaciones.
    public boolean puedeColocarse(TipoNodo tipo, int x, int y) {
        if (tipo != TipoNodo.ATRIBUTO && tipo != TipoNodo.ATRIBUTO_CLAVE) {
            return true;
        }
        return entidadMasCercana(x, y) != null;
    }

    // Numeracion por familia, saltando los nombres ya usados.
    public String nombrePorDefecto(TipoNodo tipo) {
        String base;
        switch (tipo) {
            case ENTIDAD: base = "Entidad"; break;
            case ENTIDAD_DEBIL: base = "Debil"; break;
            case RELACION: base = "Relacion"; break;
            case RELACION_IDENTIFICADORA: base = "RelacionIdent"; break;
            case ATRIBUTO_CLAVE: base = "id"; break;
            default: base = "atributo"; break;
        }
        int n = contadores.containsKey(base) ? contadores.get(base) + 1 : 1;
        while (existeNombre(base + n)) {
            n++;
        }
        contadores.put(base, n);
        return base + n;
    }

    private boolean existeNombre(String nombre) {
        for (Figura figura : getFiguras()) {
            if (figura.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    // --- Baja ---

    public void eliminar(List<Figura> condenadas) {
        for (Figura figura : new ArrayList<>(condenadas)) {
            eliminarUna(figura);
        }
        seleccionados.clear();
        avisar();
    }

    private void eliminarUna(Figura figura) {
        if (figura.esEntidad()) {
            modelo.quitarEntidad(figura.getId());
        } else if (figura.esAtributo()) {
            figura.getDuenno().quitarAtributo(figura.getNombre());
        } else {
            // ModeloER todavia no permite quitar relaciones
            ultimoAviso = "El modelo aun no permite eliminar relaciones. "
                    + "Quitale sus participaciones para dejarla suelta.";
        }
    }

    public void eliminarSeleccion() {
        if (enlaceSeleccionado != null) {
            eliminarEnlace(enlaceSeleccionado);
            return;
        }
        eliminar(new ArrayList<>(seleccionados));
    }

    // Quitar una participacion es quitar a esa entidad de la relacion.
    public void eliminarEnlace(EnlaceVista enlace) {
        enlace.getRelacion().quitarParticipacion(enlace.getEntidad().getId());
        enlaceSeleccionado = null;
        avisar();
    }

    public void vaciar() {
        for (Entidad entidad : new ArrayList<>(modelo.getEntidades())) {
            modelo.quitarEntidad(entidad.getId());
        }
        contadores.clear();
        seleccionados.clear();
        enlaceSeleccionado = null;
        if (!modelo.getRelaciones().isEmpty()) {
            ultimoAviso = "Quedan relaciones sueltas: el modelo aun no permite "
                    + "eliminarlas.";
        }
        avisar();
    }

    public boolean estaVacio() {
        return modelo.getEntidades().isEmpty() && modelo.getRelaciones().isEmpty();
    }

    // --- Enlaces ---

    public void enlazar(Figura origen, Figura destino) {
        if (origen == null || destino == null || origen.equals(destino)) {
            return;
        }
        if (origen.esAtributo() || destino.esAtributo()) {
            return;
        }
        // una participacion une siempre una relacion con una entidad
        Relacion relacion = null;
        Entidad entidad = null;
        if (origen.esRelacion() && destino.esEntidad()) {
            relacion = (Relacion) origen.getElemento();
            entidad = (Entidad) destino.getElemento();
        } else if (origen.esEntidad() && destino.esRelacion()) {
            relacion = (Relacion) destino.getElemento();
            entidad = (Entidad) origen.getElemento();
        } else {
            ultimoAviso = "Un enlace une una relacion con una entidad. "
                    + "Dos entidades se unen a traves de una relacion.";
            avisar();
            return;
        }
        if (relacion.participa(entidad.getId())) {
            return;
        }
        relacion.agregarParticipacion(new Participacion(entidad.getId(),
                Cardinalidad.MUCHOS, Modalidad.PARCIAL, ""));
        avisar();
    }

    // --- Seleccion ---

    public void seleccionarSolo(Figura figura) {
        seleccionados.clear();
        if (figura != null) {
            seleccionados.add(figura);
        }
        enlaceSeleccionado = null;
        avisar();
    }

    public void alternarSeleccion(Figura figura) {
        if (seleccionados.contains(figura)) {
            seleccionados.remove(figura);
        } else {
            seleccionados.add(figura);
        }
        enlaceSeleccionado = null;
        avisar();
    }

    public void seleccionarVarios(List<Figura> lista) {
        seleccionados.clear();
        seleccionados.addAll(lista);
        enlaceSeleccionado = null;
        avisar();
    }

    public void seleccionarEnlace(EnlaceVista enlace) {
        seleccionados.clear();
        enlaceSeleccionado = enlace;
        avisar();
    }

    public List<Figura> getSeleccionados() {
        return seleccionados;
    }

    // La unica figura seleccionada, o null si hay cero o varias.
    public Figura getSeleccionado() {
        return (seleccionados.size() == 1) ? seleccionados.get(0) : null;
    }

    public EnlaceVista getEnlaceSeleccionado() {
        return enlaceSeleccionado;
    }

    public boolean estaSeleccionado(Figura figura) {
        return seleccionados.contains(figura);
    }

    // --- Localizar bajo el raton ---

    public Figura figuraEn(int x, int y) {
        List<Figura> figuras = getFiguras();
        for (int i = figuras.size() - 1; i >= 0; i--) {
            if (figuras.get(i).contiene(x, y)) {
                return figuras.get(i);
            }
        }
        return null;
    }

    // Enlace cuya linea pasa a pocos pixeles del punto.
    public EnlaceVista enlaceEn(int x, int y) {
        for (EnlaceVista enlace : getEnlaces()) {
            Figura a = Figura.de(enlace.getRelacion());
            Figura b = Figura.de(enlace.getEntidad());
            if (Line2D.ptSegDist(a.getX(), a.getY(), b.getX(), b.getY(), x, y)
                    <= TOLERANCIA_LINEA) {
                return enlace;
            }
        }
        return null;
    }

    private Entidad entidadMasCercana(int x, int y) {
        Entidad mejor = null;
        double menor = Double.MAX_VALUE;
        for (Entidad entidad : modelo.getEntidades()) {
            double d = Math.hypot(entidad.getPosicion().getX() - x,
                    entidad.getPosicion().getY() - y);
            if (d < menor) {
                menor = d;
                mejor = entidad;
            }
        }
        return (menor < ALCANCE_ATRIBUTO) ? mejor : null;
    }

    private Entidad entidadPorId(String id) {
        for (Entidad entidad : modelo.getEntidades()) {
            if (entidad.getId().equals(id)) {
                return entidad;
            }
        }
        return null;
    }

    public Point posicionDe(Figura figura) {
        return new Point(figura.getX(), figura.getY());
    }

    // --- Avisos ---

    public String recogerAviso() {
        String aviso = ultimoAviso;
        ultimoAviso = null;
        return aviso;
    }

    public void avisarAlUsuario(String mensaje) {
        ultimoAviso = mensaje;
    }

    // Se dispara al cambiar la seleccion o el contenido.
    public void alCambiar(Runnable oyente) {
        oyentes.add(oyente);
    }

    public void avisar() {
        for (Runnable oyente : oyentes) {
            oyente.run();
        }
    }
}
