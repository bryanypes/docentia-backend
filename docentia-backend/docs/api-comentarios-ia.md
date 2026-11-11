# API — análisis de comentarios de estudiantes

Contrato para el frontend. Si el código y este texto chocan, manda el código y se corrige aquí. Los
ejemplos de [ejemplos/ia/](ejemplos/ia/) son respuestas reales de la API, generadas por
`AnalisisComentariosTest#generarEjemplosParaElFrontend` con comentarios sintéticos.

Todas las rutas van bajo `/api`. Roles: **coordinador** (solo su facultad) y **vicerrector**. Docente,
estudiante y cualquier otro rol reciben 403. Un docente de otra facultad, o inexistente, da 404.

## Pedir el análisis (ya existía; un campo nuevo)

`POST /analisis-ia/solicitudes` — coordinador.

```json
{ "docenteId": 12, "periodoId": 3, "fuente": "FORMULARIO" }
```

| Campo | Valores | Notas |
|-------|---------|-------|
| `fuente` | `CSV`, `FORMULARIO` | Opcional; sin él es `CSV`, como antes |
| `modelo` | `LOCAL`, `LLM` | Opcional. `LOCAL`: reglas, no consume tokens. `LLM`: modelo de lenguaje externo, consume tokens. Sin él se usa el configurado por defecto |
| `motivo` | Texto de 10 a 300 caracteres | Obligatorio solo si el docente ya tiene un análisis COMPLETADO en el periodo: pedir otro es regenerar |

Respuestas: 202 en cola. 400 si `modelo` no es `LOCAL` ni `LLM`. 409 si se pide `LLM` y el
ambiente no tiene un modelo de lenguaje configurado (nunca se cambia al local por su cuenta), si ya hay
una en curso, si la revisión de hallazgos no está abierta o, con
`FORMULARIO`, si la recolección sigue abierta. Con un análisis COMPLETADO previo: 400 sin `motivo` y 429 si
no ha pasado el enfriamiento (`docentia.ia.enfriamiento-regeneracion-minutos`, 60) desde la última
solicitud. Tras ERROR o INSUFICIENTE se puede volver a pedir sin motivo ni espera. La respuesta de la solicitud trae además `fuente` y
`motivo`.

## Análisis de un docente

`GET /analisis-comentarios/docentes/{docenteId}?periodo={idPeriodo}`

| Campo | Tipo | Qué es |
|-------|------|--------|
| `estado` | enum | Ver tabla de estados |
| `motivo` | enum o null | Detalle del estado |
| `mensaje` | texto o null | Para mostrar tal cual |
| `docenteId`, `nombreDocente` | | |
| `periodoId`, `nombrePeriodo` | | El id es el que manda |
| `programa`, `facultad` | `{id, nombre}` | Del docente |
| `fuente` | `CSV`, `FORMULARIO` o null | De la versión vigente, o de la última solicitud |
| `idSolicitud` | número o null | Última solicitud |
| `version` | número o null | Versión del resultado mostrado |
| `generadoEn` | fecha y hora o null | |
| `estadoHallazgos` | `PENDIENTE_VALIDACION`, `VALIDADO` o null | El docente solo ve los validados y publicados |
| `resultado` | objeto o null | Resultado vigente; puede venir aunque haya otra corrida en curso |

### Estados

| `estado` | `motivo` | Qué mostrar | Ejemplo |
|----------|----------|-------------|---------|
| `COMPLETADO` | null | El resultado | [analisis-completado.json](ejemplos/ia/analisis-completado.json) |
| `EN_COLA` | null | "En cola"; si hay `resultado`, la versión anterior | [analisis-en-cola.json](ejemplos/ia/analisis-en-cola.json) |
| `EN_PROCESO` | null | "Generando" | [analisis-en-proceso.json](ejemplos/ia/analisis-en-proceso.json) |
| `INSUFICIENTE` | `POCAS_ENCUESTAS` | `mensaje`; no hay resultado | [analisis-insuficiente.json](ejemplos/ia/analisis-insuficiente.json) |
| `ERROR` | null | `mensaje` y opción de volver a pedirlo | [analisis-error.json](ejemplos/ia/analisis-error.json) |
| `SIN_COMENTARIOS` | `SIN_COMENTARIOS` o `TODOS_EXCLUIDOS` | No hubo comentarios útiles | [analisis-sin-comentarios.json](ejemplos/ia/analisis-sin-comentarios.json) |
| `NO_DISPONIBLE_AUN` | `SIN_SOLICITAR` o `VENTANA_ABIERTA` | Aún no se pidió, o la recolección sigue abierta | [analisis-no-disponible-aun.json](ejemplos/ia/analisis-no-disponible-aun.json) |
| `ERROR` | `LLM_NO_DISPONIBLE` | Se pidió el modelo de lenguaje y no está configurado; ofrecer el local | — |
| `RECHAZADO_PRESUPUESTO` | `PRESUPUESTO_AGOTADO` | `mensaje` y botón "Analizar con el modelo local" (`modelo: LOCAL`) | — |

