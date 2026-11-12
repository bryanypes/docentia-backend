# API

Base: `/api`. Autenticación con `Authorization: Bearer <token>`, salvo `POST /sesiones`,
`POST /sesiones/google`, `GET /verificacion-actas/{codigo}` y `GET /actuator/health`.

La matriz completa de rol por endpoint es ejecutable y está en `MatrizAutorizacionTest`.

| Tema | Documento |
|------|-----------|
| Resolución 074: actas, acciones, panel, hallazgos | [api-res074.md](api-res074.md) |
| Heteroevaluación (estudiante → docente), contrato Campus | [api-heteroevaluacion.md](api-heteroevaluacion.md) |
| Estado del proceso, confirmación y asistencia a la cita | [api-proceso-evaluacion.md](api-proceso-evaluacion.md) |
| Contrato general para el frontend | [api-contrato-frontend.md](api-contrato-frontend.md) |
| Sesiones con Google | [api-sesiones-google.md](api-sesiones-google.md) |
| Periodos, calificaciones, importación | [api-sprint3-semana1.md](api-sprint3-semana1.md) |
| Agenda e indicadores | [api-sprint3-semana2.md](api-sprint3-semana2.md) |

Colección de Postman: [ejemplos/docentia.postman_collection.json](ejemplos/docentia.postman_collection.json).

## Nuevo en 0.3.1

| Método y ruta | Roles | Respuesta |
|---------------|-------|-----------|
| `GET /notificaciones?soloNoLeidas=&page=&size=` | Todos | Página propia, máximo 50 |
| `GET /notificaciones/no-leidas/conteo` | Todos | `{noLeidas}` |
| `PATCH /notificaciones/{id}/leida` | Dueño | 404 si es ajena |
| `PATCH /notificaciones/leer-todas` | Todos | 204 |
| `GET` y `PUT /notificaciones/preferencias` | Todos | `{correoHabilitado}` |
| `GET /auditoria?desde=&hasta=&accion=&usuario=&page=&size=` | Vicerrector | Página, máximo 50 |
| `GET /actuator/health/liveness` y `/readiness` | Público | Estado |

## Nuevo en 0.3.2

Todas aceptan `?periodo=`; sin él usan el periodo activo.

| Método y ruta | Roles | Respuesta |
|---------------|-------|-----------|
| `GET /formularios/autoevaluacion` | Docente | Estructura de 14 ítems y respuestas propias |
| `PUT /formularios/autoevaluacion` | Docente | Borrador: `{respuestas: {D01: 4}, comentarios: {C1: "..."}}` |
| `POST /formularios/autoevaluacion/enviar` | Docente | 422 incompleto; 409 ventana cerrada o ya enviada |
| `GET /formularios/coevaluacion/pendientes` | Coordinador, vicerrector | Estado y conteos, sin puntajes |
| `GET` y `PUT /formularios/coevaluacion/{idDocente}` | Coordinador | 404 fuera de alcance |
| `POST /formularios/coevaluacion/{idDocente}/enviar` | Coordinador | Igual que la autoevaluación |

## Nuevo en 0.3.3

| Método y ruta | Roles | Respuesta |
|---------------|-------|-----------|
| `GET /estudiante/evaluaciones?periodo=` | Estudiante | Asignaciones con estado PENDIENTE o ENVIADA |
| `GET /estudiante/evaluaciones/{idAsignacion}` | Estudiante | 11 ítems por criterio y estado; 404 si no es suya o está retirado |
| `POST /estudiante/evaluaciones/{idAsignacion}/enviar` | Estudiante | `{respuestas, comentario}`; 422 incompleto, 409 ventana cerrada o ya enviada, 429 por tope |
| `GET /cobertura-heteroevaluacion?periodo=&page=&size=` | Coordinador, vicerrector | Matriculados y respondieron por docente y programa |
| `POST /matriculas/csv` | Coordinador, vicerrector | Todo o nada; 422 con errores por fila |
| `GET /matriculas/plantilla` | Coordinador, vicerrector | CSV de ejemplo |

## Análisis de comentarios (sin publicar)

Contrato completo en [api-comentarios-ia.md](api-comentarios-ia.md).

| Método y ruta | Roles | Respuesta |
|---------------|-------|-----------|
| `POST /analisis-ia/solicitudes` | Coordinador | Campo nuevo opcional `fuente`: `CSV` (por defecto) o `FORMULARIO`; 409 con la recolección abierta. Si el docente ya tiene un análisis COMPLETADO en el periodo, exige `motivo` (400 sin él) y respeta el enfriamiento (429), igual que la regeneración |
| `GET /analisis-comentarios/docentes/{docenteId}?periodo=` | Coordinador, vicerrector | Estado y resultado vigente; 404 fuera de alcance |
| `GET /comentarios-estudiantes/docentes/{docenteId}?periodo=&criterio=&sentimiento=&page=&size=` | Coordinador, vicerrector | Comentarios anonimizados, máximo 50 por página |
| `GET /analisis-comentarios/resumen?periodo=&programa=` | Coordinador, vicerrector | Agregado sin docentes; no disponible con menos de 3 |
| `POST /analisis-comentarios/docentes/{docenteId}/regeneracion` | Coordinador, vicerrector | Con motivo; 429 durante el enfriamiento; pasa por la caché |
| `GET /analisis-comentarios/uso?periodo=` | Vicerrector | Llamadas, caché, tokens y ahorro; sin texto |
| `GET /analisis-comentarios/modelos` | Coordinador, vicerrector | Modelos que se pueden elegir (`LOCAL`, `LLM`), tope de tokens y advertencia |

## Errores

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "El acta firmada no se puede modificar",
  "mensaje": "El acta firmada no se puede modificar",
  "errores": {},
  "correlationId": "1f4e..."
}
```

El login responde 429 tras varios intentos fallidos. Un conflicto de versión responde 409. Los listados
sin paginación devuelven como máximo 1000 filas e informan el total en `X-Total-Count`.
