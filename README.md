# Convertidor de Modelos de Base de Datos

Aplicación de escritorio para dibujar un modelo entidad-relación, convertirlo
automáticamente al modelo relacional y generar el código del esquema en seis
formatos.

Escrita en Java con Swing, sin dependencias externas.

---

## Puesta en marcha

Requiere JDK 17 o superior.

```bash
javac -d bin $(find src -name "*.java")
java -cp bin modelador.interfaz.Main
```

En Windows, donde `javac` no expande comodines dentro de subcarpetas:

```bat
dir /s /B src\*.java > fuentes.txt
javac -d bin @fuentes.txt
java -cp bin modelador.interfaz.Main
```

---

## Qué hace

La ventana tiene cuatro pestañas, que recorren el proceso completo.

**Diagrama E-R.** Notación de Chen: rectángulos para entidades, doble borde si
son débiles, rombos para relaciones, doble si son identificadoras, y elipses
para atributos, subrayados si son clave. Las líneas llevan su cardinalidad en
una cajita y se dibujan dobles cuando la participación es total.

Los símbolos se arrastran desde la paleta de la izquierda. Los atributos se
sueltan sobre una entidad o una relación y quedan anclados a ella, siguiéndola
cuando se mueve. Para enlazar, se selecciona una relación y se tira del asa que
aparece a su derecha hasta la entidad que participa.

**Diagrama relacional.** Las tablas se dibujan como cajas con sus columnas,
marcando claves primarias y foráneas, unidas por flechas de referencia. Se
colocan solas cerca de la entidad de la que nacieron.

**Código.** DDL para PostgreSQL, MySQL, SQL Server y SQLite, más modelos de
SQLAlchemy e interfaces de TypeScript, con coloreado de sintaxis y exportación
a archivo.

**Traza.** Qué regla se aplicó a cada elemento y por qué, junto con los avisos
de la conversión. Es la explicación del resultado, no solo el resultado.

---

## Reglas de conversión

Cada entidad pasa a tabla. La entidad débil hereda la clave de su propietaria y
la incorpora a su clave primaria. Una relación 1:1 manda la clave al lado de
participación total; una 1:N, al lado N. Una relación N:M o n-aria genera su
propia tabla, con una clave foránea por participante y la clave primaria
formada por todas ellas. Los atributos multivaluados se extraen a una tabla
aparte y los derivados no se almacenan.

Las relaciones pueden tener atributos propios, que se vuelcan en la tabla que
reciba la clave. Es el caso de la nota de una inscripción, que no pertenece ni
al alumno ni a la materia.

---

## Estructura del repositorio

```
docs/                          Diagramas y modelo de Enterprise Architect
  DiagramasDeSecuencia/        Uno por caso de uso con interacción propia
src/modelador/
  dominio/tipos/               Vocabulario común: TipoDato, Aviso, Severidad
  dominio/er/                  Modelo entidad-relación
  dominio/relacional/          Modelo relacional
  conversion/                  Reglas de transformación E-R → relacional
  generacion/                  Los seis formatos de salida
  persistencia/                Guardar y abrir en JSON
  aplicacion/                  Fachada: punto de entrada de la interfaz
  interfaz/                    Lienzos, paleta, paneles y ventana
  pruebas/                     Las cuatro baterías
```

La organización en paquetes refleja los componentes del diseño, y las
dependencias apuntan siempre hacia el dominio: la presentación habla solo con
la fachada, esta consume `IConversor`, `IGeneradorDeCodigo` e `IRepositorio`, y
los modelos solo conocen el paquete de tipos.

Tres reglas que el diseño sostiene y conviene no romper:

- **El modelo E-R y el modelo relacional no se conocen entre sí.** Todo el
  acoplamiento vive en el conversor.
- **El dominio no contiene una sola palabra de SQL.** La sintaxis de cada motor
  está únicamente en `generacion`.
