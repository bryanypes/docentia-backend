# API DOCENTIA — Sprint 3, semana 1 (14–20 oct)

Endpoints nuevos o que cambiaron en esta semana. Todo lo que no aparece aquí sigue igual.

## Convenciones

- Base: `http://localhost:8080/api`. En el frontend, Vite reenvía `/api` al backend.
- Autenticación: `Authorization: Bearer <token>` con el token de `POST /api/sesiones`.
- Fechas `yyyy-MM-dd`; fechas con hora `yyyy-MM-ddTHH:mm:ss` (hora de Colombia).
- Los decimales llegan como números JSON (`4.34`), nunca como texto.
- Todo error responde con el mismo cuerpo:

  ```json
  { "mensaje": "Texto para mostrar al usuario", "errores": { "campo": "motivo" } }
  ```

  `errores` viene vacío (`{}`) cuando el problema no es de un campo concreto.

| Código | Cuándo                                                                                |
| ------ | ------------------------------------------------------------------------------------- |
| 400    | Datos inválidos (campos en `errores`) o JSON ilegible                                  |
| 401    | Sin token, token vencido o alterado                                                   |
| 403    | El rol no tiene permiso para la acción                                                |
| 404    | No existe **o está fuera del alcance** del usuario (mismo mensaje en los dos casos)  |
| 409    | Choca con el estado actual: duplicado, etapa cerrada, regla de negocio                |
| 413    | Archivo demasiado grande                                                              |
| 422    | Archivo o mapeo de columnas que no se puede importar                                  |

---

## K1 — Usuarios con rol y alcance

Un usuario tiene **un solo rol**. El vínculo depende del rol:

| Rol           | `idFacultad`   | `idPrograma`   |
| ------------- | -------------- | -------------- |
| `COORDINADOR` | obligatorio    | no se envía    |
| `DOCENTE`     | opcional (*)   | obligatorio    |
| `VICERRECTOR` | no se envía    | no se envía    |

(*) La facultad del docente es la de su programa. Si el formulario la envía, debe coincidir con la
del programa; si no coincide responde 400.

El alcance ya viaja en el login (`POST /api/sesiones` → `usuario.rol`, `usuario.idFacultad`,
`usuario.idPrograma`) y dentro del JWT; el backend filtra con eso sin volver a consultar la base.
**Si se edita el rol o el vínculo de alguien, su sesión abierta conserva el alcance anterior hasta
que el token vence (8 h) o vuelve a iniciar sesión.**

Todos los endpoints de `/usuarios` son solo para `VICERRECTOR` (no existe rol administrador).
Coordinador y docente reciben 403.

### `GET /api/usuarios?rol=`

`rol` opcional (`COORDINADOR`, `VICERRECTOR`, `DOCENTE`). Responde `200` con una lista de
`UsuarioResponse`, ordenada por nombre:

```json
[
  {
    "idUsuario": 4,
    "nombreCompleto": "Julián Camilo Hoyos",
    "correo": "julian.hoyos@uniautonoma.edu.co",
    "rol": "DOCENTE",
    "idFacultad": 1,
    "nombreFacultad": "Facultad de Ingeniería",
    "idPrograma": 1,
    "nombrePrograma": "Ingeniería de Software",
    "esActivo": true
  }
]
```

### `GET /api/usuarios/{idUsuario}`

`200` con un `UsuarioResponse`; `404` si no existe.

### `POST /api/usuarios`

```json
{
  "nombreCompleto": "Ana Ruiz",
  "correo": "ana.ruiz@uniautonoma.edu.co",
  "rol": "COORDINADOR",
  "idFacultad": 1,
  "idPrograma": null,
  "contrasena": "minimo-8-caracteres"
}
```

`201` con el `UsuarioResponse`. El correo se guarda en minúsculas.

| Código | Motivo                                                                                |
| ------ | ------------------------------------------------------------------------------------- |
| 400    | Falta nombre, correo, rol o contraseña; correo mal formado; contraseña de menos de 8 caracteres; vínculo que no corresponde al rol (ver tabla) |
| 404    | `idFacultad` o `idPrograma` no existen                                                |
| 409    | Ya existe un usuario con ese correo (sin distinguir mayúsculas)                        |

