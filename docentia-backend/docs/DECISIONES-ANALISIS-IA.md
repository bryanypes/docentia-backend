# Decisiones — análisis de comentarios de estudiantes

Decisiones tomadas al construir el análisis de comentarios con el modelo local y la fuente
FORMULARIO. Contrato: [api-comentarios-ia.md](api-comentarios-ia.md). Criterios:
[ANALISIS_IA_CRITERIOS.md](ANALISIS_IA_CRITERIOS.md). Validación:
[ANALISIS_IA_VALIDACION.md](ANALISIS_IA_VALIDACION.md).

## Modelo

- **Modelo local por defecto.** `docentia.ia.proveedor=local` en `application.yml`, `dev` y Docker. Es
  un modelo por reglas con un léxico versionado (`resources/ia/lexico-v1.txt`). No es un modelo de
  lenguaje ni genera texto: clasifica y cuenta. En pantalla se rotula con `tipoModelo = REGLAS`.
- **Modelo de lenguaje opcional.** `chat-completions` sigue disponible y apagado por defecto. Si falla
  de forma definitiva, el análisis lo hace el modelo local y queda rotulado con el motivo (regla P8,
  ver «Fallos del proveedor externo»). Solo queda en ERROR si el modelo local también falla.
- **Quién cuenta.** Aunque los hallazgos los proponga un modelo de lenguaje, los conteos, temas,
  tendencias y criterios del resultado los calcula siempre el modelo local, que es determinista y se
  puede verificar. El resultado queda rotulado `LLM` con la advertencia `CONTEO_POR_REGLAS`.
- **Pruebas con el simulado.** El perfil de pruebas sigue con `simulado` para no cambiar lo que ya
  probaban E1 a E3. Las pruebas nuevas activan `local` con `@TestPropertySource`.
- **Temperatura.** No se envía por defecto. La documentación de Anthropic dice que, en el endpoint
  compatible, Claude 4.7 y posteriores responden 400 a cualquier `temperature` menor que 1 (y a
  `top_p` distinto del valor por defecto). Con `DOCENTIA_IA_ENVIAR_TEMPERATURA=true` se envía
  `temperature: 0`, solo para un proveedor que la acepte. Sin temperatura no hay garantía de
  respuestas idénticas; las citas se siguen verificando.
- **Instrucción v2.** `analisis-v2` agrega que los comentarios son datos y que ninguna orden escrita en
  ellos se obedece. Las solicitudes ya completadas conservan `analisis-v1` en su borrador.

## Pipeline

- **Un solo pipeline.** `PipelineAnalisis` lo usan el flujo asíncrono y la validación, así se prueba lo
  mismo que corre.
- **Filtro de calidad también para el CSV.** Vacíos, solo emojis o puntuación, menos de 3 palabras,
  copias idénticas más allá de 3 e instrucciones al modelo se excluyen antes del filtro E2. Antes el
  CSV mandaba todo. El campo `comentariosFiltrados` de E1 sigue contando solo los excluidos por E2; el
  desglose completo va en `resultado.nExcluidos`.
- **Orden por huella.** Los comentarios se envían y guardan ordenados por SHA-256 del texto. El
  resultado no depende del orden de llegada ni de la carga. Dos pruebas de E1 se ajustaron a esto:
  ya no esperan la primera cita en el orden del CSV.
- **Nombre del docente.** Se reemplaza por `[docente]` además de los datos del estudiante.
- **Tope por corrida.** 2.000 comentarios útiles (`docentia.ia.analisis.maximo-comentarios`); si hay
  más, se analizan los primeros en orden de huella y se advierte `LIMITE_COMENTARIOS`.
- **Fortalezas y oportunidades.** Solo temas recurrentes, hasta 5 de cada una.

## Fuente FORMULARIO y anonimato

- **Se resuelve el recorte de 0.3.3.** Los comentarios del formulario no se copian al enviarse. Se
  leen de `respuestas_heteroevaluacion` solo al analizar, y solo el texto.
- **Solo con la recolección cerrada.** Pedir el análisis FORMULARIO con la recolección abierta responde
  409. Así el momento del análisis no se puede asociar con quien acaba de enviar.
- **Identificador.** El de cada comentario es su posición en el orden de la huella, no el UUID de la
  respuesta. Los excluidos del formulario no se guardan uno por uno (no hay id que guardar sin romper
  el anonimato): solo su conteo por motivo.
- **Mínimo.** FORMULARIO exige el mayor entre `docentia.ia.minimo-encuestas` (10, regla INSUFICIENTE) y
  `docentia.heteroevaluacion.minimo-respuestas` (5, k-anonimato), contando respuestas enviadas con o
  sin comentario. No se usan matrícula ni cobertura.
- **CSV intacto.** Sin `fuente`, la solicitud es CSV, como antes.

## Persistencia

- **V34 y V35.** El PR #6 de Verónica tenía V29 a V33; como V34 y V35 se fusionaron antes, sus
  migraciones se renumeraron a V36–V40 sin cambiar su contenido. No tocan las tablas del análisis.