- **El modelo relacional no es editable.** Es un resultado calculado que se
  regenera entero en cada conversión, así que la única fuente de verdad es el
  diagrama entidad-relación.

---

## Pruebas

```bash
java -cp bin modelador.pruebas.Demo               # modelo E-R
java -cp bin modelador.pruebas.PruebaGeneracion   # 44 comprobaciones
java -cp bin modelador.pruebas.PruebaInterfaz     # 48 comprobaciones
java -cp bin modelador.pruebas.PruebaConversor    # 29 comprobaciones
```

Las baterías no se limitan a comprobar que el código compila. El DDL generado
se ejecuta con el cliente `sqlite3`, se insertan filas y se verifica que las
claves foráneas rechazan las huérfanas. El código de SQLAlchemy se carga y crea
las tablas, y el de TypeScript pasa `tsc --strict`.

Se cubren los casos que suelen esconder errores: relaciones 1:1, 1:N, N:M,
n-arias y recursivas, cadenas de entidades débiles, claves primarias
compuestas, choques de nombres de columna, atributos multivaluados y derivados,
nombres con acentos, modelos vacíos y relaciones a medio enlazar.

---

## Atajos

| Atajo | Acción |
|---|---|
| Ctrl+N / Ctrl+O / Ctrl+S | Nuevo, abrir, guardar como |
| F8 | Validar el modelo |
| F9 | Convertir al modelo relacional |
| Supr o Retroceso | Eliminar lo seleccionado |
| Rueda del ratón | Acercar y alejar |
| Botón central, o espacio y arrastrar | Desplazar el lienzo |
| Inicio | Encuadrar la vista |
| Ctrl o Mayús y clic | Añadir a la selección |
| Ctrl+Q | Salir |

Arrastrando sobre el fondo se dibuja un marco que selecciona todo lo que toque.

---

## Formato del archivo

El modelo se guarda en JSON, referenciando las entidades por nombre en lugar de
por identificador interno. Así el archivo es legible, se puede revisar en un
control de versiones, y guardar y volver a guardar produce exactamente el mismo
texto.

```json
{
  "version": 1,
  "entidades": [
    { "nombre": "Profesor", "esDebil": false,
      "posicion": {"x": 200.0, "y": 300.0},
      "atributos": [
        {"nombre": "id_profesor", "tipo": "SERIAL", "naturaleza": "SIMPLE",
         "marcas": ["CLAVE"], "desplazamiento": {"x": 0.0, "y": -120.0}}
      ] }
  ],
  "relaciones": [
    { "nombre": "Dicta", "esIdentificadora": false,
      "posicion": {"x": 450.0, "y": 300.0}, "atributos": [],
      "participaciones": [
        {"entidad": "Profesor", "cardinalidad": "UNO",
         "modalidad": "PARCIAL", "rol": ""}
      ] }
  ]
}
```

El lector de JSON está escrito a mano para no añadir dependencias al proyecto.

---

## Decisiones de diseño

**Un enumerado en lugar de una jerarquía de generadores.** Entre PostgreSQL y
MySQL solo cambian los nombres de los tipos, los delimitadores de
identificadores y la palabra del autonumérico. Eso son datos, y viven en
diccionarios estáticos; añadir otro motor es una entrada nueva en cada uno.

**La posición de las tablas no se guarda en el modelo relacional.** Se deduce
de `Tabla.procedeDe`, que recuerda de qué elemento del E-R nació cada tabla, de
modo que el diagrama relacional recuerda al original sin ensuciar el modelo con
datos de presentación.

**La conversión continúa aunque haya avisos.** Un modelo a medio dibujar es el
estado normal mientras se trabaja, así que el conversor acumula los problemas y
entrega igualmente el resultado parcial, en vez de negarse a producir nada.

---

## Documentación

En `docs/` están el diagrama de clases, el de componentes, el de casos de uso y
uno de secuencia por cada caso de uso con interacción propia, además del modelo
de Enterprise Architect del que salieron.
