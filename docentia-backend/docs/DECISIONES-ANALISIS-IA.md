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

- **V34, no V29.** V29 a V33 las usa el PR #6 de Verónica. Si este PR se fusiona antes, el #6 tiene que
  renumerar sus migraciones o activar `spring.flyway.out-of-order`; si se fusiona después, nada cambia.
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

## Auditoría y avisos

- **Auditoría.** Pedir un análisis registra `SOLICITUD_ANALISIS` con el id de la solicitud. Nunca el
  texto de un comentario.
- **Avisos.** Se reutilizan los de análisis terminado, insuficiente y con error; no llevan datos de los
  comentarios.
