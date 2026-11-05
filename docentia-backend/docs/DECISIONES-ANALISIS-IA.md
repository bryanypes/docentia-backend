# Decisiones — análisis de comentarios de estudiantes

Decisiones tomadas al construir el análisis de comentarios con el modelo local y la fuente
FORMULARIO. Contrato: [api-comentarios-ia.md](api-comentarios-ia.md). Criterios:
[ANALISIS_IA_CRITERIOS.md](ANALISIS_IA_CRITERIOS.md). Validación:
[ANALISIS_IA_VALIDACION.md](ANALISIS_IA_VALIDACION.md).

## Modelo

- **Modelo local por defecto.** `docentia.ia.proveedor=local` en `application.yml`, `dev` y Docker. Es
  un modelo por reglas con un léxico versionado (`resources/ia/lexico-v1.txt`). No es un modelo de
  lenguaje ni genera texto: clasifica y cuenta. En pantalla se rotula con `tipoModelo = REGLAS`.
- **Modelo de lenguaje opcional.** `chat-completions` sigue disponible y apagado por defecto. Si falla,
  la solicitud termina en ERROR; no se recurre al modelo local en silencio.
- **Quién cuenta.** Aunque los hallazgos los proponga un modelo de lenguaje, los conteos, temas,
  tendencias y criterios del resultado los calcula siempre el modelo local, que es determinista y se
  puede verificar. El resultado queda rotulado `LLM` con la advertencia `CONTEO_POR_REGLAS`.
- **Pruebas con el simulado.** El perfil de pruebas sigue con `simulado` para no cambiar lo que ya
  probaban E1 a E3. Las pruebas nuevas activan `local` con `@TestPropertySource`.
- **Temperatura.** Se envía `temperature: 0` salvo que `DOCENTIA_IA_ENVIAR_TEMPERATURA=false`: un
  modelo reciente probado la rechaza. Sin temperatura no hay garantía de respuestas idénticas; las
  citas se siguen verificando.
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

## Auditoría y avisos

- **Auditoría.** Pedir un análisis registra `SOLICITUD_ANALISIS` con el id de la solicitud. Nunca el
  texto de un comentario.
- **Avisos.** Se reutilizan los de análisis terminado, insuficiente y con error; no llevan datos de los
  comentarios.
