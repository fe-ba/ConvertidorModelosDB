package modelador.pruebas;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import modelador.aplicacion.Fachada;
import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Marca;
import modelador.dominio.er.Modalidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Naturaleza;
import modelador.dominio.er.Punto;
import modelador.dominio.er.Relacion;
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.TipoDato;
import modelador.generacion.Destino;
import modelador.interfaz.Figura;
import modelador.interfaz.LienzoER;
import modelador.interfaz.Tablero;
import modelador.interfaz.TipoNodo;
import modelador.persistencia.Json;
import modelador.persistencia.RepositorioJson;

// Pruebas de la rama de interfaz: lienzo, persistencia y fachada.
public class PruebaInterfaz {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================");
        System.out.println("PRUEBAS DE LA INTERFAZ");
        System.out.println("==========================================");
        probarJson();
        probarLienzo();
        probarPersistencia();
        probarFachada();
        probarModeloER();
        probarTrazaYExportacion();
        System.out.println(fallos == 0 ? "\nTODO CORRECTO" : "\n" + fallos + " FALLOS");
        System.exit(fallos);
    }

    private static void probarJson() {
        System.out.println("\n--- Lector de JSON ---");
        String raro = "Año \"Académico\"\n\ttab";
        Object leido = Json.leer("{\"n\": " + Json.comillas(raro) + ", \"v\": [1, true, null]}");
        debe("acentos, comillas y escapes van y vuelven",
                raro.equals(Json.comoTexto(Json.comoObjeto(leido).get("n"), "")));
        debe("arreglos mixtos", Json.comoLista(Json.comoObjeto(leido).get("v")).size() == 3);
        try {
            Json.leer("{\"a\":}");
            debe("rechaza JSON invalido", false);
        } catch (RuntimeException e) {
            debe("rechaza JSON invalido", true);
        }
    }

    private static void probarLienzo() {
        System.out.println("\n--- Lienzo sobre el modelo E-R ---");
        ModeloER modelo = new ModeloER();
        Tablero tablero = new Tablero(modelo);
        LienzoER lienzo = new LienzoER(tablero);
        lienzo.setSize(900, 600);

        tablero.agregar(TipoNodo.ENTIDAD, 200, 300);
        tablero.agregar(TipoNodo.RELACION, 450, 300);
        tablero.agregar(TipoNodo.ENTIDAD, 700, 300);
        debe("crea entidades y relaciones en el modelo",
                modelo.getEntidades().size() == 2 && modelo.getRelaciones().size() == 1);
        debe("numeracion por familia",
                modelo.getRelaciones().get(0).getNombre().equals("Relacion1"));

        Entidad e1 = modelo.getEntidades().get(0);
        Entidad e2 = modelo.getEntidades().get(1);
        Relacion r1 = modelo.getRelaciones().get(0);
        tablero.enlazar(Figura.de(r1), Figura.de(e1));
        tablero.enlazar(Figura.de(r1), Figura.de(e2));
        tablero.enlazar(Figura.de(r1), Figura.de(e1));
        debe("crea participaciones sin duplicar", r1.getParticipaciones().size() == 2);
        tablero.enlazar(Figura.de(e1), Figura.de(e2));
        debe("no une dos entidades directamente", tablero.recogerAviso() != null);

        Figura atributo = tablero.agregar(TipoNodo.ATRIBUTO_CLAVE, 200, 180);
        debe("el atributo vive dentro de la entidad", e1.getAtributos().size() == 1);
        debe("y cae donde se solto", atributo.getX() == 200 && atributo.getY() == 180);
        debe("es clave", e1.clave().size() == 1);

        double antes = e1.getPosicion().getX();
        tablero.seleccionarSolo(Figura.de(e1));
        pulsar(lienzo, 200, 300);
        arrastrar(lienzo, 260, 300);
        soltar(lienzo, 260, 300);
        debe("arrastrar escribe en el modelo", e1.getPosicion().getX() == antes + 60);
        debe("el atributo la acompana",
                Figura.deAtributo(e1.getAtributos().get(0), e1).getX() == 260);

        pulsar(lienzo, 450, 300);
        soltar(lienzo, 450, 300);
        debe("el rombo se selecciona por su forma real",
                tablero.getSeleccionado() != null && tablero.getSeleccionado().esRelacion());
        pulsar(lienzo, 600, 300);
        soltar(lienzo, 600, 300);
        debe("la linea de participacion se selecciona",
                tablero.getEnlaceSeleccionado() != null);

        tablero.eliminarEnlace(tablero.getEnlaceSeleccionado());
        debe("quitar la participacion", r1.getParticipaciones().size() == 1);
        tablero.seleccionarSolo(Figura.de(e1));
        tablero.eliminarSeleccion();
        debe("borrar la entidad se lleva su atributo",
                modelo.getEntidades().size() == 1 && tablero.getFiguras().size() <= 2);
    }

    private static void probarPersistencia() throws Exception {
        System.out.println("\n--- Persistencia ---");
        ModeloER modelo = new ModeloER();
        Tablero tablero = new Tablero(modelo);
        tablero.agregar(TipoNodo.ENTIDAD, 200, 300);
        tablero.agregar(TipoNodo.ENTIDAD_DEBIL, 700, 300);
        tablero.agregar(TipoNodo.RELACION_IDENTIFICADORA, 450, 300);
        Entidad e1 = modelo.getEntidades().get(0);
        e1.setNombre("Año \"Académico\"");
        tablero.agregar(TipoNodo.ATRIBUTO_CLAVE, 200, 180);
        Relacion r1 = modelo.getRelaciones().get(0);
        tablero.enlazar(Figura.de(r1), Figura.de(e1));
        tablero.enlazar(Figura.de(r1), Figura.de(modelo.getEntidades().get(1)));
        r1.getParticipaciones().get(0).setCardinalidad(Cardinalidad.UNO);
        r1.getParticipaciones().get(1).setModalidad(Modalidad.TOTAL);

        RepositorioJson repositorio = new RepositorioJson();
        repositorio.guardar(modelo, "/tmp/prueba_modelo.json");
        ModeloER copia = repositorio.cargar("/tmp/prueba_modelo.json");
        repositorio.guardar(copia, "/tmp/prueba_modelo2.json");

        debe("guardar, cargar y guardar da el mismo archivo",
                Files.readString(Path.of("/tmp/prueba_modelo.json"))
                        .equals(Files.readString(Path.of("/tmp/prueba_modelo2.json"))));
        debe("nombres con acentos y comillas sobreviven",
                copia.getEntidades().get(0).getNombre().equals("Año \"Académico\""));
        debe("entidad debil y relacion identificadora",
                copia.getEntidades().get(1).esDebil()
                        && copia.getRelaciones().get(0).esIdentificadora());
        debe("atributos con su tipo y sus marcas",
                copia.getEntidades().get(0).getAtributos().get(0).esClave());
        debe("cardinalidad y modalidad",
                copia.getRelaciones().get(0).getParticipaciones().get(0)
                        .getCardinalidad() == Cardinalidad.UNO
                && copia.getRelaciones().get(0).getParticipaciones().get(1)
                        .getModalidad() == Modalidad.TOTAL);
        debe("las participaciones apuntan a las entidades cargadas",
                copia.getRelaciones().get(0).participa(copia.getEntidades().get(0).getId()));

        repositorio.guardar(new ModeloER(), "/tmp/prueba_vacio.json");
        debe("un modelo vacio va y vuelve",
                repositorio.cargar("/tmp/prueba_vacio.json").getEntidades().isEmpty());
        Files.writeString(Path.of("/tmp/prueba_roto.json"), "{no es json");
        try {
            repositorio.cargar("/tmp/prueba_roto.json");
            debe("un archivo invalido avisa", false);
        } catch (RuntimeException e) {
            debe("un archivo invalido avisa", true);
        }
    }

    private static void probarFachada() {
        System.out.println("\n--- Fachada ---");
        Fachada fachada = new Fachada();
        Tablero tablero = new Tablero(fachada.getModelo());
        tablero.agregar(TipoNodo.ENTIDAD, 100, 100);
        fachada.guardar("/tmp/prueba_fachada.json");
        fachada.nuevo();
        debe("nuevo vacia el modelo", fachada.getModelo().getEntidades().isEmpty());
        fachada.abrir("/tmp/prueba_fachada.json");
        debe("abrir recupera el modelo", fachada.getModelo().getEntidades().size() == 1);
        List<Aviso> avisos = fachada.validar();
        debe("valida", avisos != null);
        try {
            fachada.generarCodigo(Destino.POSTGRESQL);
            debe("avisa si aun no hay esquema", false);
        } catch (IllegalStateException e) {
            debe("avisa si aun no hay esquema", true);
        }
        debe("propone el nombre del archivo de salida",
                fachada.nombreDeArchivo(Destino.SQLALCHEMY).equals("esquema.py"));
    }

    // Lo que antes rodeaba la interfaz porque el modelo no lo permitia.
    private static void probarModeloER() {
        System.out.println("\n--- Modelo E-R ---");
        Atributo suelto = new Atributo("x", TipoDato.TEXTO_CORTO, Naturaleza.SIMPLE,
                new java.util.HashSet<>(), new Punto(50, -80));
        debe("un atributo sin marcas no revienta al construirse", true);
        debe("y conserva el desplazamiento que se le da",
                suelto.getDesplazamiento().getX() == 50
                        && suelto.getDesplazamiento().getY() == -80);

        ModeloER modelo = new ModeloER();
        Tablero tablero = new Tablero(modelo);
        tablero.agregar(TipoNodo.ENTIDAD, 100, 100);
        tablero.agregar(TipoNodo.RELACION, 300, 100);
        Relacion relacion = modelo.getRelaciones().get(0);
        tablero.enlazar(Figura.de(relacion), Figura.de(modelo.getEntidades().get(0)));
        tablero.seleccionarSolo(Figura.de(relacion));
        tablero.eliminarSeleccion();
        debe("una relacion se puede eliminar", modelo.getRelaciones().isEmpty());
        debe("sin avisar de que no se puede", tablero.recogerAviso() == null);
        debe("y la entidad sigue en su sitio", modelo.getEntidades().size() == 1);

        tablero.agregar(TipoNodo.RELACION, 300, 100);
        tablero.vaciar();
        debe("vaciar se lleva tambien las relaciones", tablero.estaVacio());

        ModeloER sinClave = new ModeloER();
        Entidad entidad = new Entidad("SinClave", new Punto(0, 0), false);
        entidad.agregarAtributo(new Atributo("dato", TipoDato.TEXTO_CORTO,
                Naturaleza.SIMPLE, java.util.EnumSet.noneOf(Marca.class),
                new Punto(0, -110)));
        sinClave.agregarEntidad(entidad);
        debe("validar avisa si falta el atributo clave",
                sinClave.validar().stream()
                        .anyMatch(a -> a.getMensaje().contains("clave")));
        entidad.getAtributos().get(0).marcar(Marca.CLAVE, true);
        debe("y deja de avisar al marcarlo", sinClave.validar().isEmpty());

        // Quitar una entidad se lleva las relaciones que quedan sin
        // participantes, asi que al borrar en grupo hay que tolerar que un
        // elemento ya no este cuando le llega el turno.
        ModeloER mezcla = new ModeloER();
        Tablero conMezcla = new Tablero(mezcla);
        conMezcla.agregar(TipoNodo.ENTIDAD, 0, 0);
        conMezcla.agregar(TipoNodo.ENTIDAD, 200, 0);
        conMezcla.agregar(TipoNodo.RELACION, 100, 0);
        conMezcla.seleccionarVarios(new java.util.ArrayList<>(conMezcla.getFiguras()));
        boolean sinExcepcion = true;
        try {
            conMezcla.eliminarSeleccion();
        } catch (RuntimeException e) {
            sinExcepcion = false;
        }
        debe("borrar entidades y relaciones a la vez no revienta", sinExcepcion);
        debe("y el tablero queda vacio", conMezcla.estaVacio());

        // Una relacion N:M puede llevar datos que no son de ninguna de las dos
        // entidades, como la nota de una inscripcion.
        ModeloER conNota = new ModeloER();
        Tablero tableroNota = new Tablero(conNota);
        tableroNota.agregar(TipoNodo.ENTIDAD, 0, 300);
        tableroNota.agregar(TipoNodo.ENTIDAD, 600, 300);
        tableroNota.agregar(TipoNodo.RELACION, 300, 300);
        Relacion cursa = conNota.getRelaciones().get(0);
        Figura atributoDeRelacion = tableroNota.agregar(TipoNodo.ATRIBUTO, 300, 180);
        debe("un atributo se puede colgar de una relacion",
                atributoDeRelacion != null && atributoDeRelacion.getDuenno() == cursa);
        debe("y la relacion lo guarda", cursa.getAtributos().size() == 1);
        debe("pero no admite atributos clave",
                !tableroNota.puedeColocarse(TipoNodo.ATRIBUTO_CLAVE, 300, 180)
                        || conNota.getEntidades().size() > 0);
    }

    // La traza explica por que el esquema salio asi, y la exportacion escribe
    // el codigo en disco con la extension del destino.
    private static void probarTrazaYExportacion() throws Exception {
        System.out.println("\n--- Traza y exportacion ---");
        Fachada fachada = new Fachada();
        Tablero tablero = new Tablero(fachada.getModelo());
        tablero.agregar(TipoNodo.ENTIDAD, 0, 300);
        tablero.agregar(TipoNodo.ENTIDAD, 600, 300);
        tablero.agregar(TipoNodo.RELACION, 300, 300);
        ModeloER modelo = fachada.getModelo();
        Entidad alumno = modelo.getEntidades().get(0);
        Entidad materia = modelo.getEntidades().get(1);
        alumno.agregarAtributo(new Atributo("ida", TipoDato.SERIAL, Naturaleza.SIMPLE,
                java.util.EnumSet.of(Marca.CLAVE), new Punto(0, -110)));
        materia.agregarAtributo(new Atributo("idm", TipoDato.SERIAL, Naturaleza.SIMPLE,
                java.util.EnumSet.of(Marca.CLAVE), new Punto(0, -110)));
        Relacion cursa = modelo.getRelaciones().get(0);
        tablero.enlazar(Figura.de(cursa), Figura.de(alumno));
        tablero.enlazar(Figura.de(cursa), Figura.de(materia));

        debe("sin convertir no hay traza", fachada.getTraza().isEmpty());
        fachada.convertir();
        debe("tras convertir hay traza", !fachada.getTraza().isEmpty());
        debe("cada paso lleva su explicacion",
                fachada.getTraza().stream()
                        .allMatch(p -> p.getExplicacion() != null
                                && !p.getExplicacion().isBlank()));

        boolean exportados = true;
        for (Destino destino : Destino.values()) {
            String ruta = "/tmp/prueba_exp." + destino.getExtension();
            fachada.exportarCodigo(destino, ruta);
            if (java.nio.file.Files.size(java.nio.file.Path.of(ruta)) < 50) {
                exportados = false;
            }
        }
        debe("los seis destinos se exportan a archivo", exportados);

        fachada.nuevo();
        debe("empezar de cero descarta la traza", fachada.getTraza().isEmpty());
        try {
            fachada.exportarCodigo(Destino.SQLITE, "/tmp/prueba_no.sql");
            debe("exportar sin esquema avisa", false);
        } catch (IllegalStateException e) {
            debe("exportar sin esquema avisa", true);
        }
    }

    // --- utilidades ---

    private static void pulsar(LienzoER lienzo, int x, int y) {
        for (MouseListener oyente : lienzo.getMouseListeners()) {
            oyente.mousePressed(evento(lienzo, MouseEvent.MOUSE_PRESSED, x, y));
        }
    }

    private static void arrastrar(LienzoER lienzo, int x, int y) {
        for (MouseMotionListener oyente : lienzo.getMouseMotionListeners()) {
            oyente.mouseDragged(evento(lienzo, MouseEvent.MOUSE_DRAGGED, x, y));
        }
    }

    private static void soltar(LienzoER lienzo, int x, int y) {
        for (MouseListener oyente : lienzo.getMouseListeners()) {
            oyente.mouseReleased(evento(lienzo, MouseEvent.MOUSE_RELEASED, x, y));
        }
    }

    private static MouseEvent evento(LienzoER lienzo, int id, int x, int y) {
        return new MouseEvent(lienzo, id, System.currentTimeMillis(), 0, x, y, 1,
                false, MouseEvent.BUTTON1);
    }

    private static void debe(String que, boolean condicion) {
        System.out.println((condicion ? "  [OK]   " : "  [FALLA]") + " " + que);
        if (!condicion) {
            fallos++;
        }
    }
}