La contraseña es temporal: el login con correo y contraseña existe mientras llega el SSO.

### `PUT /api/usuarios/{idUsuario}`

Mismo cuerpo que `POST`. `contrasena` es opcional: si llega vacía o nula se conserva la actual.
Permite cambiar el rol; el vínculo anterior se reemplaza. `200` con el `UsuarioResponse`. Errores
iguales a `POST`, más `404` si el usuario no existe.

---

## B2 — Etapas del periodo y ventanas

### Cambio que rompe el frontend: nombres de las etapas

`tipoEtapa` ahora solo acepta estos cuatro valores (en `POST`/`PUT /api/periodos-academicos` y en
las respuestas). Hay que actualizar `src/utils/etapasPeriodo.js` y su test:

| `tipoEtapa`                  | Nombre para mostrar            | Acciones que bloquea              |
| ---------------------------- | ------------------------------ | --------------------------------- |
| `RECOLECCION_CALIFICACIONES` | Recolección de calificaciones  | Calificar (coordinación y vicerrectoría), cargar CSV de estudiantes |
| `REVISION_HALLAZGOS`         | Revisión de hallazgos          | (historia E, pendiente)           |
| `AGENDA_CITAS`               | Agenda de citas                | (historia F, pendiente)           |
| `FIRMA_ACTAS`                | Firma de actas                 | (historia G, pendiente)           |

Los valores viejos (`EVALUACION_ESTUDIANTES`, `CALIFICACION_COORDINACION`,
`CALIFICACION_VICERRECTORIA`, `RETROALIMENTACION`) responden 400. En las bases existentes la
migración V10 juntó las tres etapas de calificación en la recolección (de la primera apertura al
último cierre) y convirtió la retroalimentación en revisión de hallazgos.

Las etapas siguen siendo opcionales al crear el periodo; una etapa sin fechas cuenta como cerrada.
Las fechas son inclusivas y se comparan contra la fecha de Colombia.

Solo hay un periodo activo: activar uno desactiva el anterior (sin cambios).

### `GET /api/periodos-academicos/activo/ventanas`

Cualquier rol autenticado. Sirve para habilitar o deshabilitar botones según la fecha.

```json
[
  {
    "etapa": "RECOLECCION_CALIFICACIONES",
    "nombreEtapa": "Recolección de calificaciones",
    "fechaApertura": "2026-09-28",
    "fechaCierre": "2026-11-13",
    "abierta": true
  },
  { "etapa": "REVISION_HALLAZGOS", "nombreEtapa": "Revisión de hallazgos",
    "fechaApertura": "2026-11-16", "fechaCierre": "2026-11-20", "abierta": false },
  { "etapa": "AGENDA_CITAS", "nombreEtapa": "Agenda de citas",
    "fechaApertura": null, "fechaCierre": null, "abierta": false },
  { "etapa": "FIRMA_ACTAS", "nombreEtapa": "Firma de actas",
    "fechaApertura": "2026-11-30", "fechaCierre": "2026-12-04", "abierta": false }
]
```

Siempre llegan las cuatro, en ese orden. `409` si no hay periodo activo.

### Respuesta cuando la etapa está cerrada (409)

Cualquier acción bloqueada por fecha (hoy: `PUT /api/docentes/{id}/calificaciones/coordinacion`,
`PUT /api/docentes/{id}/calificaciones/vicerrectoria` y la carga de CSV) responde:

```json
{
  "mensaje": "La etapa Recolección de calificaciones del periodo 2026-2 está abierta del 2026-10-21 al 2026-10-30; hoy es 2026-10-20",
  "errores": {},
  "etapa": "RECOLECCION_CALIFICACIONES",
  "nombreEtapa": "Recolección de calificaciones",
  "fechaApertura": "2026-10-21",
  "fechaCierre": "2026-10-30",
  "fechaActual": "2026-10-20"
}
```