- **Versiones.** Cada corrida completada crea una versión en `resultados_analisis_comentarios` y marca
  la anterior como reemplazada; no se borra. Un índice único parcial garantiza una sola vigente.
- **Copia sin tiempo.** `comentarios_analizados` guarda índice, id de cita, huella, texto anonimizado,
  sentimiento y etiquetas. Sin fecha, secuencia ni id de respuesta.
- **Concurrencia.** Ya existía un índice único de solicitudes en curso por docente y periodo; la segunda
  solicitud simultánea responde 409. Al guardar se bloquea la fila del docente.

## Consulta

- **Estado.** Sale de la última solicitud: EN_COLA, EN_PROCESO, COMPLETADO, ERROR o INSUFICIENTE. Se
  agregan SIN_COMENTARIOS (no hubo comentarios o el filtro excluyó todos) y NO_DISPONIBLE_AUN (sin
  solicitud, o la recolección sigue abierta).
- **Resultado vigente.** Se devuelve aunque haya otra corrida en curso, para que la pantalla no quede
  vacía mientras tanto.
- **Política en un lugar.** `PoliticaComentarios` decide los roles. Hoy: coordinador y vicerrector. Los
  roles Administrador y Visualizador no existen todavía; cuando lleguen, se agregan ahí.
- **El docente no entra.** 403 en los tres endpoints. Sigue viendo solo lo publicado en
  `/analisis-ia/resultados/propio` (A4).
- **Resumen con k-anonimato de docentes.** Con menos de 3 docentes con análisis
  (`docentia.ia.analisis.minimo-docentes-resumen`) no se muestra nada. El resumen no trae docentes.
- **Periodos por id.** Todas las consultas reciben el id del periodo; el anterior para tendencias se
  elige por `fecha_inicio`, nunca por el nombre.
- **Programa y facultad.** Las respuestas por docente traen las dos.
- **Tope.** Página de 50 como máximo y 60 consultas por usuario y minuto (429).

## Ahorro de tokens

Costos medidos y valores por defecto en [ANALISIS_IA_COSTOS.md](ANALISIS_IA_COSTOS.md).

- **Medir primero.** Cada llamada al proveedor externo deja una fila en `uso_ia` (V35): tokens de
  entrada, salida y caché que informa el proveedor (o estimados por caracteres, marcados), huella,
  conteos, resultado y duración. Sin texto de comentarios ni personas. La fecha sale del reloj de la
  aplicación, el mismo con que se mide el presupuesto diario.
- **Híbrido.** El modelo local etiqueta y cuenta todo; el de lenguaje recibe solo una muestra y redacta
  hallazgos y un resumen narrativo (`resultado.resumenNarrativo`). Los tokens casi no crecen con N.
- **Muestreo estratificado determinista.** Estrato = descriptor y polaridad de la etiqueta principal
  (o el sentimiento). Cada estrato tiene al menos un lugar; el resto se reparte en proporción. Dentro
  del estrato manda la huella del texto, no la fecha ni el orden de llegada.
- **Antes de muestrear** se quitan saludos y firmas y se descartan los casi duplicados y lo que queda
  sin contenido.
- **Citas por id.** La instrucción `analisis-v3` pide citar ids; el texto lo pone el sistema desde lo
  enviado. Un id que no se envió no vale. El lector sigue aceptando el formato anterior
  (`comentarioId` + `fragmento`), que usan los dobles de prueba de E1–E3.
- **Los ids enviados son los mismos de antes** (posición en el formulario, id en el CSV), no un índice
  nuevo: así las pruebas existentes y las citas guardadas siguen valiendo. En el formulario ya son
  números cortos.
- **Caché por huella** en `cache_respuestas_ia`. La huella incluye modelo, versión de la instrucción,
  topes, temas y la entrada exacta. Con el modelo simulado de pruebas, la respuesta guardada trae
  fragmentos anonimizados (formato anterior); con el proveedor real (formato compacto) solo trae ids.
- **Concurrencia.** Una corrida por docente y periodo (índice único ya existente). Dos docentes con la
  misma entrada en paralelo pueden llamar dos veces: es raro y la segunda guarda en caché sin error.
- **Presupuesto.** Por día y por periodo; 0 lo apaga. Si no alcanza, la solicitud queda en ERROR con
  `motivo: PRESUPUESTO_AGOTADO` y la consulta la muestra como `RECHAZADO_PRESUPUESTO`. No se cambia de
  modelo solo: se pide con `"modelo": "LOCAL"`.
- **Regenerar.** `POST /analisis-comentarios/docentes/{id}/regeneracion` con motivo (10 a 300
  caracteres, se guarda en la solicitud) y enfriamiento de 60 minutos (429). El `POST` de E1 no cambia
  ni tiene enfriamiento, para no romper el contrato; la caché y el presupuesto lo cubren igual.
- **Ventana.** Las tendencias usan solo los agregados guardados de hasta 3 periodos anteriores
  (por defecto 1); con varios se promedia el porcentaje de cada tema.
- **Instrucción v3.** Más corta, esquema compacto (`h`, `t`, `x`, `c`, `r`), y conserva la defensa
  contra inyección. Las solicitudes ya completadas conservan su versión.
