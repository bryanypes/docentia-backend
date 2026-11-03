# API — estado del proceso, cita y asistencia

Trazabilidad del proceso de cada docente en el periodo activo: evaluaciones, horario escogido,
confirmación, asistencia a la reunión y acta firmada. Las decisiones están en
[DECISIONES.md](DECISIONES.md#proceso-de-retroalimentación).

## Estados

Se calculan en cada consulta a partir de los datos (`EstadoProcesoEvaluacion`); no se guardan.

| Estado | Paso | Cuándo |
|--------|------|--------|
| `EVALUACIONES_EN_CURSO` | 1 | Falta Estudiantes o Coordinación |
| `PENDIENTE_AUTOEVALUACION` | 2 | Solo falta la autoevaluación |
| `EVALUACION_COMPLETA` | 3 | Promedio COMPLETO, sin cita activa |
| `NO_ASISTIO` | 3 | Su última cita se liberó por inasistencia |
| `CITA_AGENDADA` | 4 | Cita activa sin confirmar |
| `ASISTENCIA_CONFIRMADA` | 5 | El docente confirmó |
| `EN_REUNION` | 6 | La coordinación registró que asistió: resultado habilitado |
| `ACTA_EN_FIRMA` | 7 | El acta tiene una de las dos firmas |
| `FINALIZADO` | 8 | Acta firmada por las dos partes |

## Endpoints

| Método y ruta | Roles | Respuesta |
|---------------|-------|-----------|
| `GET /procesos-evaluacion?idFacultad=` | Coordinador, Vicerrector | Lista por docente del alcance; vacía sin periodo activo |
| `GET /procesos-evaluacion/{idDocente}` | Coordinador, Vicerrector | Un docente; 404 fuera del alcance |
| `GET /procesos-evaluacion/propio` | Docente | El suyo. En `EVALUACIONES_EN_CURSO` llega sin fuentes, cita ni hitos |
| `POST /citas/mia/confirmacion` `{asiste}` | Docente | `CitaResponse`. `false` declina y libera la franja |
| `POST /citas/{idCita}/asistencia` `{asistio}` | Coordinador | `CitaResponse`. `true` habilita el resultado; `false` libera la cita |
| `POST /citas/{idCita}/cancelacion` | Coordinador | `CitaResponse` liberada (`CANCELADA_POR_COORDINACION`); la franja queda libre |
| `POST /citas/{idCita}/reprogramacion` `{idFranja}` | Coordinador | `CitaResponse` de la nueva cita, sin confirmación; `idCitaLiberada` es la anterior (`REPROGRAMADA_POR_COORDINACION`) |
| `PATCH /actas/{idActa}/tipo` `{tipo}` | Coordinador | `ActaResponse`. `COMPROMISO` o `RETROALIMENTACION` |

`POST /actas` acepta `tipo` opcional. `ActaResponse` trae `tipo`; `CitaResponse` y la reserva de
`FranjaResponse` traen `confirmacion` y `asistencia`; `ExpedientePropioResponse` trae
`resultadoHabilitado`.

## Respuesta

```json
{
  "docente": { "idDocente": 4, "nombreCompleto": "Nora Muñoz", "...": "..." },
  "estado": "ASISTENCIA_CONFIRMADA",
  "nombreEstado": "Asistencia confirmada",
  "paso": 5,
  "fuentesPendientes": [],
  "coberturaEstudiantes": { "respondidas": 18, "esperadas": 25 },
  "cita": {
    "idCita": 3, "activa": true, "fecha": "2026-11-09", "horaInicio": "08:00:00",
    "horaFin": "08:30:00", "nombreCoordinador": "Marta Ruiz",
    "confirmacion": "CONFIRMADA", "fechaConfirmacion": "2026-11-08T09:00:00",
    "asistencia": null, "fechaAsistencia": null
  },
  "acta": null,
  "resultadoHabilitado": false,
  "hitos": [
    { "codigo": "RESERVA", "titulo": "Escogió horario", "detalle": "09/11/2026 08:00–08:30", "fecha": "..." },
    { "codigo": "CONFIRMACION", "titulo": "Confirmó asistencia", "detalle": "...", "fecha": "..." }
  ]
}
```

`coberturaEstudiantes` cuenta participaciones frente a matrículas activas, con el mismo criterio
de `GET /cobertura-heteroevaluacion`; nunca dice quién respondió y no llega al docente. Códigos de hito: `RESERVA`, `CAMBIO`, `DECLINACION`,
`CONFIRMACION`, `ASISTENCIA`, `INASISTENCIA`, `ACTA`, `FIRMA`, `CIERRE`.

## Reglas y errores

| Caso | Respuesta |
|------|-----------|
| Confirmar sin cita activa | 404 |
| Confirmar cuando la cita ya empezó, ya tiene asistencia o ya estaba confirmada | 409 |
| Registrar asistencia antes del día de la cita | 409 |
| Registrar `asistio: true` sin las tres evaluaciones | 409 |
| Registrar asistencia dos veces o en una cita liberada | 409 |
| Cita de otra facultad | 404 |
| Reservar otra franja después de haber asistido | 409 |
| Firmar el acta antes de registrar la asistencia | 409 |
| Acta de retroalimentación con nota bajo el umbral | 409 |
| Cambiar el tipo con el acta ya firmada por alguien | 409 |
| Cancelar o mover una cita liberada o con asistencia registrada | 409 |
| Mover a una franja cerrada, que ya empezó, de otro periodo, reservada o la misma | 409 |
| Cancelar o mover una cita, o usar una franja, de otro coordinador | 404 |

Hasta que la coordinación registre la asistencia, el docente recibe `GET /expedientes/propio` sin
`promedio.valor`, sin categoría ni resultado por dimensión, `GET /actas` sin el borrador del periodo
y `GET /analisis-ia/resultados/propio` sin hallazgos.

## Notificaciones

| Tipo | Destino | Cuándo |
|------|---------|--------|
| `RECORDATORIO_CITA` | Docente | 24 h antes; ahora pide confirmar o declinar |
| `ASISTENCIA_CONFIRMADA` | Coordinación de la facultad | El docente confirmó |
| `CITA_DECLINADA` | Coordinación de la facultad | El docente declinó; la franja quedó libre |
| `RESULTADO_HABILITADO` | Docente | La coordinación registró que asistió |
| `INASISTENCIA_CITA` | Docente | La coordinación registró que no asistió |
| `CITA_CANCELADA` | Docente | La coordinación canceló la cita |
| `CITA_CAMBIADA` | Docente | La coordinación movió la cita a otra franja |

`ACTA_POR_FIRMAR` ya no se envía antes de habilitar el resultado.

## Firma dibujada (V30)

`POST /actas/{id}/firmas` recibe `{confirmacion, trazo}`. `trazo` es el PNG de la firma hecha en la
página, como `data:image/png;base64,...`, de hasta 300 KB y 2000 px por lado, y con trazo visible.
Se guarda con su SHA-256 en `firmas_acta` y se imprime en el PDF bajo el nombre de quien firma.
Sin trazo, o con un lienzo en blanco, responde 422 `Dibuja tu firma antes de firmar el acta`.
`docentia.actas.exigir-firma-dibujada` (por defecto `true`) lo hace obligatorio. `ActaResponse.firmas[]`
trae `conTrazo`.

## Panorama del análisis

`GET /analisis-ia/panorama?periodoId=&idFacultad=` (coordinación y vicerrectoría) devuelve, por
docente del alcance, comentarios recibidos, estado del último análisis, si está publicado,
fortalezas, oportunidades, descartados, por decidir, citas y citas verificadas, más los totales.
Solo conteos: ningún texto de comentario o de hallazgo.