Si la etapa no está programada, `fechaApertura` y `fechaCierre` llegan `null` y el mensaje dice que
la vicerrectoría debe programarla. El 404 de alcance tiene prioridad: un coordinador que intenta
calificar a un docente de otra facultad recibe 404 aunque la etapa esté cerrada.

**Para las historias E, F y G:** el bloqueo es una sola línea en el servicio,
`ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.X)`, antes de modificar datos.

---

## D2 — Corrección de notas e historial

Corregir una nota es volver a enviar la calificación de esa fuente
(`PUT /api/docentes/{id}/calificaciones/coordinacion` o `/vicerrectoria`, o recargar el CSV de
estudiantes). Si las notas cambian, el backend:

1. guarda la nota nueva;
2. recalcula el promedio ponderado y lo guarda con una nueva fecha de cálculo;
3. agrega una fila al historial con la nota y el promedio de antes y de después.

Si se envían las mismas notas (aunque cambie la observación) no hay corrección. La primera
calificación de una fuente tampoco es una corrección.

### Cambio en `PUT /api/docentes/{id}/calificaciones/coordinacion` y `/vicerrectoria`

Campo nuevo, opcional: `motivoCorreccion` (máximo 500 caracteres). Solo se usa si la calificación
ya existía y sus notas cambian.

```json
{
  "dominioDisciplinar": 4.5, "planeacionAcademica": 4.0, "metodologiaEnsenanza": 4.0,
  "evaluacionAprendizaje": 4.5, "compromisoInstitucional": 5.0,
  "observacion": "…",
  "motivoCorreccion": "Error de digitación en la visita de aula"
}
```

`400` con `errores.motivoCorreccion` si supera 500 caracteres. Puede responder `409` si la
recolección está cerrada (ver B2) o si dos personas guardan lo mismo al mismo tiempo
(`"Otra persona modificó estos datos al mismo tiempo; vuelve a intentarlo"`).

### `GET /api/docentes/{idDocente}/calificaciones/correcciones?idPeriodo=`

Roles: `COORDINADOR` (solo docentes de su facultad; otro docente → `404 Docente no encontrado`) y
`VICERRECTOR`. `DOCENTE` → `403` (el docente nunca ve notas por fuente, regla A4).

`idPeriodo` opcional; sin él trae todos los periodos. Orden: de la más reciente a la más antigua.

```json
[
  {
    "idCorreccion": 3,
    "idPeriodo": 2,
    "nombrePeriodo": "2026-2",
    "fuente": "COORDINACION",
    "notaAnterior": 3.000,
    "notaNueva": 5.000,
    "promedioAnterior": { "estado": "COMPLETO", "valor": 3.90, "fuentesPendientes": [] },
    "promedioNuevo": { "estado": "COMPLETO", "valor": 4.50, "fuentesPendientes": [] },
    "idUsuario": 2,
    "nombreUsuario": "Andrés Felipe Ordóñez",
    "motivo": "Error de digitación en la visita de aula",
    "fechaCorreccion": "2026-10-20T10:15:42.120331"
  }
]
```

- `notaAnterior`/`notaNueva`: nota de la fuente = promedio de sus cinco aspectos (hasta 3 decimales).
- `promedioAnterior.valor` es `null` si el promedio estaba `INCOMPLETO`. En el historial
  `fuentesPendientes` siempre llega vacío; el estado basta.
- `idUsuario`/`nombreUsuario` son `null` solo para cambios de procesos del sistema (seed).

### Actas firmadas (historia G, pendiente)

No se construyen actas todavía. Cuando se firme un acta, debe **copiar** a sus propias columnas el
promedio de `promedios_docente` (valor, estado, fecha de cálculo) y las notas por fuente de ese
momento, y guardar el `id_correccion` más reciente del docente en ese periodo. Así el acta conserva
el valor original aunque después haya correcciones: las correcciones posteriores quedan en
`correcciones_calificacion` con fecha mayor a la de la firma y no tocan el acta.

### Concurrencia del promedio (pendiente de D1, resuelto)