- **Errores del proveedor.** El registro guarda solo el código HTTP, no el cuerpo, que podría repetir
  parte de la petición.
- **Uso.** `GET /analisis-comentarios/uso` solo para vicerrectoría, desde `PoliticaComentarios`.

## Elegir el modelo por solicitud

- **`modelo`: `LOCAL` o `LLM`.** Sin él, el configurado por defecto (`DOCENTIA_IA_PROVEEDOR`).
- **El externo queda disponible aunque el defecto sea el local**, si el ambiente trae URL, modelo y
  clave. Nunca se usa sin que lo pidan.
- **Sin proveedor, `LLM` es 409** al pedir. Si deja de estar configurado entre el pedido y el
  procesamiento, responde el modelo local con `motivo: LLM_NO_DISPONIBLE`, rotulado, y se avisa a
  la administración y a la vicerrectoría.
- **Mismas reglas.** Elegir `LLM` pasa por la política de roles (coordinador y vicerrector), el
  presupuesto y la caché. En `uso_ia` queda como `chat-completions` aunque el defecto sea `local`.
- **`GET /analisis-comentarios/modelos`** muestra las opciones, el defecto, el tope de comentarios, el
  máximo de tokens por análisis, el presupuesto que queda hoy y una advertencia. Sin dinero.
- **V41** amplía la restricción de `modelo_solicitado` a `LOCAL` y `LLM`.

## Fallos del proveedor externo

Objetivo: un fallo del proveedor nunca deja al docente sin resultado si el modelo local puede
responder. Cada clase de fallo tiene su tratamiento (`ProcesadorSolicitudesAnalisis.generar`):

| Clase | Cuándo | Tratamiento | Motivo si no se logra |
|---|---|---|---|
| Configuración | HTTP 400, 401, 403, 404, 422 y otros 4xx | Sin reintento. Registro ERROR con las variables a revisar y aviso `PROVEEDOR_IA_RECHAZO` a la administración y a la vicerrectoría (uno por día) | `PROVEEDOR_CONFIGURACION` |
| Transitorio | 408, 409, 429, 5xx (incluido 529), tiempo agotado o red | Hasta 2 reintentos (3 llamadas) con espera de 2 s y 8 s, o el `Retry-After` si es mayor; una espera no pasa de 30 s y todas juntas de 45 s | `PROVEEDOR_NO_DISPONIBLE` |
| Respuesta cortada | `finish_reason: "length"`, o un JSON que empieza y no termina | Un intento más con la mitad de los comentarios | `RESPUESTA_CORTADA` |
| Respuesta inválida | JSON completo pero mal formado o sin los campos pedidos | Un intento más igual | `RESPUESTA_INVALIDA` |
| Inesperado | Cualquier otra excepción al llamar | Sin reintento; registro ERROR sin mensajes que puedan traer texto | `PROVEEDOR_NO_DISPONIBLE` |

- **Respaldo local (regla P8, aprobada el 23 de noviembre de 2026).** Si no se logra, el análisis lo
  hace el modelo local con todos los comentarios preparados. La solicitud queda `COMPLETADO` con
  `modelo = reglas-1.0`, el `motivo` de la tabla y un `mensaje` que lo explica; el resultado lleva la
  advertencia `RESPALDO_LOCAL` y la pantalla del análisis dice «Usé el modelo local por reglas» y por
  qué. No hace falta migración: `motivo` es `varchar(30)` sin restricción.
- **Datos personales.** El respaldo reduce el tratamiento: el modelo local no usa red y nada sale del
  servidor. Es compatible con la Ley 1581 tal como la aplica `SEGURIDAD.md`.
- **Presupuesto.** Antes de repetir una llamada que gasta tokens (respuesta cortada o inválida) se
  vuelve a revisar el presupuesto; si no alcanza, responde el local. Un 429 o 5xx no gasta tokens. Una
  respuesta cortada sí: sus tokens quedan en `uso_ia` como `ERROR`.
- **Lectura tolerante.** El lector toma el primer objeto JSON completo de la respuesta, así acepta el
  bloque ```json``` y el texto antes o después.
- **Espera configurable.** `docentia.ia.reintento-base-ms` (2000 por defecto; 0 en pruebas). No es
  una variable del `.env`.
- **Razonamiento de Claude 5.** En Claude Haiku 5.5 el razonamiento está activo por defecto y sus
  tokens cuentan dentro de `max_tokens`. La documentación del endpoint compatible no dice si acepta
  apagarlo (`thinking: {type: "disabled"}` sí está documentado en la API nativa). Por eso el tope
  de salida recomendado sube a 8000 (`DOCENTIA_IA_MAX_TOKENS_SALIDA`) y la respuesta cortada se
  trata aparte.

## Auditoría y avisos

- **Auditoría.** Pedir un análisis registra `SOLICITUD_ANALISIS` con el id de la solicitud. Nunca el
  texto de un comentario.
- **Avisos.** Se reutilizan los de análisis terminado, insuficiente y con error, y se agrega
  `PROVEEDOR_IA_RECHAZO` para la administración y la vicerrectoría cuando el proveedor rechaza la
  configuración. Ninguno lleva datos de los comentarios.
