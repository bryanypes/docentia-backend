# Decisiones

Decisiones tomadas donde la Resolución 074 de 2026, el Excel de historias o el código dejaban
ambigüedad. La fuente de verdad es la resolución; el Excel v2 tiene errores (ver
[RES074-TRAZABILIDAD.md](RES074-TRAZABILIDAD.md)).

## Pendientes de Vicerrectoría

| # | Tema | Decisión aplicada | Quién la confirma |
|---|------|-------------------|-------------------|
| P1 | Fuentes faltantes (Art. 3) | Se renormaliza entre las fuentes disponibles y el promedio queda PARCIAL con las faltantes expuestas. Sin ninguna fuente queda SIN_NOTA, nunca 0. | Vicerrectoría |
| P2 | Umbral de compromisos | 3,8 por defecto, que es el inicio de la categoría Alto. El requerimiento original decía 4,00. Es configurable con `DOCENTIA_COMPROMISOS_UMBRAL`. | Vicerrectoría |
| P3 | Quién corrige un acta firmada | Solo Vicerrectoría, con motivo. Configurable con `DOCENTIA_ACTAS_ROL_CORRECTOR`. | Vicerrectoría |

## Motor

- **Migración.** La instrucción pedía V18, pero V18 ya existía. Se usan V19 a V21 y no se editó
  ninguna migración aplicada.
- **La autoevaluación reemplaza a la vicerrectoría como tercera fuente.** La Res. 074 pondera
  estudiantes 50 %, coordinación 30 % y docente 20 %. Se retiraron los endpoints
  `/calificaciones/vicerrectoria` y la fuente pasa a `AUTOEVALUACION`.
- **Redondeo.** A dos decimales HALF_UP antes de clasificar.
- **Categoría en PARCIAL.** Coordinación y Vicerrectoría ven la categoría provisional, marcada
  como parcial. El docente no ve valor ni categoría hasta que el promedio esté COMPLETO (A4).
- **Agregados.** I11 y los indicadores cuentan solo promedios COMPLETO.
- **Seed.** Se agregaron tres docentes (2,85, 4,75 y 1,90), por lo que los indicadores de la
  demo cambian frente a los del Sprint 3.

## Actas

- **Firma.** Exige acta en borrador, confirmación explícita, etapa FIRMA abierta, cita activa y
  promedio COMPLETO. Firmar con un promedio PARCIAL se rechaza con 409, porque el acta es el
  resultado definitivo.
- **Documento del firmante.** Los usuarios no tienen número de documento; el PDF usa el correo
  institucional como identificación.
- **Dos hashes.** El PDF imprime el SHA-256 del contenido canónico. El SHA-256 del archivo se guarda
  aparte, porque un archivo no puede contener su propio hash.
- **PDF sin notas por fuente.** El docente lo descarga, así que solo lleva el resultado final (A4).
- **Verificación pública.** Devuelve validez y datos del acta, no notas.
- **Acción Muy bajo.** El sistema solo muestra "Alerta informativa para Vicerrectoría — la
  institución evalúa el proceso conforme a su normativa". No envía correos, no bloquea ni cambia
  nada, y el texto no menciona desvinculación.

## Análisis de comentarios (E4)

- **Publicación explícita.** Incluir o descartar no le muestra nada al docente. Hay que publicar,
  sin hallazgos por decidir y con al menos uno incluido. Después la revisión no cambia.
- **Editar no toca las citas.** Se corrige la redacción; las citas siguen siendo las verificadas.
- **Versión GENERADO.** Se crea en el primer cambio del hallazgo, con el modelo y la instrucción.
- **El docente ve** tipo y texto. No ve citas ni comentarios, para no exponer a quien escribió.

## Calendario

- **Las ventanas siguen siendo por fecha, sin scheduler,** y responden 409. La semana de la
  resolución es una etiqueta informativa.
- **Etapa vigente.** Si varias ventanas se solapan, como en el seed, se toma la más avanzada.
- **Última franja del día.** 23:00–23:30. La de 23:30–00:00 cruza de día y antes terminaba en un
  409 genérico; ahora recibe un mensaje claro.

## Notificaciones