`promedios_docente` se escribe con `INSERT … ON CONFLICT (id_docente, id_periodo) DO UPDATE`, así
dos recálculos simultáneos del mismo docente no fallan ni duplican filas. Lo demuestra
`PromedioConcurrenciaTest` (10 docentes, dos hilos por docente al mismo tiempo); con la versión
anterior (buscar y luego insertar) esa prueba fallaba por llave duplicada.

---

## C4 — Carga del CSV de evaluación de estudiantes

Flujo en la pantalla: el coordinador elige el archivo → `POST /api/importaciones/csv/validar`
(no guarda nada, muestra el reporte) → si `valido` es `true`, el mismo archivo va a
`POST /api/importaciones/csv`. Con C5 los dos endpoints aceptan además el mapeo de columnas
(ver la sección C5).

**Roles:** solo `COORDINADOR` puede validar e importar (vicerrector y docente → `403`). No existe
rol administrador. El historial lo ven coordinador (sus cargas) y vicerrector (todas).

**Formato de la petición:** `multipart/form-data` con el archivo en la parte `archivo`.

### Qué archivo acepta

- Separador `;` o `,` (se detecta en el encabezado). Comillas dobles para textos con `;`, `,` o
  saltos de línea.
- UTF-8 (con o sin BOM) o ISO-8859-1/Windows-1252, como lo guarda Excel en español.
- Decimales con punto o coma: `4.5` y `4,5` valen lo mismo. Sin separador de miles ni signo.
- Tamaño máximo 2 MB y 5 000 filas de datos (configurable con `docentia.importacion.*`).
- Primera fila: encabezados. Las filas totalmente vacías se saltan y no cuentan.

| Campo                      | Obligatorio | Encabezados que se reconocen solos (sin tildes ni mayúsculas)    |
| -------------------------- | ----------- | ----------------------------------------------------------------- |
| `CORREO_DOCENTE`           | sí          | correo, correo docente, correo del docente, correo electrónico, email |
| `DOMINIO_DISCIPLINAR`      | sí          | dominio disciplinar                                               |
| `PLANEACION_ACADEMICA`     | sí          | planeación académica                                              |
| `METODOLOGIA_ENSENANZA`    | sí          | metodología de enseñanza                                          |
| `EVALUACION_APRENDIZAJE`   | sí          | evaluación del aprendizaje                                        |
| `COMPROMISO_INSTITUCIONAL` | sí          | compromiso institucional                                          |
| `COMENTARIO`               | no          | comentario, comentarios, observación, observaciones               |

Las demás columnas (nombre, programa, etc.) se ignoran.

### Reglas por fila (se valida todo antes de guardar)

- El correo es obligatorio y debe ser de un docente activo **de la facultad del coordinador**. Si
  no existe o es de otra facultad el motivo es el mismo: `Docente no encontrado`.
- Las cinco notas van juntas: todas llenas o todas vacías. Cada una entre **0 y 5**.
- Una fila sin notas pero con comentario es válida (solo agrega el comentario).
- Una fila sin notas ni comentario es un error.
- Si un docente tiene varias filas con notas, su nota por aspecto es el **promedio** de esas filas,
  redondeado a 2 decimales. Con una sola fila por docente queda tal cual.
- **Todo o nada:** con un solo error no se guarda ninguna nota ni comentario.

### `POST /api/importaciones/csv/validar`

`200` siempre que el archivo llegue (aunque tenga errores; mirar `valido`).