### `resultado`

| Campo | Qué es |
|-------|--------|
| `tipoModelo` | `REGLAS` (modelo local) o `LLM` (modelo de lenguaje externo) |
| `versionModelo` | `reglas-1.0` o el modelo del proveedor |
| `versionLexicoOPrompt` | Versión del léxico (`lexico-2026.11-v1`) o de la instrucción (`analisis-v2`) |
| `nComentarios` | Comentarios útiles analizados |
| `nExcluidos` | `{motivo: cantidad}`; motivos en [ANALISIS_IA_CRITERIOS.md](ANALISIS_IA_CRITERIOS.md#exclusiones) |
| `sentimientoGlobal` | `{positivos, negativos, neutros, mixtos, indice}`; `indice` de -1 a 1 |
| `temas[]` | `{criterio, descriptor, nombre, polaridad, frecuencia, porcentaje, intensidad, relevancia, recurrente, citas[]}` ordenados por relevancia |
| `temas[].citas[]` | `{comentarioId, fragmento}`; el fragmento está copiado tal cual de un comentario anonimizado |
| `aspectos` | `{fortalezas[], oportunidades[]}`: temas recurrentes, máximo 5 cada uno |
| `tendencias[]` | `{criterio, descriptor, polaridad, porcentajeAnterior, porcentajeActual, cambio}` |
| `advertencias[]` | `{tipo, cantidad, mensaje}` |
| `criterios[]` | `{codigo, nombre, positivos, negativos, neutros, balance}` |
| `parametros` | Umbrales que se usaron |
| `limitesAplicados` | `{comentariosAnalizados, comentariosMuestreados, muestreado, recortados, descartadosRelleno, maxComentariosModelo, maxCaracteresComentario, periodosReferencia[]}` |
| `desdeCache` | `true` si la respuesta del modelo de lenguaje salió de la caché (no gastó tokens) |
| `generadoEn` | Fecha y hora de la corrida, a nivel del resultado (nunca por comentario) |
| `resumenNarrativo` | Resumen de 2 o 3 frases del modelo de lenguaje; nulo con el modelo local. Pendiente de validación como los hallazgos |

Los resultados guardados antes de este cambio traen esos cuatro campos en nulo.

Enums: `polaridad` POSITIVO, NEGATIVO, NEUTRO. `cambio` NUEVO, SE_MANTIENE, EN_AUMENTO,
EN_DISMINUCION, SIN_REFERENCIA. Advertencias: SENSIBLE_ACOSO, SENSIBLE_AMENAZA,
SENSIBLE_CONDUCTA_GRAVE, INSTRUCCION_SOSPECHOSA, LENGUAJE_OFENSIVO, TRUNCADO, POCOS_COMENTARIOS,
LIMITE_COMENTARIOS, CONTEO_POR_REGLAS.

## Comentarios anonimizados

`GET /comentarios-estudiantes/docentes/{docenteId}?periodo=&criterio=&sentimiento=&page=0&size=20`

Solo los comentarios útiles de la versión vigente, ya anonimizados. Nunca los excluidos.

| Parámetro | Valores |
|-----------|---------|
| `criterio` | `C1` a `C6`; otro valor da 400 |
| `sentimiento` | `POSITIVO`, `NEGATIVO`, `NEUTRO`, `MIXTO`; otro da 400 |
| `page` | Desde 0; negativo da 400 |
| `size` | 1 a 50; más de 50 se recorta a 50 (`tamano` dice el aplicado) |

Respuesta: `{disponible, motivo, docenteId, periodoId, fuente, version, pagina, tamano, total,
comentarios[{indice, texto, sentimiento, etiquetas[{criterio, descriptor, nombre, polaridad}]}]}`.
Sin análisis completado: `disponible: false` y `motivo` con el estado del análisis.
Ejemplos: [comentarios-disponibles.json](ejemplos/ia/comentarios-disponibles.json),
[comentarios-no-disponibles.json](ejemplos/ia/comentarios-no-disponibles.json).

## Resumen agregado

`GET /analisis-comentarios/resumen?periodo=&programa=`

Suma de los resultados vigentes del periodo en el alcance de quien consulta (el coordinador queda en su
facultad). `programa` es opcional; uno de otra facultad da 404. No trae docentes. Con menos de 3
docentes con análisis: `disponible: false`, `motivo: GRUPO_PEQUENO` y listas vacías.

Campos: `disponible, motivo, periodoId, programa, docentesConAnalisis, minimoDocentes, nComentarios,
nExcluidos, sentimientoGlobal{positivos, negativos, neutros, mixtos}, temas[{criterio, descriptor,
nombre, polaridad, frecuencia, porcentaje, docentes}], criterios[{codigo, nombre, positivos, negativos,
neutros}], advertencias{tipo: cantidad}`. Ejemplo: [resumen.json](ejemplos/ia/resumen.json).

## Elegir el modelo

`GET /analisis-comentarios/modelos` — coordinador y vicerrector. Para mostrar las opciones antes de
confirmar el pedido.

```json
{
  "porDefecto": "LOCAL",
  "opciones": [
    {"codigo": "LOCAL", "nombre": "Modelo local por reglas", "disponible": true, "consumeTokens": false, "descripcion": "..."},
    {"codigo": "LLM", "nombre": "Modelo de lenguaje externo", "disponible": true, "consumeTokens": true, "descripcion": "..."}
  ],
  "llmDisponible": true,
  "modeloLlm": "nombre-del-modelo",
  "maxComentariosLlm": 150,
  "maxCaracteresComentario": 300,
  "tokensPorAnalisis": {"entradaMaxima": 16000, "salidaMaxima": 4000, "totalMaximo": 20000},
  "tokensRestantesHoy": 500000,
  "advertencia": "El modelo de lenguaje envía una muestra anonimizada a un proveedor externo y consume hasta 20000 tokens por análisis nuevo. ..."
}
```

- Mostrar `advertencia` y `tokensPorAnalisis.totalMaximo` antes de confirmar `LLM`. No hay cifras en
  dinero.
- Si `llmDisponible` es falso, la opción `LLM` va deshabilitada con su `descripcion`.
- `tokensRestantesHoy` es nulo si no hay presupuesto diario.
- Medido con 150 comentarios: unos 10.300 tokens de entrada y 2.000 de salida (ver
  [ANALISIS_IA_COSTOS.md](ANALISIS_IA_COSTOS.md)). Una respuesta desde la caché no consume nada.
- Elegir `LLM` respeta la política de roles, el presupuesto y la caché igual que el modelo por
  defecto. Si se pidió `LLM` y el proveedor deja de estar configurado antes de procesar, la solicitud
  queda en `ERROR` con `motivo: LLM_NO_DISPONIBLE`.

## Regenerar

`POST /analisis-comentarios/docentes/{docenteId}/regeneracion` — coordinador y vicerrector.

```json
{ "periodoId": 3, "motivo": "Se corrigió la carga de respuestas", "fuente": "FORMULARIO", "modelo": null }
```

202 con la solicitud. 400 sin motivo (10 a 300 caracteres). 429 si la última solicitud tiene menos de
60 minutos. Si los comentarios no cambiaron, la corrida sale de la caché y no gasta tokens.

## Uso del proveedor externo

`GET /analisis-comentarios/uso?periodo=` — solo vicerrector.

`{periodoId, llamadas, aciertosCache, rechazadosPresupuesto, errores, tokensEntrada, tokensSalida,
tokensCacheProveedor, tokensAhorradosCache, ahorroCachePct, ahorroMuestreoPct, llamadasEstimadas,
costoEstimado}`. Sin texto de comentarios ni docentes. `costoEstimado` es nulo si no hay precios
configurados.

## Errores

| Código | Cuándo |
|--------|--------|
| 400 | `periodo` falta, o criterio, sentimiento o página inválidos |
| 403 | Rol sin permiso (docente, estudiante) |
| 404 | Docente, periodo o programa fuera del alcance o inexistente |
| 409 | Al pedir: ya en curso, ventana cerrada o recolección abierta |
| 429 | Más de 60 consultas por minuto |

## Para la pantalla

- **Rotular con honestidad.** `tipoModelo: REGLAS` es "Análisis automático por reglas", no "IA
  generativa". `LLM` es "Propuesto por un modelo de lenguaje". En los dos casos: "Pendiente de
  validación por coordinación" mientras `estadoHallazgos` sea `PENDIENTE_VALIDACION`.
- **Advertencias graves.** `SENSIBLE_*` va destacado solo para coordinación, sin texto de comentarios.
- **Citas.** Mostrar el `fragmento` tal cual; no reescribirlo.
- **Datos sintéticos.** Los ejemplos usan comentarios inventados; no copiar sus textos a la interfaz.
- **Muestreo.** Si `limitesAplicados.muestreado` es verdadero: "El modelo de lenguaje leyó
  {comentariosMuestreados} de {comentariosAnalizados} comentarios; los conteos y temas incluyen todos".
  No decir que se leyó todo.
- **Caché.** Si `desdeCache` es verdadero: "Resultado reutilizado; los comentarios no cambiaron desde
  la corrida anterior", junto a `generadoEn`.
- **Regenerar.** El botón pide el motivo y muestra el 429 como "Podrás regenerar en una hora".
