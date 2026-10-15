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
