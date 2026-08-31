import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del algoritmo que convierte un {@code ModeloER} en un
 * {@link EsquemaRelacional}.
 *
 * <p><b>El orden de las pasadas es intencional y no debe alterarse:</b></p>
 * <ol>
 *   <li>{@link #convertirEntidades}: crea una tabla por cada entidad (fuerte
 *       o débil). Las fuertes quedan con su clave primaria definida; las
 *       débiles quedan sin clave todavía.</li>
 *   <li>{@link #propagarClavesDebiles}: ya con todas las tablas de entidades
 *       creadas, copia la clave de cada entidad propietaria hacia su(s)
 *       entidad(es) débil(es) y recién ahí define la clave primaria
 *       (compuesta) de la tabla débil.</li>
 *   <li>{@link #convertirRelaciones}: requiere que TODAS las entidades
 *       (fuertes y débiles) ya tengan tabla y clave primaria definitiva, para
 *       poder crear las claves foráneas de las relaciones 1:1, 1:N, N:M y
 *       N-arias.</li>
 *   <li>{@link #extraerMultivaluados}: requiere que la entidad dueña del
 *       atributo multivaluado ya tenga su clave definitiva, para poder
 *       referenciarla desde la nueva tabla del atributo.</li>
 * </ol>
 *
 * <p>Si se cambia este orden, las entidades débiles se quedan sin clave:
 * su clave primaria depende de la clave, ya definida, de su entidad
 * propietaria, y las relaciones/atributos multivaluados dependen a su vez de
 * que esa clave ya esté completa.</p>
 *
 * <p><b>Nota sobre dependencias externas:</b> {@code ModeloER}, {@code Entidad},
 * {@code Relacion}, {@code Participacion}, {@code Atributo},
 * {@code ElementoDelModelo}, {@code Cardinalidad}, {@code Modalidad} y
 * {@code Aviso} son clases de otros miembros del equipo. Esta clase asume la
 * API pública que se ve en el diagrama, incluyendo un par de getters de
 * {@code Atributo} ({@code getNombre()}, {@code getTipo()}) que no se alcanzan
 * a ver completos en el diagrama pero que son necesarios para poder crear las
 * columnas. Si el nombre real de esos métodos difiere, solo hay que ajustar
 * las llamadas correspondientes aquí.</p>
 */
public class Conversor implements IConversor {

    @Override
    public ResultadoConversion convertir(ModeloER modelo) {
        if (modelo == null) {
            throw new IllegalArgumentException("El modelo ER no puede ser nulo.");
        }

        ResultadoConversion resultado = new ResultadoConversion();

        convertirEntidades(modelo, resultado);
        propagarClavesDebiles(modelo, resultado);
        convertirRelaciones(modelo, resultado);
        extraerMultivaluados(modelo, resultado);

        return resultado;
    }

    // ------------------------------------------------------------------
    // Pasada 1: entidades
    // ------------------------------------------------------------------

    public void convertirEntidades(ModeloER modelo, ResultadoConversion resultado) {
        for (Entidad entidad : modelo.getEntidades()) {
            OrigenTabla origen = entidad.esDebil() ? OrigenTabla.ENTIDAD_DEBIL : OrigenTabla.ENTIDAD_FUERTE;
            Tabla tabla = crearTabla(entidad, origen, resultado);

            if (!entidad.esDebil()) {
                List<String> nombresClave = entidad.clave().stream()
                        .map(Atributo::getNombre)
                        .collect(Collectors.toList());
                if (nombresClave.isEmpty()) {
                    resultado.advertir(new Aviso(Severidad.ADVERTENCIA,
                            "La entidad no tiene atributos marcados como clave.", entidad.getNombre()));
                } else {
                    tabla.definirClave(nombresClave);
                }
                resultado.anotar(TipoRegla.ENTIDAD_FUERTE,
                        "Entidad fuerte '" + entidad.getNombre() + "' convertida en la tabla '"
                                + tabla.getNombre() + "'.");
            } else {
                // Su clave primaria se completa en propagarClavesDebiles(), una vez
                // exista la tabla (y la clave) de su entidad propietaria.
                resultado.anotar(TipoRegla.ENTIDAD_DEBIL,
                        "Entidad débil '" + entidad.getNombre() + "' convertida en la tabla '"
                                + tabla.getNombre() + "' (pendiente de propagar su clave).");
            }
        }
    }

    // ------------------------------------------------------------------
    // Pasada 2: propagar la clave de la propietaria a cada entidad débil
    // ------------------------------------------------------------------

    public void propagarClavesDebiles(ModeloER modelo, ResultadoConversion resultado) {
        for (Entidad entidad : modelo.getEntidades()) {
            if (!entidad.esDebil()) {
                continue;
            }

            Tabla tablaDebil = tablaDe(entidad.getId(), resultado);

            Entidad propietaria = modelo.propietariaDe(entidad.getId());
            if (propietaria == null) {
                resultado.advertir(new Aviso(Severidad.ERROR,
                        "No se encontró la entidad propietaria de esta entidad débil.", entidad.getNombre()));
                continue;
            }

            Tabla tablaPropietaria = tablaDe(propietaria.getId(), resultado);
            Restriccion clavePropietaria = tablaPropietaria.primaria();
            if (clavePropietaria == null) {
                resultado.advertir(new Aviso(Severidad.ERROR,
                        "La entidad propietaria '" + propietaria.getNombre()
                                + "' todavía no tiene clave primaria.", entidad.getNombre()));
                continue;
            }

            List<String> columnasHeredadas = copiarColumnasClave(clavePropietaria, tablaPropietaria, tablaDebil);

            // La relación identificadora se traduce en una FK obligatoria con
            // borrado en cascada: si se borra el "dueño", las filas de la
            // entidad débil dejan de tener sentido por sí solas.
            tablaDebil.restringir(Restriccion.foranea(columnasHeredadas,
                    tablaPropietaria.getNombre(), columnasHeredadas,
                    AccionReferencial.CASCADA, AccionReferencial.CASCADA));

            // La clave primaria de la débil = clave heredada + su clave parcial (discriminante).
            List<String> claveParcial = entidad.clave().stream()
                    .map(Atributo::getNombre)
                    .collect(Collectors.toList());
            List<String> claveCompuesta = new ArrayList<>(columnasHeredadas);
            claveCompuesta.addAll(claveParcial);
            tablaDebil.definirClave(claveCompuesta);

            resultado.anotar(TipoRegla.ENTIDAD_DEBIL,
                    "Se propagó la clave de '" + propietaria.getNombre() + "' hacia la tabla débil '"
                            + tablaDebil.getNombre() + "'.");
        }
    }

    // ------------------------------------------------------------------
    // Pasada 3: relaciones (1:1, 1:N, N:M, N-arias)
    // ------------------------------------------------------------------

    public void convertirRelaciones(ModeloER modelo, ResultadoConversion resultado) {
        for (Relacion relacion : modelo.getRelaciones()) {
            aplicarRegla(modelo, relacion, resultado);
        }
    }

    public void aplicarRegla(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        TipoRegla regla = firma(relacion);
        switch (regla) {
            case UNO_A_UNO -> reglaUnoAUno(modelo, relacion, resultado);
            case UNO_A_MUCHOS -> reglaUnoAMuchos(modelo, relacion, resultado);
            case MUCHOS_A_MUCHOS -> reglaMuchosAMuchos(modelo, relacion, resultado);
            case N_ARIA -> reglaNAria(modelo, relacion, resultado);
            default -> resultado.advertir(new Aviso(Severidad.ADVERTENCIA,
                    "No se reconoce una regla de conversión aplicable a esta relación.", relacion.getNombre()));
        }
    }

    /**
     * Determina qué regla de conversión corresponde a la relación, según su
     * grado y las cardinalidades de sus participaciones.
     */
    public TipoRegla firma(Relacion relacion) {
        if (relacion.grado() != 2) {
            return TipoRegla.N_ARIA;
        }
        int ladosMuchos = relacion.contar(Cardinalidad.MUCHOS);
        if (ladosMuchos == 0) {
            return TipoRegla.UNO_A_UNO;
        }
        if (ladosMuchos == 1) {
            return TipoRegla.UNO_A_MUCHOS;
        }
        return TipoRegla.MUCHOS_A_MUCHOS;
    }

    public Participacion ladoUno(Relacion relacion) {
        return relacion.getParticipaciones().stream()
                .filter(p -> p.getCardinalidad() == Cardinalidad.UNO)
                .findFirst()
                .orElse(null);
    }

    public Participacion ladoMuchos(Relacion relacion) {
        return relacion.getParticipaciones().stream()
                .filter(p -> p.getCardinalidad() == Cardinalidad.MUCHOS)
                .findFirst()
                .orElse(null);
    }

    /**
     * Para relaciones 1:1, decide en qué lado se coloca la clave foránea: se
     * prefiere el lado obligatorio (así se evitan columnas nulas); si ninguno
     * o ambos lo son, se toma el primero de la lista.
     */
    public Participacion ladoDestino(Relacion relacion) {
        List<Participacion> participaciones = relacion.getParticipaciones();
        return participaciones.stream()
                .filter(Participacion::esObligatoria)
                .findFirst()
                .orElse(participaciones.get(0));
    }

    public void reglaUnoAUno(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        Participacion destino = ladoDestino(relacion);
        Participacion origen = relacion.getParticipaciones().stream()
                .filter(p -> p != destino)
                .findFirst()
                .orElse(null);
        if (origen == null) {
            resultado.advertir(new Aviso(Severidad.ERROR,
                    "La relación 1:1 no tiene dos participaciones válidas.", relacion.getNombre()));
            return;
        }

        Tabla tablaDestino = tablaDe(destino.getEntidad(), resultado);
        Tabla tablaOrigen = tablaDe(origen.getEntidad(), resultado);

        agregarAtributosDeRelacion(relacion, tablaDestino);

        Restriccion claveOrigen = tablaOrigen.primaria();
        if (claveOrigen == null) {
            resultado.advertir(new Aviso(Severidad.ERROR,
                    "La tabla '" + tablaOrigen.getNombre() + "' no tiene clave primaria.", relacion.getNombre()));
            return;
        }

        List<String> columnasFk = copiarColumnasClave(claveOrigen, tablaOrigen, tablaDestino);
        AccionReferencial alBorrar = destino.esObligatoria() ? AccionReferencial.CASCADA : AccionReferencial.ANULAR;
        tablaDestino.restringir(Restriccion.foranea(columnasFk,
                tablaOrigen.getNombre(), columnasFk, AccionReferencial.CASCADA, alBorrar));

        resultado.anotar(TipoRegla.UNO_A_UNO,
                "Relación 1:1 '" + relacion.getNombre() + "' resuelta con FK en '" + tablaDestino.getNombre() + "'.");
    }

    public void reglaUnoAMuchos(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        Participacion ladoUno = ladoUno(relacion);
        Participacion ladoMuchos = ladoMuchos(relacion);
        if (ladoUno == null || ladoMuchos == null) {
            resultado.advertir(new Aviso(Severidad.ERROR,
                    "La relación 1:N no tiene un lado '1' y un lado 'N' identificables.", relacion.getNombre()));
            return;
        }

        Tabla tablaMuchos = tablaDe(ladoMuchos.getEntidad(), resultado);
        Tabla tablaUno = tablaDe(ladoUno.getEntidad(), resultado);

        agregarAtributosDeRelacion(relacion, tablaMuchos);

        Restriccion claveUno = tablaUno.primaria();
        if (claveUno == null) {
            resultado.advertir(new Aviso(Severidad.ERROR,
                    "La tabla '" + tablaUno.getNombre() + "' no tiene clave primaria.", relacion.getNombre()));
            return;
        }

        List<String> columnasFk = copiarColumnasClave(claveUno, tablaUno, tablaMuchos);
        AccionReferencial alBorrar = ladoMuchos.esObligatoria() ? AccionReferencial.CASCADA : AccionReferencial.ANULAR;
        tablaMuchos.restringir(Restriccion.foranea(columnasFk,
                tablaUno.getNombre(), columnasFk, AccionReferencial.CASCADA, alBorrar));

        resultado.anotar(TipoRegla.UNO_A_MUCHOS,
                "Relación 1:N '" + relacion.getNombre() + "' resuelta con FK en '" + tablaMuchos.getNombre() + "'.");
    }

    public void reglaMuchosAMuchos(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        Tabla tablaRelacion = crearTabla(relacion, OrigenTabla.RELACION_NM, resultado);
        List<String> claveCompuesta = enlazarParticipaciones(relacion, tablaRelacion, resultado);
        if (!claveCompuesta.isEmpty()) {
            tablaRelacion.definirClave(claveCompuesta);
        }
        resultado.anotar(TipoRegla.MUCHOS_A_MUCHOS,
                "Relación N:M '" + relacion.getNombre() + "' resuelta en la tabla '" + tablaRelacion.getNombre() + "'.");
    }

    public void reglaNAria(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        Tabla tablaRelacion = crearTabla(relacion, OrigenTabla.RELACION_NARIA, resultado);
        List<String> claveCompuesta = enlazarParticipaciones(relacion, tablaRelacion, resultado);
        if (!claveCompuesta.isEmpty()) {
            tablaRelacion.definirClave(claveCompuesta);
        }
        resultado.anotar(TipoRegla.N_ARIA,
                "Relación N-aria '" + relacion.getNombre() + "' resuelta en la tabla '" + tablaRelacion.getNombre() + "'.");
    }

    // ------------------------------------------------------------------
    // Pasada 4: atributos multivaluados -> tabla propia
    // ------------------------------------------------------------------

    public void extraerMultivaluados(ModeloER modelo, ResultadoConversion resultado) {
        for (Entidad entidad : modelo.getEntidades()) {
            Tabla tablaEntidad = tablaDe(entidad.getId(), resultado);
            Restriccion claveEntidad = tablaEntidad.primaria();
            if (claveEntidad == null) {
                continue; // ya se avisó del problema en una pasada anterior
            }

            for (Atributo atributo : entidad.atributosMultivaluados()) {
                String nombreTabla = tablaEntidad.getNombre() + "_" + atributo.getNombre().toLowerCase();
                Tabla tablaAtributo = new Tabla(nombreTabla, OrigenTabla.ATRIBUTO_MULTIVALUADO, atributo.getId());
                resultado.getEsquema().agregarTabla(tablaAtributo);

                List<String> columnasFk = copiarColumnasClave(claveEntidad, tablaEntidad, tablaAtributo);
                tablaAtributo.restringir(Restriccion.foranea(columnasFk,
                        tablaEntidad.getNombre(), columnasFk,
                        AccionReferencial.CASCADA, AccionReferencial.CASCADA));

                tablaAtributo.agregarColumna(new Columna(atributo.getNombre(), atributo.getTipo(), false));

                List<String> claveCompuesta = new ArrayList<>(columnasFk);
                claveCompuesta.add(atributo.getNombre());
                tablaAtributo.definirClave(claveCompuesta);

                resultado.anotar(TipoRegla.ATRIBUTO_MULTIVALUADO,
                        "Atributo multivaluado '" + atributo.getNombre() + "' de '" + entidad.getNombre()
                                + "' extraído a la tabla '" + tablaAtributo.getNombre() + "'.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades comunes (también parte de la API pública según el diagrama)
    // ------------------------------------------------------------------

    public Tabla crearTabla(ElementoDelModelo elemento, OrigenTabla origen, ResultadoConversion resultado) {
        Tabla tabla = new Tabla(elemento.nombreNormalizado(), origen, elemento.getId());
        for (Atributo atributo : elemento.atributosAlmacenables()) {
            tabla.agregarColumna(new Columna(atributo.getNombre(), atributo.getTipo(), !atributo.esObligatorio()));
        }
        resultado.getEsquema().agregarTabla(tabla);
        return tabla;
    }

    public Tabla tablaDe(String idElemento, ResultadoConversion resultado) {
        Tabla tabla = resultado.getEsquema().deOrigen(idElemento);
        if (tabla == null) {
            throw new IllegalStateException(
                    "No existe todavía una tabla para el elemento del modelo con id '" + idElemento
                            + "'. ¿Se respetó el orden de las pasadas de conversión?");
        }
        return tabla;
    }

    // ------------------------------------------------------------------
    // Helpers privados (no forman parte del diagrama, solo evitan repetir código)
    // ------------------------------------------------------------------

    /**
     * Enlaza la tabla de una relación (N:M o N-aria) con cada una de sus
     * entidades participantes: copia las columnas de la clave de cada
     * entidad, crea la restricción foránea correspondiente, y devuelve la
     * unión de todas esas columnas para usarlas como clave primaria
     * compuesta de la tabla de la relación.
     */
    private List<String> enlazarParticipaciones(Relacion relacion, Tabla tablaRelacion, ResultadoConversion resultado) {
        List<String> claveCompuesta = new ArrayList<>();
        for (Participacion participacion : relacion.getParticipaciones()) {
            Tabla tablaEntidad = tablaDe(participacion.getEntidad(), resultado);
            Restriccion claveEntidad = tablaEntidad.primaria();
            if (claveEntidad == null) {
                resultado.advertir(new Aviso(Severidad.ERROR,
                        "La tabla '" + tablaEntidad.getNombre() + "' no tiene clave primaria.", relacion.getNombre()));
                continue;
            }
            List<String> columnasFk = copiarColumnasClave(claveEntidad, tablaEntidad, tablaRelacion);
            tablaRelacion.restringir(Restriccion.foranea(columnasFk,
                    tablaEntidad.getNombre(), columnasFk,
                    AccionReferencial.CASCADA, AccionReferencial.CASCADA));
            claveCompuesta.addAll(columnasFk);
        }
        return claveCompuesta;
    }

    /**
     * Copia (si aún no existen) las columnas de {@code clave} desde
     * {@code tablaOrigen} hacia {@code tablaDestino}, y devuelve los nombres
     * resultantes en el mismo orden.
     */
    private List<String> copiarColumnasClave(Restriccion clave, Tabla tablaOrigen, Tabla tablaDestino) {
        List<String> nombres = new ArrayList<>();
        for (String nombreColumna : clave.getColumnas()) {
            Columna columnaOriginal = tablaOrigen.buscarColumna(nombreColumna);
            if (tablaDestino.buscarColumna(nombreColumna) == null) {
                tablaDestino.agregarColumna(new Columna(columnaOriginal.getNombre(), columnaOriginal.getTipo(), false));
            }
            nombres.add(nombreColumna);
        }
        return nombres;
    }

    private void agregarAtributosDeRelacion(Relacion relacion, Tabla tabla) {
        for (Atributo atributo : relacion.atributosAlmacenables()) {
            if (tabla.buscarColumna(atributo.getNombre()) == null) {
                tabla.agregarColumna(new Columna(atributo.getNombre(), atributo.getTipo(), !atributo.esObligatorio()));
            }
        }
    }
}