- **Textos fijos por tipo.** La privacidad queda garantizada por diseño y no por revisar cada envío.
- **Alerta Muy bajo.** Solo con promedio COMPLETO, para no alarmar con un resultado provisional. El
  texto no nombra al docente ni la categoría, porque el correo no puede llevarla.
- **Cancelación de citas.** No hay endpoint para cancelar (F3 no se implementó) y este alcance no
  agrega funciones. Se cubre el cambio de cita, que libera la anterior.
- **Acta lista para firma.** Se avisa cuando se puede firmar: al crearla, o al agregar el compromiso
  si la nota lo exige.
- **Compromisos de actas en borrador.** No generan recordatorios hasta que el acta esté firmada.
- **Columnas de V22.** Siguen los nombres pedidos (`usuario_id`), no la convención `id_usuario`.

## Publicación

- **Gate de autores.** `git shortlog` muestra a Daniel Aranda y a Vero, colaboradores reales con
  commits ya públicos. Quitarlos exigiría reescribir historial, y eso está prohibido. Tus dos correos
  se unifican con `.mailmap`.
- **Repositorio `docentia-docker`.** Recibe una copia de cada versión publicada, con `backend/` y
  `frontend/`. El desarrollo sigue en `docentia-backend` y `DocenteIA`.
- **PostgreSQL en CI.** Las pruebas usan `embedded-postgres`, un PostgreSQL 16 real, así que no hace
  falta un servicio aparte.

## Endurecimiento

- **Numeración.** V22 notificaciones, V23 auditoría y V24 bloqueo optimista con índices.
- **Campo de bloqueo.** Se llama `version_bloqueo`, porque `version` ya es la versión del acta.
- **Promedios.** Se escriben con un upsert atómico; `version_bloqueo` sube en cada recálculo.
- **Rechazo por validación.** Bean Validation corre antes que `@PreAuthorize`: un rol sin permiso con un
  cuerpo inválido recibe 400 o 415 en lugar de 403. No expone datos. La matriz acepta los tres.
- **RFC 7807 compatible.** Se conservan `mensaje` y `errores`, que lee el frontend.
- **Tope de listados.** La respuesta se corta en 1000 filas de forma central, sin romper el contrato. Es
  un tope de respuesta, no paginación en base de datos.
- **429 en el login.** Se cuenta igual si el correo no existe, por eso no revela usuarios.
- **Correlación.** El identificador lo genera el servidor; no se acepta del cliente.
- **Exportación CSV.** No existe, así que la protección contra fórmulas no aplica.
- **Swagger.** No está instalado; la API se documenta en `docs/` y Postman.
- **Zona horaria.** La API corre en `America/Bogota`, igual que su reloj. La cola de correo compara con
  el reloj de la base, para no depender de que ambos coincidan.
- **Aviso de apertura.** Solo los primeros 3 días de la etapa; así las etapas ya abiertas del seed no
  generan correos, y una caída corta de la app no hace perder el aviso.
- **Seed silencioso.** Lo que crea el seed no notifica a nadie.
- **Campana.** Va fija en el contenedor principal para que las páginas no dependan de ella.

## Formularios del Anexo 1

- **Criterios de D05 a D08.** La asignación es ambigua en el PDF, por una celda combinada, y el Excel
  la reagrupa. Se asigna por la definición de cada criterio: ejecutar incluye acompañamiento; evaluar
  incluye procesos evaluativos y retroalimentación. La media de la fuente usa todos los descriptores,
  así que esto solo afecta el desglose visual.
- **Escala de 1 a 5.** La resolución no la define. Sus etiquetas están en configuración
  (`docentia.formularios.etiquetas-escala`) y quedan pendientes de confirmar con Vicerrectoría.
- **Precedencia.** La media del formulario prevalece sobre la nota previa de AUTO, COEVAL y HETERO.
  HU-01 corta el CSV de notas: la fuente `ESTUDIANTES` sale de formularios HETERO agregados.
  El reemplazo queda en el historial de correcciones.
- **Unidad HETERO.** Un formulario por (estudiante, clase, docente). Clase = curso + grupo + jornada
  del JSON de Campus. El detalle de HU-01 está en
  [DECISIONES-HETEROEVALUACION.md](DECISIONES-HETEROEVALUACION.md) y el contrato en
  [api-heteroevaluacion.md](api-heteroevaluacion.md).