```json
{
  "valido": false,
  "nombreArchivo": "evaluacion-estudiantes.csv",
  "hashSha256": "9f2c…64 caracteres",
  "codificacion": "UTF-8",
  "delimitador": ";",
  "columnas": ["Correo docente", "Dominio disciplinar", "…", "Comentario"],
  "mapeo": { "Correo docente": "CORREO_DOCENTE", "Dominio disciplinar": "DOMINIO_DISCIPLINAR" },
  "camposObligatoriosSinMapear": [],
  "filasProcesadas": 3,
  "filasConError": 1,
  "errores": [
    { "fila": 3, "columna": "Dominio disciplinar", "valor": "7", "motivo": "La nota debe estar entre 0 y 5" }
  ],
  "docentes": [
    {
      "idDocente": 4, "nombreCompleto": "Julián Camilo Hoyos",
      "correo": "julian.hoyos@uniautonoma.edu.co",
      "filas": 2, "filasConNotas": 2, "comentarios": 2,
      "dominioDisciplinar": 4.25, "planeacionAcademica": 4.00, "metodologiaEnsenanza": 4.00,
      "evaluacionAprendizaje": 4.00, "compromisoInstitucional": 4.00, "notaFuente": 4.05
    }
  ],
  "advertencias": ["Este mismo archivo ya se importó en 2026-2 (carga #3); volver a importarlo no duplica notas ni comentarios"]
}
```

- `fila` es el número de registro como en Excel (encabezado = 1). Es `null` si el error es del
  archivo completo (vacío, sin separador, comillas sin cerrar, columna obligatoria faltante);
  `columna` es `null` si el error es de la fila completa.
- `errores` trae como máximo 500; `filasConError` tiene el total.
- `docentes` solo incluye filas válidas. `notaFuente` es el promedio de los cinco aspectos.
- `advertencias`: archivo ya importado, recolección cerrada hoy, no hay periodo activo.

### `POST /api/importaciones/csv`

Importa en el **periodo activo**. Respuestas:

| Código | Cuándo | Cuerpo |
| ------ | ------ | ------ |
| `200`  | Se importó | `ResultadoImportacion` con `importacion.resultado = "EXITOSA"` |
| `422`  | El archivo tiene errores; **no se guardó nada** | `ResultadoImportacion` con `resultado = "RECHAZADA"` y el reporte |
| `409`  | No hay periodo activo, o la recolección de calificaciones está cerrada (cuerpo de B2) | error |
| `413`  | El archivo pesa más de 2 MB | `{ "mensaje": "El archivo pesa más de 2048 KB" }` |
| `400`  | Falta la parte `archivo` | `{ "mensaje": "Falta la parte \"archivo\" del formulario" }` |

```json
{
  "importacion": {
    "idImportacion": 5,
    "nombreArchivo": "evaluacion-estudiantes.csv",
    "hashSha256": "9f2c…",
    "idPeriodo": 2, "nombrePeriodo": "2026-2",
    "idUsuario": 2, "nombreUsuario": "Andrés Felipe Ordóñez",
    "fechaImportacion": "2026-10-20T09:41:10.231",
    "resultado": "EXITOSA",
    "codificacion": "UTF-8", "delimitador": ";",
    "filasProcesadas": 3, "filasConError": 0,
    "docentesAfectados": 2,
    "notasNuevas": 1, "notasCorregidas": 1, "notasSinCambios": 0,
    "comentariosNuevos": 3, "comentariosRepetidos": 0
  },
  "reporte": { "…": "mismo formato que /validar" }
}
```

Qué pasa al importar:

- La nota de la fuente `ESTUDIANTES` de cada docente se crea o se reemplaza (clave: docente +
  periodo + fuente). Si cambia, queda en el historial de correcciones (D2) con el motivo
  `Carga de CSV #<id> (<archivo>)`. La nota guarda qué carga fijó su valor.
- Se recalcula el promedio ponderado (D1) solo de los docentes cuya nota cambió.
- Los comentarios se guardan sin duplicar. Clave: docente + periodo + hash del texto normalizado y
  de su número de aparición en el archivo. Recargar el mismo archivo no agrega nada, pero si dos
  estudiantes escribieron exactamente lo mismo en el mismo archivo se conservan los dos.
- Repetir el mismo archivo: `notasSinCambios` y `comentariosRepetidos` suben, nada se duplica, y
  la carga igual queda registrada.
- **Los comentarios todavía no se muestran en ningún endpoint** (pendiente: decidir quién los ve).

### `GET /api/importaciones?idPeriodo=`

Lista de `importacion` (mismo objeto de arriba), de la más reciente a la más antigua. Incluye las
rechazadas. Coordinador: solo las suyas. Vicerrector: todas. Docente: `403`.
