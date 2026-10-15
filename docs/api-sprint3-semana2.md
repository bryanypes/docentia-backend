# API DOCENTIA — Sprint 3, semana 2 (21–27 oct)

Endpoints nuevos de esta semana. Las convenciones (base `/api`, token Bearer, formato de error,
significado de 400/401/403/404/409/422) son las de `docs/api-sprint3-semana1.md`. Código nuevo:

| Código | Cuándo |
| ------ | ------ |
| 202    | La solicitud se registró y se procesa en segundo plano |
| 503    | La función está apagada en este ambiente (análisis con IA) |

---

## E1 — Análisis de comentarios con IA

El coordinador pide el análisis de los comentarios de un docente en un periodo. La petición vuelve
de inmediato (`202`, `EN_COLA`); un procesador en segundo plano genera el borrador. El frontend
consulta el estado cada pocos segundos hasta que deje de estar `EN_COLA` o `EN_PROCESO`.

```
EN_COLA ──► EN_PROCESO ──► COMPLETADO     (hay borrador)
                       ├─► INSUFICIENTE   (menos de 10 encuestas cerradas; sin borrador)
                       └─► ERROR          (el modelo no respondió bien; se puede volver a pedir)
```

**Encuesta cerrada:** respuesta de un estudiante con comentario abierto que llegó en una carga de
CSV exitosa (C4) para ese docente y periodo. El CSV de Power BI se exporta cuando la encuesta cerró,
así que todo lo importado cuenta. Las filas que solo traen notas no cuentan: sin comentario no hay
nada que analizar. Se necesitan **10**.

Las solicitudes viven en la base: si el backend se reinicia, las `EN_COLA` siguen en cola y las que
quedaron `EN_PROCESO` vuelven a la cola al arrancar.

### `POST /api/analisis-ia/solicitudes`

Rol: `COORDINADOR`, solo para docentes de su facultad. Vicerrector y docente → `403`.

```json
{ "docenteId": 4, "periodoId": 2 }
```

`202` con la solicitud (mismo formato que el `GET`), en `EN_COLA`.

| Código | Motivo |
| ------ | ------ |
| `400`  | Falta `docenteId` o `periodoId` |
| `404`  | Docente fuera del alcance o inexistente (`Docente no encontrado`), periodo inexistente |
| `409`  | La etapa **revisión de hallazgos** del periodo está cerrada (cuerpo de B2 con la etapa y las fechas) |
| `409`  | Ya hay una solicitud en curso para ese docente y periodo: `"Ya hay una solicitud de análisis en curso para este docente y periodo (#12)"` |
| `503`  | El análisis está apagado en este ambiente |

### `GET /api/analisis-ia/solicitudes/{idSolicitud}`

Roles: `COORDINADOR` (con alcance; si no, `404 Solicitud no encontrada`) y `VICERRECTOR`.
`DOCENTE` → `403`: **el borrador nunca se muestra al docente** mientras no esté validado (E4).

```json
{
  "idSolicitud": 12,
  "idDocente": 4,
  "nombreDocente": "Julián Camilo Hoyos",
  "idPeriodo": 2,
  "nombrePeriodo": "2026-2",
  "estado": "COMPLETADO",
  "mensaje": null,
  "encuestasCerradas": 13,
  "comentariosEnviados": 13,
  "fechaSolicitud": "2026-10-21T09:00:01.2",
  "fechaInicio": "2026-10-21T09:00:03.4",
  "fechaFin": "2026-10-21T09:00:04.1",
  "borrador": {
    "modelo": "simulado-determinista",
    "versionInstruccion": "analisis-v1",
    "fechaGeneracion": "2026-10-21T09:00:04.1",
    "estado": "PENDIENTE_VALIDACION",
    "hallazgos": [
      {
        "idHallazgo": 31,
        "tipo": "FORTALEZA",
        "texto": "Los estudiantes valoran: explica con ejemplos reales del sector.",
        "estado": "PENDIENTE_VALIDACION",
        "citas": [
          { "idCita": 40, "idComentario": 101, "fragmento": "Explica con ejemplos reales del sector y se nota que domina la arquitectura de software" }
        ]
      }
    ]
  }
}
```

- `borrador` es `null` salvo en `COMPLETADO`.
- `tipo`: `FORTALEZA` u `OPORTUNIDAD_DE_MEJORA`.
- En `INSUFICIENTE`: `"mensaje": "evidencia insuficiente (6 de 10 requeridas)"` y
  `encuestasCerradas` con el conteo.
- En `ERROR`: `"mensaje": "No fue posible generar el análisis en este momento. Puedes volver a
  solicitarlo más tarde."` (nunca detalles técnicos). Se puede volver a hacer `POST`.

### `GET /api/analisis-ia/solicitudes?docenteId=&periodoId=`

Mismos roles y alcance. Lista las solicitudes del docente en el periodo, la más reciente primero.
Sirve para que la pantalla del docente en coordinación muestre el último análisis sin guardar ids.

### Qué se envía al modelo