- **Origen.** `calificaciones.origen` admite MANUAL, CSV y FORMULARIO. Se agregó MANUAL porque los datos
  existentes y las notas registradas por pantalla no son ninguno de los otros dos.
- **Notas por aspecto.** El instrumento no tiene los cinco aspectos anteriores. Al enviar, los cinco
  toman la media redondeada a 2 decimales para no romper el esquema, y `nota_fuente` guarda los 4
  decimales que usa el promedio.
- **Formulario anterior.** Las pantallas de calificar por aspectos se retiraron. Sus endpoints siguen
  para datos ya cargados y responden 409 si la fuente ya viene de un formulario.
- **Prueba de 4,41.** Catorce enteros no pueden promediar 4,8 exacto. Se usa 67/14 = 4,7857, que con
  4,5 y 4,0 da 4,41.
- **Dos coordinadores en una facultad.** La coevaluación es de quien la empezó; el otro recibe 404.
- **Auditoría.** `entidad_id` guarda el docente evaluado, y el periodo se deduce de la fecha.
- **Desglose del docente.** Mi evaluación muestra solo su autoevaluación por criterio. `GET
  /expedientes/propio` conserva `dimensiones`, valores ya ponderados que no revelan una fuente.

## Heteroevaluación (HU-01)

- **Base de Daniel.** Se conservan el rol `ESTUDIANTE`, cursos, clases, matrículas y el seed de Campus
  (V26). Su esquema de respuestas (V27) guardaba al estudiante junto a sus puntajes; V28 lo reemplaza
  por tablas anónimas y borra esas filas, que solo existían en desarrollo.
- **Asignación.** Es una fila de `clases_docente`, que recibió un id. Una matrícula RETIRADA es
  `es_activa = false`.
- **Anonimato.** `participaciones_heteroevaluacion` dice quién ya respondió; `respuestas_heteroevaluacion`
  dice qué se respondió, con id UUID aleatorio y sin estudiante, fecha ni secuencia. Ninguna consulta,
  log ni registro de auditoría las une.
- **Sin borrador.** El estudiante envía una sola vez; no se guardan respuestas parciales.
- **Mínimo de respuestas.** 5 por defecto (`docentia.heteroevaluacion.minimo-respuestas`). Por debajo,
  el formulario no reemplaza al CSV y no se muestra ningún agregado.
- **Nota de la fuente.** Media de todas las respuestas del docente en el periodo de la media simple de
  sus 11 descriptores, con 4 decimales.
- **Precedencia.** Con el mínimo, el formulario prevalece y un CSV posterior no lo pisa. Por debajo, manda
  el CSV; si no hay CSV, la fuente queda faltante y el promedio PARCIAL.
- **CSV restaurado.** El PR #4 había desactivado la carga CSV de estudiantes y borrado su prueba de
  concurrencia. Ambas vuelven, porque el CSV es el respaldo por debajo del mínimo.
- **Autenticación del estudiante.** El login con Google acepta cualquier correo institucional que exista,
  y la matrícula crea al estudiante. Fuera de desarrollo, el estudiante importado no recibe una
  contraseña utilizable. En desarrollo usa la contraseña de demo.
- **Matrícula por CSV.** El curso queda en el programa del docente; la clase se crea como grupo
  `UNICO`, porque el CSV no trae grupo ni jornada. Una columna opcional `estado` admite RETIRADA.
- **Cobertura.** Cuenta participación, nunca puntajes. Un estudiante cuenta una vez por cada asignación,
  y un retirado no cuenta.
- **Comentarios hacia el análisis de IA.** En 0.3.3 se recortó: copiarlos a `comentarios_estudiantes`,
  que tiene fecha y secuencia, los ligaba en el tiempo a la participación. Ahora no se copian: se leen de
  la respuesta solo al analizar y con la recolección cerrada. Ver
  [DECISIONES-ANALISIS-IA.md](DECISIONES-ANALISIS-IA.md).
- **Profe David Melo.** No había cambios suyos en los repositorios; los cambios de usuarios y roles
  venían del PR #4.

