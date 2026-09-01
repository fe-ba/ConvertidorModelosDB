import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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

            ClaveCopiada heredada = copiarColumnasClave(clavePropietaria, tablaPropietaria,
                    tablaDebil, true);

            // La relación identificadora se traduce en una FK obligatoria con
            // borrado en cascada: si se borra el "dueño", las filas de la
            // entidad débil dejan de tener sentido por sí solas.
            tablaDebil.restringir(Restriccion.foranea(heredada.enDestino(),
                    tablaPropietaria.getNombre(), heredada.enOrigen(),
                    AccionReferencial.CASCADA, AccionReferencial.CASCADA));

            // La clave primaria de la débil = clave heredada + su clave parcial (discriminante).
            List<String> claveParcial = entidad.clave().stream()
                    .map(Atributo::getNombre)
                    .collect(Collectors.toList());
            List<String> claveCompuesta = new ArrayList<>(heredada.enDestino());
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
            // Las identificadoras ya se resolvieron al propagar la clave a las
            // entidades débiles; volver a tratarlas duplicaba la clave heredada
            // y creaba una segunda foránea con el mismo nombre.
            if (yaResueltaComoIdentificadora(modelo, relacion)) {
                continue;
            }
            aplicarRegla(modelo, relacion, resultado);
        }
    }

    /** Cierto si la relación es identificadora y toca alguna entidad débil. */
    private boolean yaResueltaComoIdentificadora(ModeloER modelo, Relacion relacion) {
        if (!relacion.esIdentificadora()) {
            return false;
        }
        for (Participacion parte : relacion.getParticipaciones()) {
            Entidad entidad = buscarPorId(modelo, parte.getEntidad());
            if (entidad != null && entidad.esDebil()) {
                return true;
            }
        }
        return false;
    }

    private Entidad buscarPorId(ModeloER modelo, String id) {
        for (Entidad entidad : modelo.getEntidades()) {
            if (entidad.getId().equals(id)) {
                return entidad;
            }
        }
        return null;
    }

    public void aplicarRegla(ModeloER modelo, Relacion relacion, ResultadoConversion resultado) {
        // Una relacion suelta o a medio enlazar no describe nada: convertirla
        // producia una tabla con una sola foranea y sin sentido.
        if (relacion.grado() < 2) {
            resultado.advertir(new Aviso(Severidad.ERROR,
                    "La relación necesita al menos dos participaciones para convertirse.",
                    relacion.getNombre()));
            return;
        }
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

        AccionReferencial alBorrar = destino.esObligatoria()
                ? AccionReferencial.CASCADA : AccionReferencial.ANULAR;
        ClaveCopiada copia = copiarColumnasClave(claveOrigen, tablaOrigen, tablaDestino,
                destino.esObligatoria());
        tablaDestino.restringir(Restriccion.foranea(copia.enDestino(),
                tablaOrigen.getNombre(), copia.enOrigen(),
                AccionReferencial.CASCADA, alBorrar));

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

        AccionReferencial alBorrar = ladoMuchos.esObligatoria()
                ? AccionReferencial.CASCADA : AccionReferencial.ANULAR;
        ClaveCopiada copia = copiarColumnasClave(claveUno, tablaUno, tablaMuchos,
                ladoMuchos.esObligatoria());
        tablaMuchos.restringir(Restriccion.foranea(copia.enDestino(),
                tablaUno.getNombre(), copia.enOrigen(),
                AccionReferencial.CASCADA, alBorrar));

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

                ClaveCopiada copia = copiarColumnasClave(claveEntidad, tablaEntidad,
                        tablaAtributo, true);
                tablaAtributo.restringir(Restriccion.foranea(copia.enDestino(),
                        tablaEntidad.getNombre(), copia.enOrigen(),
                        AccionReferencial.CASCADA, AccionReferencial.CASCADA));

                tablaAtributo.agregarColumna(new Columna(atributo.getNombre(), atributo.getTipo(), false));

                List<String> claveCompuesta = new ArrayList<>(copia.enDestino());
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
            ClaveCopiada copia = copiarColumnasClave(claveEntidad, tablaEntidad,
                    tablaRelacion, true);
            tablaRelacion.restringir(Restriccion.foranea(copia.enDestino(),
                    tablaEntidad.getNombre(), copia.enOrigen(),
                    AccionReferencial.CASCADA, AccionReferencial.CASCADA));
            claveCompuesta.addAll(copia.enDestino());
        }
        return claveCompuesta;
    }

    /**
     * Copia (si aún no existen) las columnas de {@code clave} desde
     * {@code tablaOrigen} hacia {@code tablaDestino}, y devuelve los nombres
     * resultantes en el mismo orden.
     *
     * El nombre que se utiliza en el destino se calcula antes de crear la
     * columna por {@link #nombreForaneo}: si el nombre ya está ocupado en el
     * destino (por ejemplo, cuando dos entidades participantes tienen una
     * clave con el mismo nombre), se desambigua añadiendo el nombre de la
     * tabla de origen. Así la restricción foránea queda siempre sincronizada
     * con la columna realmente creada.
     */
    private ClaveCopiada copiarColumnasClave(Restriccion clave, Tabla tablaOrigen,
                                             Tabla tablaDestino, boolean obligatoria) {
        List<String> enDestino = new ArrayList<>();
        List<String> enOrigen = new ArrayList<>();
        for (String nombreColumna : clave.getColumnas()) {
            Columna columnaOriginal = tablaOrigen.buscarColumna(nombreColumna);
            String nombreEnDestino = nombreForaneo(tablaOrigen, tablaDestino, nombreColumna);
            if (tablaDestino.buscarColumna(nombreEnDestino) == null) {
                // El tipo base y no el original: una FK hacia un SERIAL es un
                // entero corriente, no otra secuencia.
                // Admite nulos si la participacion es parcial: en una relacion
                // recursiva, el primero de la cadena no tiene con quien
                // relacionarse y no podria insertarse nunca.
                tablaDestino.agregarColumna(new Columna(nombreEnDestino,
                        columnaOriginal.getTipo().getBase(), !obligatoria));
            }
            enDestino.add(nombreEnDestino);
            enOrigen.add(nombreColumna);
        }
        return new ClaveCopiada(enDestino, enOrigen);
    }

    /**
     * Nombres de las columnas de una clave copiada de una tabla a otra.
     *
     * Hacen falta las dos listas: la copia puede haberse renombrado en el
     * destino para no chocar, y la clave foránea tiene que referenciar el
     * nombre que la columna tiene en el origen, no el nuevo.
     */
    private static final class ClaveCopiada {

        private final List<String> enDestino;
        private final List<String> enOrigen;

        private ClaveCopiada(List<String> enDestino, List<String> enOrigen) {
            this.enDestino = enDestino;
            this.enOrigen = enOrigen;
        }

        List<String> enDestino() {
            return enDestino;
        }

        List<String> enOrigen() {
            return enOrigen;
        }
    }

    /**
     * Devuelve un nombre único para la columna {@code nombre} que se va a
     * copiar de {@code tablaOrigen} hacia {@code tablaDestino}. Si el nombre
     * ya está ocupado en el destino, se prefija con el nombre de la tabla de
     * origen; si aun así choca, se añade un sufijo numérico hasta encontrar
     * uno libre.
     */
    private String nombreForaneo(Tabla tablaOrigen, Tabla tablaDestino, String nombre) {
        if (tablaDestino.buscarColumna(nombre) == null) {
            return nombre;
        }
        String basеPrefijado = tablaOrigen.getNombre() + "_" + nombre;
        if (tablaDestino.buscarColumna(basеPrefijado) == null) {
            return basеPrefijado;
        }
        int i = 2;
        String candidato;
        do {
            candidato = basеPrefijado + "_" + i;
            i++;
        } while (tablaDestino.buscarColumna(candidato) != null);
        return candidato;
    }

    private void agregarAtributosDeRelacion(Relacion relacion, Tabla tabla) {
        for (Atributo atributo : relacion.atributosAlmacenables()) {
            if (tabla.buscarColumna(atributo.getNombre()) == null) {
                tabla.agregarColumna(new Columna(atributo.getNombre(), atributo.getTipo(), !atributo.esObligatorio()));
            }
        }
    }
}