- La instrucción versionada (`src/main/resources/prompts/analisis-v1.txt`).
- Los comentarios **anonimizados**: se reemplazan correos, enlaces, usuarios (`@…`), códigos
  estudiantiles, números de 6 o más cifras y el nombre cuando el estudiante se presenta
  ("me llamo…"). No se envía el nombre del docente.
- El modelo debe responder `{"hallazgos":[{"tipo","texto","citas":[{"comentarioId","fragmento"}]}]}`.
  Si la respuesta no cumple el formato se pide una vez más; si vuelve a fallar, `ERROR`.

---

## E2 — Filtro de comentarios ofensivos o sobre rasgos personales

Antes de armar la petición al modelo, cada comentario pasa por un filtro. Los que se excluyen:

- **no se envían** al modelo, así que no pueden aparecer en el borrador ni citarse;
- quedan registrados en `comentarios_filtrados` con la solicitud, la categoría, la regla y la fecha.

Las **encuestas cerradas** se cuentan antes del filtro (el umbral de 10 mide la evidencia
recogida). Si el filtro excluye todos los comentarios, la solicitud queda `INSUFICIENTE` con el
mensaje `"evidencia insuficiente: el filtro excluyó todos los comentarios"`.

**Reglas:** `src/main/resources/filtros/reglas-comentarios.txt`, una por línea con el formato
`CATEGORIA | expresión regular`. Se comparan contra el texto en minúsculas, sin tildes y con
espacios simples. Categorías actuales: `INSULTO`, `APARIENCIA`, `EDAD`, `GENERO`, `VIDA_PRIVADA`.
Para usar un archivo fuera del jar: `docentia.ia.filtro-reglas=file:/ruta/reglas.txt` (requiere
reiniciar).

