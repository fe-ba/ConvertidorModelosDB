import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Contenido del diagrama. TEMPORAL: luego lo sustituye ModeloER.
public class Tablero {

    private static final int TOLERANCIA_LINEA = 7;
    private static final int ALCANCE_ATRIBUTO = 320;

    private List<NodoVista> nodos;
    private List<Enlace> enlaces;
    private List<NodoVista> seleccionados;
    private Enlace enlaceSeleccionado;
    // Un contador por familia, para que la numeracion no se mezcle.
    private Map<String, Integer> contadores;
    private List<Runnable> oyentes;

    public Tablero() {
        this.nodos = new ArrayList<NodoVista>();
        this.enlaces = new ArrayList<Enlace>();
        this.seleccionados = new ArrayList<NodoVista>();
        this.contadores = new HashMap<String, Integer>();
        this.oyentes = new ArrayList<Runnable>();
    }

    // --- Alta ---
    public NodoVista agregar(TipoNodo tipo, String nombre, int x, int y) {
        NodoVista nodo = new NodoVista(tipo, nombre, x, y);
        if (nodo.esAtributo()) {
            nodo.setPadre(nodoMasCercano(x, y));
        }
        nodos.add(nodo);
        seleccionarSolo(nodo);
        return nodo;
    }

    // Un atributo solo tiene sentido colgado de algo.
    public boolean puedeColocarse(TipoNodo tipo, int x, int y) {
        boolean esAtributo = (tipo == TipoNodo.ATRIBUTO
                || tipo == TipoNodo.ATRIBUTO_CLAVE);
        return !esAtributo || nodoMasCercano(x, y) != null;
    }

    // Numeracion por familia, saltando los numeros ya usados.
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
        for (NodoVista nodo : nodos) {
            if (nodo.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    // --- Baja ---
    // Unico camino de borrado: arrastra los atributos colgados y los enlaces que queden sueltos.
    public void eliminarNodos(List<NodoVista> condenados) {
        if (condenados.isEmpty()) {
            return;
        }
        List<NodoVista> quedan = new ArrayList<NodoVista>();
        for (NodoVista n : nodos) {
            if (!condenados.contains(n) && !condenados.contains(n.getPadre())) {
                quedan.add(n);
            }
        }
        List<Enlace> enlacesQueQuedan = new ArrayList<Enlace>();
        for (Enlace e : enlaces) {
            if (quedan.contains(e.getOrigen()) && quedan.contains(e.getDestino())) {
                enlacesQueQuedan.add(e);
            }
        }
        nodos = quedan;
        enlaces = enlacesQueQuedan;
        seleccionados.retainAll(nodos);
        avisar();
    }

    public void eliminarNodo(NodoVista nodo) {
        List<NodoVista> uno = new ArrayList<NodoVista>();
        uno.add(nodo);
        eliminarNodos(uno);
    }

    // Borra los nodos seleccionados, o el enlace seleccionado.
    public void eliminarSeleccion() {
        if (enlaceSeleccionado != null) {
            eliminarEnlace(enlaceSeleccionado);
            return;
        }
        eliminarNodos(new ArrayList<NodoVista>(seleccionados));
    }

    public void eliminarEnlace(Enlace enlace) {
        enlaces.remove(enlace);
        if (enlace == enlaceSeleccionado) {
            enlaceSeleccionado = null;
        }
        avisar();
    }

    public void vaciar() {
        nodos.clear();
        enlaces.clear();
        contadores.clear();
        seleccionados.clear();
        enlaceSeleccionado = null;
        avisar();
    }

    // --- Enlaces ---
    // No enlaza atributos, ni un nodo consigo mismo, ni repite un enlace.
    public void enlazar(NodoVista origen, NodoVista destino) {
        if (origen == null || destino == null || origen == destino) {
            return;
        }
        if (origen.esAtributo() || destino.esAtributo()) {
            return;
        }
        for (Enlace e : enlaces) {
            if (e.une(origen, destino)) {
                return;   // ya estaban unidos
            }
        }
        enlaces.add(new Enlace(origen, destino));
        avisar();
    }

    public List<Enlace> enlacesDe(NodoVista nodo) {
        List<Enlace> suyos = new ArrayList<Enlace>();
        for (Enlace e : enlaces) {
            if (e.toca(nodo)) {
                suyos.add(e);
            }
        }
        return suyos;
    }

    // Atributos colgados de este nodo.
    public List<NodoVista> atributosDe(NodoVista nodo) {
        List<NodoVista> suyos = new ArrayList<NodoVista>();
        for (NodoVista n : nodos) {
            if (n.getPadre() == nodo) {
                suyos.add(n);
            }
        }
        return suyos;
    }

    public NodoVista otroExtremo(Enlace enlace, NodoVista nodo) {
        return (enlace.getOrigen() == nodo) ? enlace.getDestino() : enlace.getOrigen();
    }

    // --- Seleccion ---
    public void seleccionarSolo(NodoVista nodo) {
        seleccionados.clear();
        if (nodo != null) {
            seleccionados.add(nodo);
        }
        enlaceSeleccionado = null;
        avisar();
    }

    public void alternarSeleccion(NodoVista nodo) {
        if (seleccionados.contains(nodo)) {
            seleccionados.remove(nodo);
        } else {
            seleccionados.add(nodo);
        }
        enlaceSeleccionado = null;
        avisar();
    }

    public void seleccionarVarios(List<NodoVista> lista) {
        seleccionados.clear();
        seleccionados.addAll(lista);
        enlaceSeleccionado = null;
        avisar();
    }

    public void seleccionarEnlace(Enlace enlace) {
        seleccionados.clear();
        enlaceSeleccionado = enlace;
        avisar();
    }

    public List<NodoVista> getSeleccionados() {
        return seleccionados;
    }

    // El unico seleccionado, o null si hay cero o varios.
    public NodoVista getSeleccionado() {
        return (seleccionados.size() == 1) ? seleccionados.get(0) : null;
    }

    public Enlace getEnlaceSeleccionado() {
        return enlaceSeleccionado;
    }

    public boolean estaSeleccionado(NodoVista nodo) {
        return seleccionados.contains(nodo);
    }

    // --- Consultas ---
    public List<NodoVista> getNodos() {
        return nodos;
    }

    public List<Enlace> getEnlaces() {
        return enlaces;
    }

    public boolean estaVacio() {
        return nodos.isEmpty();
    }

    public NodoVista nodoEn(int x, int y) {
        for (int i = nodos.size() - 1; i >= 0; i--) {
            if (nodos.get(i).contiene(x, y)) {
                return nodos.get(i);
            }
        }
        return null;
    }

    // Enlace cuya linea pasa a pocos pixeles del punto.
    public Enlace enlaceEn(int x, int y) {
        for (Enlace e : enlaces) {
            NodoVista a = e.getOrigen();
            NodoVista b = e.getDestino();
            if (Line2D.ptSegDist(a.getX(), a.getY(), b.getX(), b.getY(), x, y)
                    <= TOLERANCIA_LINEA) {
                return e;
            }
        }
        return null;
    }

    public NodoVista nodoMasCercano(int x, int y) {
        NodoVista mejor = null;
        double menor = Double.MAX_VALUE;
        for (NodoVista n : nodos) {
            if (n.esAtributo()) {
                continue;
            }
            double d = Math.hypot(n.getX() - x, n.getY() - y);
            if (d < menor) {
                menor = d;
                mejor = n;
            }
        }
        return (menor < ALCANCE_ATRIBUTO) ? mejor : null;
    }

    // --- Avisos ---
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
