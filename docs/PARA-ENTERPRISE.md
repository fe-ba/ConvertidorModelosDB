# Importar el modelo de clases en Enterprise Architect

## Solo el dominio, sin la presentación

La capa de presentación no forma parte del modelo de dominio, así que el
diagrama de clases final no debería incluirla. Con la nueva estructura basta
con importar las carpetas que interesan.

**Opción A, la más limpia.** Importa cada paquete por separado con
*Code → Reverse Engineer → Import Source Directory*, apuntando solo a:

```
src/modelador/dominio/tipos
src/modelador/dominio/er
src/modelador/dominio/relacional
src/modelador/conversion
src/modelador/generacion
src/modelador/persistencia
src/modelador/aplicacion
```

**Opción B, más rápida.** Importa `src/modelador` entero y después borra del
Project Browser los paquetes `interfaz` y `pruebas`.

En ambos casos marca *Recurse sub-directories* e *Import dependencies*: esto
último es lo que convierte `List<Entidad>` en una asociación con multiplicidad.

## Después de importar

Java no distingue composición de agregación, así que EA importa todo como
asociación simple. Cambia a rombo relleno:

| Todo | Parte | Multiplicidad |
|---|---|---|
| ModeloER | Entidad | 1 .. 0..* |
| ModeloER | Relacion | 1 .. 0..* |
| Entidad | Atributo | 1 .. 0..* |
| Relacion | Atributo | 1 .. 0..* |
| Relacion | Participacion | 1 .. **2..*** |
| EsquemaRelacional | Tabla | 1 .. 0..* |
| Tabla | Columna | 1 .. **1..*** |
| Tabla | Restriccion | 1 .. 0..* |
| ResultadoConversion | Traza | 1 .. 0..* |

Las dos multiplicidades en negrita llevan semántica: una relación necesita al
menos dos participaciones, y una tabla vacía no significa nada.

Oculta los métodos `get` y `set` en el diagrama con Ctrl+Shift+Y, o las cajas
quedarán ilegibles.
