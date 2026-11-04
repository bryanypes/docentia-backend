# Integración del análisis de comentarios

Guía para quien integre el análisis en estadísticas o en otra pantalla. El contrato está en
[api-comentarios-ia.md](api-comentarios-ia.md).

## De dónde sale cada dato

| Tabla | Qué guarda | Quién escribe |
|-------|------------|---------------|
| `solicitudes_analisis` | Pedido, estado, `fuente`, `motivo` | `AnalisisIaService`, procesador |
| `resultados_analisis_comentarios` | Una fila por corrida: versión, `reemplazado`, `tipo_modelo`, resultado JSON | Procesador |
| `comentarios_analizados` | Copia anonimizada de lo analizado, sin fechas ni ids de respuesta | Procesador |
| `hallazgos_analisis` | Hallazgos propuestos y su revisión (E4) | Procesador, revisión |
| `respuestas_heteroevaluacion` | Respuesta anónima del formulario | Estudiante; el análisis solo la lee |

El vigente de un docente y periodo es la fila con `reemplazado = false` (hay un índice único).

## Orden de llamadas

1. `GET /periodos-academicos/activo/ventanas` — saber si la recolección cerró.
2. `POST /analisis-ia/solicitudes` con `fuente` — 202.
3. `GET /analisis-comentarios/docentes/{id}?periodo=` cada pocos segundos mientras `estado` sea
   `EN_COLA` o `EN_PROCESO`; también llega el aviso "análisis terminado" a la campana.
4. `GET /comentarios-estudiantes/docentes/{id}?periodo=` para el detalle.
5. `GET /analisis-comentarios/resumen?periodo=&programa=` para las estadísticas por programa.

## Consistencia

- `resumen.nComentarios` = suma de `resultado.nComentarios` de los docentes del alcance con análisis
  vigente. Lo mismo `nExcluidos`, `sentimientoGlobal` y cada `temas[].frecuencia`. Lo comprueba
  `AnalisisComentariosTest.elResumenEsLaSumaYNoSeMuestraConPocosDocentes`.
- `temas[].frecuencia` de un docente = comentarios de `comentarios_analizados` con esa etiqueta.
- `porcentaje` es sobre comentarios útiles, no sobre encuestas ni matriculados.

## Qué no hacer

- No leer `respuestas_heteroevaluacion` para estadísticas de texto: se rompe la regla de recolección
  cerrada y el mínimo.
- No mostrar el resumen cuando `disponible` es falso, ni recalcularlo por docente desde el cliente.
- No unir `comentarios_analizados` con respuestas, participaciones ni fechas.
- No usar matrícula ni cobertura para el análisis: solo conteos de respuestas.
- No buscar periodos por nombre; siempre por id.
- No mostrarle al docente nada de estos endpoints; lo suyo es `/analisis-ia/resultados/propio`.
