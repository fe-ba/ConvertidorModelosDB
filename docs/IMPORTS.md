# Imports añadidos a cada archivo

Cada archivo lleva primero su `package` y después estos `import`.
Los de `java.*` y `javax.*` que ya tenía se conservan.


## `modelador.aplicacion`

**Fachada.java**
```java
package modelador.aplicacion;

import modelador.conversion.Conversor;
import modelador.conversion.ResultadoConversion;
import modelador.dominio.er.ModeloER;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.tipos.Aviso;
import modelador.generacion.CatalogoGeneradores;
import modelador.generacion.Destino;
import modelador.persistencia.IRepositorio;
import modelador.persistencia.RepositorioJson;
```


## `modelador.conversion`

**Conversor.java**
```java
package modelador.conversion;

import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.ElementoDelModelo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Participacion;
import modelador.dominio.er.Relacion;
import modelador.dominio.relacional.AccionReferencial;
import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.Severidad;
```

**IConversor.java**
```java
package modelador.conversion;

import modelador.dominio.er.ModeloER;
```

**ResultadoConversion.java**
```java
package modelador.conversion;

import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.tipos.Aviso;
```

**TipoRegla.java**
```java
package modelador.conversion;
```
*(no necesita imports del proyecto)*

**Traza.java**
```java
package modelador.conversion;
```
*(no necesita imports del proyecto)*


## `modelador.dominio.er`

**Atributo.java**
```java
package modelador.dominio.er;

import modelador.dominio.tipos.TipoDato;
```

**Cardinalidad.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**ElementoDelModelo.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Entidad.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Marca.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Modalidad.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**ModeloER.java**
```java
package modelador.dominio.er;

import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.Severidad;
```

**Naturaleza.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Participacion.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Punto.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*

**Relacion.java**
```java
package modelador.dominio.er;
```
*(no necesita imports del proyecto)*


## `modelador.dominio.relacional`

**AccionReferencial.java**
```java
package modelador.dominio.relacional;
```
*(no necesita imports del proyecto)*

**Columna.java**
```java
package modelador.dominio.relacional;

import modelador.dominio.tipos.TipoDato;
```

**EsquemaRelacional.java**
```java
package modelador.dominio.relacional;

import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.Severidad;
```

**OrigenTabla.java**
```java
package modelador.dominio.relacional;
```
*(no necesita imports del proyecto)*

**Restriccion.java**
```java
package modelador.dominio.relacional;
```
*(no necesita imports del proyecto)*

**Tabla.java**
```java
package modelador.dominio.relacional;
```
*(no necesita imports del proyecto)*

**TipoRestriccion.java**
```java
package modelador.dominio.relacional;
```
*(no necesita imports del proyecto)*


## `modelador.dominio.tipos`

**Aviso.java**
```java
package modelador.dominio.tipos;
```
*(no necesita imports del proyecto)*

**Severidad.java**
```java
package modelador.dominio.tipos;
```
*(no necesita imports del proyecto)*

**TipoDato.java**
```java
package modelador.dominio.tipos;
```
*(no necesita imports del proyecto)*


## `modelador.generacion`

**CatalogoGeneradores.java**
```java
package modelador.generacion;
```
*(no necesita imports del proyecto)*

**Destino.java**
```java
package modelador.generacion;
```
*(no necesita imports del proyecto)*

**GeneradorObjetos.java**
```java
package modelador.generacion;

import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
import modelador.dominio.relacional.TipoRestriccion;
import modelador.dominio.tipos.TipoDato;
```

**GeneradorSQL.java**
```java
package modelador.generacion;

import modelador.dominio.relacional.AccionReferencial;
import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
import modelador.dominio.relacional.TipoRestriccion;
import modelador.dominio.tipos.Aviso;
import modelador.dominio.tipos.TipoDato;
```

**IGeneradorDeCodigo.java**
```java
package modelador.generacion;

import modelador.dominio.relacional.EsquemaRelacional;
```


## `modelador.interfaz`

**EnlaceVista.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Entidad;
import modelador.dominio.er.Participacion;
import modelador.dominio.er.Relacion;
```

**Figura.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Atributo;
import modelador.dominio.er.ElementoDelModelo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Punto;
import modelador.dominio.er.Relacion;
```

**FiguraTabla.java**
```java
package modelador.interfaz;

import modelador.dominio.relacional.Tabla;
```

**FormaSimbolo.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**IconoSimbolo.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**LienzoER.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**LienzoRelacional.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Atributo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Relacion;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.Tabla;
```

**Main.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**Paleta.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**PanelPropiedades.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.ElementoDelModelo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Marca;
import modelador.dominio.er.Modalidad;
import modelador.dominio.er.Naturaleza;
import modelador.dominio.er.Relacion;
import modelador.dominio.tipos.TipoDato;
```

**PintorER.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.Modalidad;
```

**PintorRelacional.java**
```java
package modelador.interfaz;

import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
```

**Tablero.java**
```java
package modelador.interfaz;

import modelador.dominio.er.Atributo;
import modelador.dominio.er.Cardinalidad;
import modelador.dominio.er.ElementoDelModelo;
import modelador.dominio.er.Entidad;
import modelador.dominio.er.Marca;
import modelador.dominio.er.Modalidad;
import modelador.dominio.er.ModeloER;
import modelador.dominio.er.Naturaleza;
import modelador.dominio.er.Participacion;
import modelador.dominio.er.Punto;
import modelador.dominio.er.Relacion;
import modelador.dominio.tipos.TipoDato;
```

**Tema.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**TipoNodo.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*

**VentanaPrincipal.java**
```java
package modelador.interfaz;

import modelador.aplicacion.Fachada;
import modelador.dominio.tipos.Aviso;
import modelador.generacion.Destino;
```

**VistaCodigo.java**
```java
package modelador.interfaz;
```
*(no necesita imports del proyecto)*


## `modelador.persistencia`

**IRepositorio.java**
```java
package modelador.persistencia;

import modelador.dominio.er.ModeloER;
```

**Json.java**
```java
package modelador.persistencia;
```
*(no necesita imports del proyecto)*

**RepositorioJson.java**
```java
package modelador.persistencia;

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
```


## `modelador.pruebas`

**Demo.java**
```java
package modelador.pruebas;

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
```

**PruebaConversor.java**
```java
package modelador.pruebas;

import modelador.conversion.Conversor;
import modelador.conversion.ResultadoConversion;
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
import modelador.dominio.relacional.AccionReferencial;
import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
import modelador.dominio.tipos.TipoDato;
import modelador.generacion.Destino;
import modelador.generacion.GeneradorSQL;
```

**PruebaGeneracion.java**
```java
package modelador.pruebas;

import modelador.dominio.relacional.AccionReferencial;
import modelador.dominio.relacional.Columna;
import modelador.dominio.relacional.EsquemaRelacional;
import modelador.dominio.relacional.OrigenTabla;
import modelador.dominio.relacional.Restriccion;
import modelador.dominio.relacional.Tabla;
import modelador.dominio.relacional.TipoRestriccion;
import modelador.dominio.tipos.TipoDato;
import modelador.generacion.CatalogoGeneradores;
import modelador.generacion.Destino;
import modelador.generacion.GeneradorObjetos;
import modelador.generacion.GeneradorSQL;
import modelador.generacion.IGeneradorDeCodigo;
```

**PruebaInterfaz.java**
```java
package modelador.pruebas;

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
```