## Proceso de retroalimentación

Contrato: [api-proceso-evaluacion.md](api-proceso-evaluacion.md).

- **Fuentes y pesos sin cambios.** Siguen las tres fuentes de la Res. 074 (50/30/20). Vicerrectoría
  no califica: observa el proceso de todas las facultades.
- **Estado calculado.** `EstadoProcesoEvaluacion` se deduce del promedio, las citas y el acta en cada
  consulta. No hay una columna de estado que se pueda desfasar.
- **Estudiantes completos.** Se usa el mismo criterio del motor: la fuente cuenta cuando hay al menos
  un formulario HETERO enviado. La cobertura (respondidas de esperadas) se muestra a coordinación y
  vicerrectoría para que decidan, pero no bloquea.
- **Qué ve el docente.** Mientras falten estudiantes o coordinación solo sabe que su evaluación está
  en curso. Desde que solo falta su autoevaluación ve los pasos del proceso, nunca la nota.
- **Resultado en la reunión.** La nota, la categoría, el resultado por dimensión, los hallazgos y el
  acta del periodo se le muestran cuando la coordinación registra que asistió. La propiedad
  `docentia.proceso.exigir-asistencia` (por defecto `true`) permite apagarlo; las pruebas anteriores
  a esta regla corren con `false` y `ProcesoEvaluacionTest` la activa.
- **Confirmación.** El recordatorio de 24 h pide confirmar o declinar. Declinar libera la cita con
  `DECLINADA_POR_DOCENTE` y la franja vuelve a estar disponible para otro docente. Solo se puede
  responder antes de que empiece la cita.
- **Asistencia.** La registra la coordinación desde el día de la cita, en el calendario o en el
  expediente. `ASISTIO` exige el promedio COMPLETO; `NO_ASISTIO` libera la cita con `INASISTENCIA`
  y el docente debe reservar otra. Después de asistir ya no puede cambiar de franja.
- **Dos actas.** `COMPROMISO` exige al menos un compromiso con plazo; `RETROALIMENTACION` es la
  constancia de que recibió la retroalimentación. Con la nota bajo el umbral siempre es de
  compromiso; por encima, la coordinación elige al crearla o la cambia antes de la primera firma.
  Las actas anteriores a V29 toman el tipo según su nota.
- **Firma en la reunión.** Además de las reglas anteriores, el acta se firma después de registrar la
  asistencia. El docente la firma desde su sesión, en Mi acta.
- **Migración V29.** Agrega confirmación y asistencia a `citas` y `tipo` a `actas`. No se editó
  ninguna migración anterior.

## Firma, inicio y estadísticas

- **Firma dibujada.** Se firma en un modal con un lienzo (mouse, dedo o lápiz). Quien no puede
  dibujar escribe su nombre y se dibuja con letra manuscrita: el resultado es el mismo PNG. El
  backend valida que sea un PNG con trazo, guarda su hash y lo imprime en el PDF (V30).
- **Inicio único.** `/inicio` es la ruta principal de todos los roles y muestra el panel de cada uno.
  Solo cuando la sesión vence y entra la misma persona se vuelve a la pantalla anterior; tras un
  Salir nadie hereda la ruta del rol anterior.
- **Estadísticas.** Vista propia para coordinación y vicerrectoría. Se calculan en el navegador con
  `/docentes/seguimiento` y `/procesos-evaluacion`: una sola selección filtrada alimenta todas las
  gráficas. El Inicio conserva lo accionable y enlaza a ellas.
- **Panorama del análisis.** Sin docente elegido, Análisis muestra el avance de la facultad con
  conteos del backend, sin textos de comentarios ni de hallazgos.

## Estructura de repositorios

- El backend se trabaja en `docentia-backend`, carpeta `docentia-backend/`.
- El frontend se trabaja en el repositorio `DocenteIA`. La carpeta `DocenteIA/` del monorepo es una
  copia para Docker y se sincroniza al cerrar la versión. El login con Google solo estaba en esa
  copia y se portó a `DocenteIA`.
- La instrucción hablaba de 82 pruebas de frontend existentes; en `DocenteIA` había 26. Todas
  siguen en la suite.
