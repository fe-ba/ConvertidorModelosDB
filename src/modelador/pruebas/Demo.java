package modelador.pruebas;

import java.util.EnumSet;
import java.util.List;
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
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.TipoDato;

/**
 * Programa de prueba para verificar el modelo Entidad-Relación, sus
 * validaciones, el borrado en cascada y los tipos de dato.
 */
public class Demo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("PRUEBAS DEL MODELO ENTIDAD-RELACION (rama tipos)");
        System.out.println("==================================================");

        probarTiposDeDato();
        probarModeloCompleto();
        probarValidacion();
        probarBorradoEnCascada();
    }

    private static void probarTiposDeDato() {
        System.out.println("\n--- Tipos de dato ---");
        System.out.println("SERIAL.esAutonumerico() = " + TipoDato.SERIAL.esAutonumerico()
                + " (esperado: true)");
        System.out.println("ENTERO.esAutonumerico() = " + TipoDato.ENTERO.esAutonumerico()
                + " (esperado: false)");
        System.out.println("SERIAL.getBase() = " + TipoDato.SERIAL.getBase()
                + " (esperado: ENTERO)");
        System.out.println("FECHA.getBase() = " + TipoDato.FECHA.getBase()
                + " (esperado: FECHA)");
        verificar(TipoDato.SERIAL.esAutonumerico(), "SERIAL es autonumerico");
        verificar(!TipoDato.ENTERO.esAutonumerico(), "ENTERO no es autonumerico");
        verificar(TipoDato.SERIAL.getBase() == TipoDato.ENTERO, "base de SERIAL es ENTERO");
    }

    private static void probarModeloCompleto() {
        System.out.println("\n--- Crear modelo ER completo ---");
        ModeloER modelo = new ModeloER();

        Entidad usuario = new Entidad("Usuario", new Punto(100, 100), false);
        usuario.agregarAtributo(new Atributo("id", TipoDato.ENTERO, Naturaleza.SIMPLE,
                EnumSet.of(Marca.CLAVE, Marca.OBLIGATORIO), new Punto(60, 120)));
        usuario.agregarAtributo(new Atributo("nombre", TipoDato.TEXTO_MEDIO, Naturaleza.SIMPLE,
                EnumSet.of(Marca.OBLIGATORIO), new Punto(60, 140)));

        Entidad pedido = new Entidad("Pedido", new Punto(300, 100), false);
        pedido.agregarAtributo(new Atributo("numero", TipoDato.ENTERO_GRANDE, Naturaleza.SIMPLE,
                EnumSet.of(Marca.CLAVE), new Punto(250, 120)));

        String idUsuario = modelo.agregarEntidad(usuario);
        String idPedido = modelo.agregarEntidad(pedido);

        Relacion realiza = new Relacion("Realiza", new Punto(200, 80), false);
        realiza.agregarParticipacion(new Participacion(idUsuario, Cardinalidad.UNO, Modalidad.TOTAL, "cliente"));
        realiza.agregarParticipacion(new Participacion(idPedido, Cardinalidad.MUCHOS, Modalidad.PARCIAL, "pedido"));
        modelo.agregarRelacion(realiza);

        System.out.println("Entidades: " + modelo.getEntidades().size() + " (esperado: 2)");
        System.out.println("Relaciones: " + modelo.getRelaciones().size() + " (esperado: 1)");
        System.out.println("Grado de 'Realiza': " + realiza.grado() + " (esperado: 2)");
        System.out.println("Recursiva: " + realiza.esRecursiva() + " (esperado: false)");
        System.out.println("Relaciones de 'Usuario': " + modelo.relacionesDe(idUsuario).size()
                + " (esperado: 1)");

        verificar(modelo.getEntidades().size() == 2, "dos entidades creadas");
        verificar(modelo.getRelaciones().size() == 1, "una relacion creada");
        verificar(realiza.grado() == 2, "grado de la relacion es 2");
        verificar(!realiza.esRecursiva(), "relacion no recursiva");
        verificar(modelo.relacionesDe(idUsuario).size() == 1, "usuario participa en 1 relacion");
        verificar(realiza.getParticipaciones().stream()
                .anyMatch(p -> p.getEntidad().equals(idPedido) && p.esLadoMuchos()),
                "lado pedido es lado muchos");
    }

    private static void probarValidacion() {
        System.out.println("\n--- Validacion de un modelo correcto ---");
        ModeloER modelo = modeloCorrecto();
        List<Aviso> avisos = modelo.validar();
        System.out.println("Avisos en modelo correcto: " + avisos.size() + " (esperado: 0)");
        for (Aviso a : avisos) {
            System.out.println("  [" + a.getSeveridad() + "] " + a.getMensaje());
        }
        verificar(avisos.isEmpty(), "modelo correcto sin avisos");

        System.out.println("\n--- Validacion de un modelo con errores ---");
        ModeloER malo = new ModeloER();

        // Entidad sin atributos
        Entidad vacia = new Entidad("Vacia", new Punto(10, 10), false);
        malo.agregarEntidad(vacia);
        String idVacia = vacia.getId();

        // Entidad debil sin relacion identificadora
        Entidad debil = new Entidad("Debil", new Punto(30, 30), true);
        debil.agregarAtributo(new Atributo("cod", TipoDato.ENTERO, Naturaleza.SIMPLE,
                EnumSet.of(Marca.CLAVE), new Punto(20, 40)));
        malo.agregarEntidad(debil);

        // Relacion con participacion a entidad inexistente
        Relacion rota = new Relacion("Rota", new Punto(50, 50), false);
        rota.agregarParticipacion(new Participacion(idVacia, Cardinalidad.UNO, Modalidad.TOTAL, "a"));
        rota.agregarParticipacion(new Participacion("id-inexistente", Cardinalidad.MUCHOS, Modalidad.PARCIAL, "b"));
        malo.agregarRelacion(rota);

        List<Aviso> malos = malo.validar();
        System.out.println("Avisos en modelo con errores: " + malos.size());
        for (Aviso a : malos) {
            System.out.println("  [" + a.getSeveridad() + "] " + a.getMensaje()
                    + "  -> " + a.getElemento());
        }
        int errores = (int) malos.stream().filter(Aviso::esError).count();
        System.out.println("Errores: " + errores);
        verificar(errores >= 3, "se detectan entidad sin atributos, debil sin identificar, participacion inexistente");
    }

    private static void probarBorradoEnCascada() {
        System.out.println("\n--- Borrado en cascada de una entidad ---");
        ModeloER modelo = modeloCorrecto();
        Entidad usuario = modelo.buscarEntidad("Usuario");
        String idUsuario = usuario.getId();

        int relacionesAntes = modelo.getRelaciones().size();
        System.out.println("Relaciones antes de borrar: " + relacionesAntes + " (esperado: 1)");

        modelo.quitarEntidad(idUsuario);

        System.out.println("Entidades tras borrar: " + modelo.getEntidades().size() + " (esperado: 1)");
        System.out.println("Relaciones tras borrar: " + modelo.getRelaciones().size()
                + " (esperado: 0, la unica quedo con 1 extremo y se elimino)");
        System.out.println("quedan participaciones referidas a Usuario? "
                + modelo.getRelaciones().stream()
                        .anyMatch(r -> r.participa(idUsuario)));

        verificar(modelo.getEntidades().size() == 1, "entidad borrada");
        verificar(modelo.getRelaciones().isEmpty(), "relacion vacia eliminada en cascada");
        verificar(modelo.getRelaciones().stream().noneMatch(r -> r.participa(idUsuario)),
                "sin participaciones huerfanas");
    }

    private static ModeloER modeloCorrecto() {
        ModeloER modelo = new ModeloER();

        Entidad usuario = new Entidad("Usuario", new Punto(100, 100), false);
        usuario.agregarAtributo(new Atributo("id", TipoDato.ENTERO, Naturaleza.SIMPLE,
                EnumSet.of(Marca.CLAVE, Marca.OBLIGATORIO), new Punto(60, 120)));
        usuario.agregarAtributo(new Atributo("nombre", TipoDato.TEXTO_MEDIO, Naturaleza.SIMPLE,
                EnumSet.of(Marca.OBLIGATORIO), new Punto(60, 140)));

        Entidad pedido = new Entidad("Pedido", new Punto(300, 100), false);
        pedido.agregarAtributo(new Atributo("numero", TipoDato.ENTERO_GRANDE, Naturaleza.SIMPLE,
                EnumSet.of(Marca.CLAVE), new Punto(250, 120)));

        String idUsuario = modelo.agregarEntidad(usuario);
        String idPedido = modelo.agregarEntidad(pedido);

        Relacion realiza = new Relacion("Realiza", new Punto(200, 80), false);
        realiza.agregarParticipacion(new Participacion(idUsuario, Cardinalidad.UNO, Modalidad.TOTAL, "cliente"));
        realiza.agregarParticipacion(new Participacion(idPedido, Cardinalidad.MUCHOS, Modalidad.PARCIAL, "pedido"));
        modelo.agregarRelacion(realiza);

        return modelo;
    }

    private static void verificar(boolean condicion, String descripcion) {
        System.out.println("  ["
                + (condicion ? "OK" : "FALLO")
                + "] " + descripcion);
    }
}