**Criterio:** se excluye lo ofensivo y lo que habla de la persona. Una crítica dura a la práctica
docente se conserva ("la metodología es pésima", "el material es viejo", "es el peor profesor, no
prepara las clases"). Por eso las palabras ambiguas solo cuentan cuando el sujeto es el docente
("el profesor está muy viejo" sí, "el material es viejo" no).

**Limitación:** un filtro por reglas no es perfecto. Puede dejar pasar un insulto escrito de forma
rara o, en casos poco comunes, excluir algo válido. Por eso cada hallazgo del borrador lo valida un
coordinador (E4) antes de que el docente lo vea, y la lista de filtrados es consultable.

### Cambio en `GET /api/analisis-ia/solicitudes/{id}`

Campo nuevo `comentariosFiltrados` (entero; `null` mientras la solicitud no se ha procesado).
`comentariosEnviados` ya descuenta los filtrados.

### `GET /api/analisis-ia/solicitudes/{idSolicitud}/comentarios-filtrados`

Roles y alcance iguales al `GET` de la solicitud (coordinador con alcance, vicerrector; docente
`403`; fuera de alcance `404`).

```json
[
  {
    "idComentario": 112,
    "texto": "Es un profesor muy feo y se viste mal.",
    "categoria": "APARIENCIA",
    "regla": "\b(se viste mal|mal vestid[oa]|su ropa|…)\b",
    "fechaFiltro": "2026-10-21T09:00:03.9"
  }
]
```

---

## E3 — Citas textuales verificables

Cada hallazgo trae sus citas: `idComentario` (el comentario de origen) y `fragmento` (el texto
citado). Al guardar el borrador, el servidor comprueba cada cita:

- el comentario tiene que ser uno de los que **se enviaron al modelo**: del mismo docente y periodo
  y **no filtrado** (E2);
- el fragmento tiene que aparecer en ese comentario, comparando en minúsculas, sin tildes, sin
  signos de puntuación y con espacios simples. Debe tener al menos 2 palabras completas.

Una cita inventada, parafraseada, de otro comentario o de un comentario filtrado queda con
`"verificada": false`.

| Estado del hallazgo     | Significado |
| ----------------------- | ----------- |
| `PENDIENTE_VALIDACION`  | Tiene al menos una cita verificada; el coordinador puede validarlo |
| `SIN_CITA_VERIFICADA`   | Ninguna cita se pudo verificar (o no trae citas); **no se puede validar** |
| `VALIDADO`              | El coordinador lo validó |

### Cambio en `GET /api/analisis-ia/solicitudes/{id}`

Cada cita del borrador trae `verificada` (booleano):

```json
"citas": [
  { "idCita": 40, "idComentario": 101, "fragmento": "las notas de los parciales se demoran", "verificada": true }
]
```

### `POST /api/analisis-ia/hallazgos/{idHallazgo}/validacion`

Rol: `COORDINADOR` con alcance sobre el docente del borrador. Sin cuerpo. Solo durante la etapa
**revisión de hallazgos**.

`200` con el hallazgo (`"estado": "VALIDADO"`). Validar dos veces no cambia nada.

| Código | Motivo |
| ------ | ------ |
| `403`  | Vicerrector o docente |
| `404`  | Hallazgo inexistente o de un docente fuera del alcance (`Hallazgo no encontrado`) |
| `409`  | La revisión de hallazgos está cerrada (cuerpo de B2) |
| `422`  | El hallazgo no tiene ninguna cita verificada: `"El hallazgo no tiene ninguna cita que aparezca en los comentarios de los estudiantes; no se puede marcar como válido"` |

### Visibilidad para el docente

El borrador **no se expone al docente en ningún endpoint**: los `GET` de análisis responden `403` al
rol docente y el expediente propio no incluye hallazgos. El borrador tiene su propio estado
(`borrador.estado`, hoy siempre `PENDIENTE_VALIDACION`). Cuando E4 permita validarlo completo
(`VALIDADO`), un endpoint del docente solo podrá mostrar borradores en ese estado y, dentro de ellos,
solo hallazgos `VALIDADO`. La regla ya está en el modelo (`SolicitudAnalisis.esVisibleParaDocente`).

---

## F1 — Franjas horarias del coordinador

Franjas de **30 minutos exactos** en las que el coordinador atiende citas de retroalimentación.
Siempre en el **periodo activo**. Abrir, cerrar y eliminar solo se puede durante la etapa
**agenda de citas** (409 con el cuerpo de B2 fuera de ella); consultar se puede siempre.

Reglas:

- `horaFin` = `horaInicio` + 30 minutos; inicio en minutos exactos y no después de las 23:30.
- La fecha no puede ser pasada y debe estar dentro de las fechas del periodo.
- Dos franjas **abiertas** del mismo coordinador no pueden cruzarse. Pegadas sí (9:00–9:30 y
  9:30–10:00). La regla también está en la base (restricción de exclusión sobre rangos), así que ni
  dos peticiones simultáneas pueden crear un cruce.
- Una franja **cerrada** no bloquea: se puede abrir otra en el mismo horario.
- Una franja con **cita reservada** no se puede cerrar ni eliminar (409).
- Solo se elimina una franja que nunca tuvo citas; si tuvo, hay que cerrarla (409).

Todos los endpoints son solo para `COORDINADOR` (vicerrector y docente → `403`). Cada coordinador
maneja únicamente sus franjas; una ajena responde `404 Franja no encontrada`.

### `POST /api/franjas`

```json
{ "fecha": "2026-11-09", "horaInicio": "08:00", "horaFin": "08:30" }
```

`201`:

```json
{
  "idFranja": 7,
  "fecha": "2026-11-09",
  "horaInicio": "08:00:00",
  "horaFin": "08:30:00",
  "estado": "ABIERTA",
  "reservada": false,
  "reserva": null
}
```

| Código | Motivo |
| ------ | ------ |
| `400`  | Duración distinta de 30 minutos, fecha pasada, fuera del periodo, campos faltantes |
| `409`  | Se cruza con otra franja abierta (`"Se cruza con la franja abierta del 2026-11-09 de 08:00 a 08:30"`), agenda cerrada, no hay periodo activo |

### `POST /api/franjas/lote`

Abre o cierra varias franjas. Cada una se procesa por separado: las que no se pueden se **omiten**
con su motivo y no impiden las demás. Nunca toca franjas reservadas. Máximo 200 por lote.

```json
{ "accion": "ABRIR", "franjas": [ { "fecha": "2026-11-09", "horaInicio": "08:00", "horaFin": "08:30" } ] }
```

```json
{ "accion": "CERRAR", "ids": [7, 8, 9] }
```

`200`:

```json
{
  "accion": "CERRAR",
  "aplicadas": [
    { "indice": 0, "idFranja": 7, "fecha": "2026-11-09", "horaInicio": "08:00:00", "horaFin": "08:30:00", "motivo": null }
  ],
  "omitidas": [
    { "indice": 1, "idFranja": 8, "fecha": "2026-11-09", "horaInicio": "08:30:00", "horaFin": "09:00:00", "motivo": "Tiene una cita reservada" },
    { "indice": 2, "idFranja": 9, "fecha": null, "horaInicio": null, "horaFin": null, "motivo": "Franja no encontrada" }
  ]
}
```

`indice` es la posición en la lista enviada. Motivos posibles al abrir: las mismas reglas del
`POST` individual y `"Se cruza con otra franja del mismo lote (08:00)"`. Al cerrar:
`"Tiene una cita reservada"`, `"La franja ya estaba cerrada"`, `"Franja no encontrada"`. La agenda
cerrada responde `409` para todo el lote.

### `PATCH /api/franjas/{idFranja}/cierre`

Sin cuerpo. `200` con la franja `CERRADA` (si ya estaba cerrada, igual). `409` si tiene cita
reservada.

### `DELETE /api/franjas/{idFranja}`

`204`. `409` si tiene cita reservada o historial de citas.

### `GET /api/franjas?fecha=&estado=`

Franjas propias del periodo activo, ordenadas por fecha y hora. Filtros opcionales: `fecha`
(`yyyy-MM-dd`) y `estado` (`ABIERTA`, `CERRADA`). Si una franja está reservada:

```json
"reservada": true,
"reserva": { "idCita": 3, "idDocente": 4, "nombreDocente": "Julián Camilo Hoyos" }
```
