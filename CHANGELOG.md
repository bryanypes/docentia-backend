# Cambios

## Sin versión (20 de noviembre de 2026)

Backend y frontend juntos. Sin migraciones: la base sigue en V50.

- **Léxico v2 del análisis local** (`lexico-2026.11-v2`, por defecto): entiende jerga colombiana y
  juvenil («melo», «brutal», «crack», «se rifa», «una nota», «qué chimba», «maluco», «jarto», «qué
  pereza», «se la monta»…), abreviaturas de chat («xk», «bn», «tmbn», «pa», «nd»), risas («jajaja»,
  «xd», sin peso en el sentido) y emojis (😍 🔥 👏 ❤️ positivos; 😴 🙄 👎 😡 negativos), con tono de
  piel o no. Las métricas del conjunto de validación no bajan frente al v1. El v1 se conserva.
- **Instrucción `analisis-v5`** para el modelo externo: interpreta la jerga, las abreviaturas y los
  emojis por su sentido y redacta los hallazgos en español formal.
- La coevaluación cuenta los comentarios que leyó el análisis.
- **La administración reabre directo**: en «Solicitudes de corrección» elige un docente, ve su
  autoevaluación y su coevaluación del periodo (quién la respondió y cuándo, sin puntajes) y reabre
  la que haga falta con un motivo, sin solicitud ni verificación del superior. Quien la respondió
  recibe el aviso «Formulario reabierto» y la vuelve a hacer; queda como solicitud atendida y en la
  auditoría. No se reabre si el acta del periodo ya está firmada. Nuevos
  `GET /solicitudes-reapertura/formularios` y `POST /solicitudes-reapertura/formularios/{id}/reapertura`.
- **Citas programadas** (administración): todas las citas agendadas del periodo, coordinación con sus
  docentes y vicerrectoría con los coordinadores, agrupadas por día, con buscador y filtros por tipo y
  estado (por confirmar, confirmada, asistió). Solo lectura y sin notas. Nuevo `GET /citas/programadas`.

## 0.4.4 (20 de noviembre de 2026)

Backend y frontend se despliegan juntos desde la etiqueta `v0.4.4`. Migraciones nuevas: la base pasa de V48 a **V50**.

- **El análisis de IA agrupa los comentarios** (V50, instrucción `analisis-v4`): los comentarios que
  dicen lo mismo forman un solo hallazgo, ordenado por los criterios de la evaluación docente (C1 a
  C6), y cada comentario aparece una sola vez. Aunque el modelo no lo haga, el servidor funde los
  hallazgos del mismo tipo y criterio y deja cada comentario en el hallazgo más respaldado. El modelo
  local también marca el criterio y ya no pone cifras en el texto.
- **Cada hallazgo** muestra su criterio, cuántos comentarios lo respaldan (dos a la vista y el resto
  plegado) y solo avisa de las citas que no se verificaron. «Ver versiones» pasa a ser «Historial de
  cambios» y solo aparece si alguien lo incluyó, descartó o editó; la vicerrectoría ya no recibe
  «Hallazgo no encontrado» al abrirlo en el análisis de un docente.
- **El análisis se pide cuando se necesite**: ya no espera a que cierre la recolección de comentarios
  ni a la etapa de revisión de hallazgos. Siguen haciendo falta al menos 5 comentarios.
- **Volver a analizar** no pide motivo y se habilita 5 minutos después de la última solicitud
  (`DOCENTIA_IA_ENFRIAMIENTO_MINUTOS`), o de inmediato si llegaron comentarios nuevos; la pantalla
  muestra los minutos que faltan.
- **Panel de análisis de comentarios**: el resumen dice cuántos comentarios hay y cuántos docentes
  están listos para analizar, con evidencia insuficiente (menos de 5 comentarios), sin comentarios o
  con error; cada cifra filtra la lista. La lista tiene buscador y filtros con su cantidad y una
  explicación, y cada docente dice cuántos comentarios le faltan para poder analizarlo.
- **Anonimato de la nota de estudiantes**: con la recolección abierta la nota solo se actualiza cada
  4 respuestas (`DOCENTIA_HETEROEVALUACION_BLOQUE`) y se consolida con todas al cerrar la recolección
  (tarea programada). El recálculo automático ya no aparece en el historial de correcciones: antes
  cada envío dejaba la nota de antes y de después y eso permitía deducir lo que calificó un
  estudiante.
- **Coevaluación con el análisis de comentarios**: antes de calificar, DocenteIA le pide a quien
  coevalúa revisar el análisis y le dice cuántos comentarios hay; si ya existe, muestra lo que dicen
  los estudiantes por criterio, sin comentarios textuales. Si la coevaluación se envió sin análisis,
  pide rectificarla con «Solicitar corrección»; la lista de coevaluaciones las marca «Sin análisis de
  IA». El docente también ve el criterio en «Lo que dicen tus estudiantes».
- **Estadísticas de comentarios**: cada fortaleza y oportunidad deja ver a qué docentes se refiere,
  con cuántas menciones y enlace a su expediente.
- **Acciones sugeridas**: buscador y filtros por programa, categoría y desempeño; un aviso cuenta los
  docentes con promedio por debajo de 4,0 y los filtra con un clic para que la vicerrectoría los
  revise.
- **«Para revisar» por debajo de 4,0** (`DOCENTIA_UMBRAL_REVISION`): una nota final completa por
  debajo del umbral queda marcada «Para revisar» en el expediente, la lista de docentes (con su
  filtro) y acciones sugeridas, y la vicerrectoría recibe el aviso «Promedio para revisar». Las
  categorías de la Resolución 074 no cambian; «Muy bajo» sigue siendo la alerta informativa y el
  docente no ve la marca.
- **Expediente**: las tarjetas se acomodan sin huecos; el historial de correcciones y el seguimiento
  del plan llegan plegados y solo se consultan al abrirlos.
- **Proveedor externo de IA**: el aviso lo da DocenteIA en la conversación la primera vez; después
  queda una línea con los límites y un enlace para volver a leerlo.

- **PDF del acta como documento institucional**: membrete con el logo y el nombre de la Corporación
  Universitaria Autónoma del Cauca (sin el título DOCENTIA), código y «Página N de M» arriba a la
  derecha, título centrado, secciones numeradas, texto en serif justificado y bloque de firmas con
  nombre, cargo, correo y fecha. Se redacta como carta, sin tablas: lugar, fecha y hora de la
  reunión, escuela y programa del docente, cargo de quien evaluó, resultado, fortalezas y aspectos
  por mejorar del análisis de IA validado (sin conteos de comentarios), lo tratado en la reunión y
  los compromisos con su plazo. Ya no muestra la versión del acta. Dice «Escuela» donde decía
  «Facultad». Las actas firmadas antes conservan su PDF.
- **Lo tratado en la reunión** (V49): quien evalúa escribe, antes de la primera firma, lo que se
  habló en la reunión; va en el PDF y entra en la huella del contenido. Una vez firmada, el
  disparador de la base impide cambiarlo.
- **Verificación con la dirección del despliegue**: el enlace impreso apunta a
  `https://nova.uniautonoma.edu.co/Evadocente-app/verificar-acta/<código>` (configurable con
  `DOCENTIA_ACTAS_URL_VERIFICACION`). La página pública de verificación tiene membrete, deja escribir
  el código (también en `/verificar-acta` sin código) y ya no muestra la versión.
- **Actas firmadas** (administración y vicerrectoría): registro de todas las actas firmadas con
  buscador por docente, programa o código, filtro por tipo, vigentes y reemplazadas, detalle de quién
  firmó y descarga del PDF. La administración puede descargar cualquier acta; cada descarga queda en
  la auditoría. Nuevos `GET /actas/registro` y `GET /actas/codigo/{codigo}` (sin nota ni categoría).

- **Análisis de IA desde la vicerrectoría**: la vicerrectoría pide el análisis de comentarios de
  cualquier docente o coordinador. Valida, descarta, edita y publica los hallazgos de los
  coordinadores; los de los docentes los sigue revisando la coordinación de su facultad.
- **Panel por programa interactivo**: búsqueda, orden (mejor o menor promedio, cobertura, nombre),
  las categorías del total filtran los programas y cada programa enlaza a sus estadísticas.
- **Acciones sugeridas**: muestra el promedio de cada docente y ordena del mejor al peor (o al revés).
- **Imprimir resumen** del expediente rediseñado: logo de la Uniautónoma, DOCENTIA y el robot de la
  IA, la nota final con su nivel, las notas por evaluador con barras y los compromisos. Ya se imprime
  completo (antes el navegador lo recortaba).
- **Responsividad**: en el celular, Usuarios ya no se sale de la pantalla (filtros a lo ancho, tabla
  sin ancho mínimo, títulos de facultad que parten línea); los filtros de Docentes se pliegan bajo
  **Filtrar** y sus tarjetas son compactas; Acciones sugeridas muestra una tarjeta por docente con
  promedio, categoría y acción.
- **Actas de coordinadores**: el PDF y la pantalla dicen «la vicerrectoría» donde antes decían «la
  coordinación» (reunión, constancia de firmas y quién agrega los compromisos). Se revisó el proceso
  completo de cita, asistencia, acta, firmas y PDF para docente, coordinador y vicerrectoría.

## 0.4.3 (18 de noviembre de 2026)

Backend y frontend se despliegan juntos desde la etiqueta `v0.4.3`. Migraciones nuevas: la base pasa de V43 a **V48**.

- **El coordinador también es docente evaluado** (V44): lo evalúan sus estudiantes, su autoevaluación
  y la vicerrectoría en el lugar de la coordinación, con el mismo peso. Para los estudiantes es un
  docente más. La vicerrectoría abre franjas que solo ven los coordinadores, los coevalúa, registra su
  asistencia y firma su acta («Firma vicerrectoría» en el PDF). El coordinador tiene en su menú
  «Mi evaluación como docente» (evaluación, autoevaluación, cita y acta) y ve su promedio en cuanto
  tiene las tres notas.
- **Promedio solo con las tres notas** (V45): ya no hay promedio parcial renormalizado. Mientras falte
  una nota, cada fuente pendiente figura con 0,00 y el promedio dice «Pendiente»; sin categoría ni
  acción sugerida.
- **Análisis de comentarios con IA** (V46): basta un mínimo de **5 comentarios** de estudiantes
  (`DOCENTIA_IA_MINIMO_COMENTARIOS`), en lugar de 10 encuestas. Si llegan comentarios después de un
  análisis, la coordinación ve «Tenemos nuevos comentarios de estudiantes, por favor actualiza el
  análisis de IA» y lo actualiza sin motivo ni hora de espera.
- **Estadísticas de comentarios**: el resumen «Lo que dicen los comentarios» (sentimiento, balance por
  criterio y temas) llega a Estadísticas de coordinación y vicerrectoría. Venía de la rama
  `feature/V1-vistas-paneles-y-comentarios`, que nunca se integró.
- **Tutorial del primer inicio de sesión** (V47): todos lo ven al entrar. Bloquea la pantalla con el
  robot, recorre las páginas del rol paso a paso, avanza solo como un video (con pausa) y se puede
  saltar un paso o todo. Al terminar queda en el botón **?** junto a la campana y en Mi perfil.
- **Solicitudes de corrección** (V48): quien envió una autoevaluación o coevaluación pide corregirla
  con un motivo; la verifica su superior (coordinación para docentes, vicerrectoría para coordinadores
  y para las coevaluaciones de la coordinación) y la administración reabre el formulario, que se
  corrige aunque la recolección haya cerrado. Avisos en la plataforma y por correo en cada paso. No se
  pide si el acta del periodo ya está firmada.
- **Administración de usuarios**: resumen «Dónde están los usuarios» por facultad y rol, filtro por
  facultad y agrupación por facultad, rol o programa. Los coordinadores llevan «También docente».
- **Correcciones**: el expediente ya no se desborda en el celular y «Imprimir resumen» imprime el
  resumen completo aunque el proceso no haya cargado.

## 0.4.2 (17 de noviembre de 2026)

Solo frontend, sin migraciones: la base sigue en V43. Backend y frontend se despliegan juntos desde la
etiqueta `v0.4.2`.

- **Confirmación al enviar una evaluación** (PR #32, Verónica): al enviar la heteroevaluación, la
  autoevaluación o la coevaluación ya no queda el asistente de pasos. Se ve solo «Tu evaluación ha sido
  recibida», con la fecha o el aviso de anonimato, el resumen por criterio y **Volver al inicio**; en la
  coevaluación, también **Ver el expediente del docente**. Lo mismo al abrir una evaluación ya enviada:
  las respuestas ya no se muestran, solo el resumen. El foco pasa al título para el lector de pantalla.

## 0.4.1 (13 de noviembre de 2026)

Mejoras de acceso y ayuda (PR #30, Verónica) y una corrección del límite de intentos. Sin migraciones:
la base queda en V43. Backend y frontend se despliegan juntos desde esta etiqueta, porque el login usa
los campos nuevos del 401 y del 429.

- **Corrección del bloqueo:** si otra petición del mismo correo entraba bien o limpiaba los intentos
  vencidos justo mientras se calculaba la espera, el login respondía 500. Ahora la purga, el conteo y
  el cálculo ocurren juntos.
- **Inicio de sesión:** cada intento fallido dice cuántos lleva la persona («Llevas 2 de 5 intentos.
  Te quedan 3.») y, al superar el límite, la pantalla muestra un contador con el tiempo que falta y
  desactiva **Ingresar** hasta que termina. El mensaje sigue sin decir si falló el correo o la
  contraseña. La API agrega `intentosUsados` e `intentosMaximos` al 401 del login, y
  `segundosParaReintentar` con la cabecera `Retry-After` al 429.
- **Ingreso pausado:** el bloqueo reemplaza el formulario por un cronómetro centrado con anillo de
  progreso. El fin del bloqueo se guarda en el navegador, así que recargar la página no lo esconde.
- **Correo externo:** si el correo no termina en `@uniautonoma.edu.co`, DocenteIA lo explica antes de
  enviar nada al servidor (el intento no cuenta).
- **Solicitar ayuda:** enlace a los tickets de NOVA en el login (siempre al pie, y destacado al llegar
  al límite o en el bloqueo) y en «No tienes docentes por evaluar», que reemplaza a «No tienes
  evaluaciones» con **Intentar nuevamente**. Los enlaces van según el perfil y se pueden cambiar con
  `VITE_URL_NOVA`.
- **Acceso compacto y sin scroll:** login, recuperar contraseña y cambio de contraseña comparten una
  tarjeta con la marca en una sola fila (DocenteIA, DOCENTIA y el logo de la Universidad) que cabe sin
  desplazarse en un portátil al 100 % y en un celular. El robot ya no se superpone al texto y las
  animaciones son solo de color. El login tiene **Volver al inicio**; recuperar contraseña ofrece
  siempre **Ir al inicio**, **Ir al login** y **Solicitar ayuda**, haya o no correo configurado.
- **Logo institucional con fondo transparente.**
- **Página de inicio compacta:** sin animaciones de desplazamiento; foco en el anonimato, cómo funciona
  en tres pasos, invitación a los tutoriales y buzón de ayuda con los servicios de NOVA (tickets por
  perfil, PQRS, directorio y manual).
- **Tutoriales:** página pública `/tutoriales` con un video por rol. Mientras no tengan enlace se
  muestran como pendientes; los enlaces se ponen en `src/shared/constants/tutoriales.js`.
- **Docente sin materias:** `GET /expedientes/propio` agrega `clasesEnPeriodo`. Con cero clases y sin
  ninguna nota, **Mi evaluación** dice «No tienes materias asignadas en este periodo» con **Intentar
  nuevamente** y **Solicitar ayuda**.
- **Semilla de desarrollo:** `estudiante.sinmaterias` y `docente.sinmaterias` para probar esos estados.

## 0.4.0 (12 de noviembre de 2026)

### Resumen

0.4.0 cierra el ciclo de la Resolución 074 sobre lo publicado en 0.3.x: el motor de la resolución
(0.3.0), los formularios AUTO y COEVAL del Anexo 1 (0.3.2) y la heteroevaluación anónima de estudiantes
(0.3.3). Sobre esa base agrega:

- **Proceso completo hasta el acta:** cita con confirmación, asistencia registrada por coordinación,
  acta de compromiso o de retroalimentación, firma dibujada con su hash en el PDF y trazabilidad.
- **Análisis de comentarios de estudiantes** con selector de modelo: `LOCAL` (reglas, sin red ni costo,
  por defecto) o `LLM` (proveedor externo opcional), incluida la fuente del formulario anónimo una vez
  cerrada la recolección.
- **Ahorro de tokens** con el proveedor externo: muestra de 150 comentarios, caché por huella,
  presupuesto por día y periodo, y regeneración con motivo y espera de una hora.
- **Cuestionarios por pasos** para docentes y estudiantes, estadísticas con exportación a Excel y
  agenda en calendario.
- **Recuperar la contraseña por correo**, con enlace de un solo uso que vence en 30 minutos.
- **Roles ADMINISTRADOR y VISUALIZADOR:** la administración (usuarios, programas, ponderaciones y
  fechas) sale de la vicerrectoría; el visualizador solo consulta el panel por programa.
- **Periodos con nombre automático** (AAAA-1 o AAAA-2 según la fecha de inicio) y **firma del acta
  subiendo un PNG**, además de dibujarla o escribirla.
- **Rediseño** de toda la interfaz: presentación pública, inicio por rol, destacados, estadísticas con
  lectura en una frase, paginación en las listas largas y diseño responsive.
- **Operación:** primer administrador desde el entorno, publicación bajo una subruta, validación de la
  configuración de producción al arrancar, respaldos verificados, runbook y kit de despliegue.

### Migraciones

Diez nuevas, de V34 a V43. V29 a V33 no existen: eran de una rama que se renumeró a V36–V40.
Además, los callbacks de Flyway `beforeMigrate.sql` y `afterMigrate.sql` apagan el disparador de actas
firmadas solo mientras se migra: sin ellos, V36 fallaba sobre una base con actas firmadas o anuladas.

| Versión | Qué hace |
|---------|----------|
| V34 | Resultado versionado del análisis de comentarios y copia anonimizada de lo analizado |
| V35 | Medición del gasto del proveedor externo (`uso_ia`) y caché de sus respuestas |
| V36 | Confirmación y asistencia de la cita, y tipo de acta |
| V37 | Trazo de la firma y su hash |
| V38 | Nombres con mayúscula inicial (datos) |
| V39 | Tildes en palabras académicas de los nombres (datos) |
| V40 | Cita cancelada o movida por la coordinación |
| V41 | Modelo pedido en cada solicitud de análisis (`LOCAL` o `LLM`) |
| V42 | Recuperación de contraseña: hash del token, vencimiento y uso único |
| V43 | Roles ADMINISTRADOR y VISUALIZADOR |

### Agregado

- Runbook del despliegue de 0.4.0 en OPERACION.md (diagnóstico, respaldo, ensayo de V34–V41 sobre una
  copia, despliegue, verificación y vuelta atrás) y `docs/consultas-verificacion.sql`, solo lectura.
- `docs/sustentacion/`: resumen ejecutivo y guía técnica con arquitectura, modelo de datos, flujo,
  anonimato, análisis de comentarios, seguridad, usuarios de demostración y guion por rol.
- Primer usuario en producción: con `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE` y `_CONTRASENA` (opcional) se
  crea una sola vez el primer ADMINISTRADOR si no hay ninguno activo, con contraseña temporal y sin
  nada sensible en el log (`docs/OPERACION.md`).
- Recuperar la contraseña por correo: respuesta igual exista o no la cuenta, enlace de un solo uso que
  vence en 30 minutos, solo el hash en la base y límites por correo e IP (V42).
- Roles ADMINISTRADOR y VISUALIZADOR (V43), con la matriz de autorización generada desde el código en
  `ROLES.md`.
- Nombre del periodo calculado desde la fecha de inicio (AAAA-1 o AAAA-2, mes de corte configurable y
  por confirmar); solo la administración lo ajusta, con formato y sin repetir.
- Firma del acta subiendo un PNG: se valida por su cabecera, hasta 300 KB y 2000 px, sin SVG, y se guarda
  recodificado sin metadatos. `docs/FIRMA-ACTAS.md` explica qué es y qué no es esta firma.
- Presentación pública en `/`, docentes destacados con su clasificación y enlace al expediente, lectura
  de las estadísticas en una frase y paginación en las listas largas.
- Publicación bajo una subruta (`VITE_BASE_PATH`), probada detrás de un proxy en `/Evadocente-app/`.
- Con el perfil `prod`, la API no arranca si `DOCENTIA_APP_URL` está vacía o en localhost.
- `respaldar.sh` y `restaurar.sh` verifican el respaldo; restaurar ya no borra la base con un archivo vacío.
- Con el perfil `prod` y sin `DOCENTIA_GOOGLE_CLIENT_ID`, la API avisa al arrancar que los estudiantes
  no podrán iniciar sesión. OPERACION.md explica la configuración de Google y qué pasa si falta.
- Docentes ordenados por puntaje, de mayor a menor; quienes no tienen promedio completo van al final.
- Coevaluaciones con búsqueda y filtros por programa y estado; la enviada se muestra como «Realizada».
- Estado general del proceso por docente, con línea de pasos y trazabilidad para coordinación,
  vicerrectoría y el propio docente.
- Confirmación o declinación de la cita por el docente; declinar libera la franja.
- Registro de asistencia por la coordinación, que habilita al docente su resultado y el acta.
- Acta de compromiso y acta de retroalimentación, con el tipo en el PDF.
- Agenda como calendario semanal: se arrastra para abrir franjas y cada reserva muestra su estado.
- Columna y filtro de proceso en Docentes, y cobertura de estudiantes que respondieron.
- Firma dibujada (o escrita) del acta en un modal; el trazo va en el PDF con su hash (V37).
- Vista de Estadísticas con filtros y barras que actualizan todas las gráficas.
- Panorama del análisis con IA: etapas, fortalezas y oportunidades, citas verificadas y filtrados.
- Ver información de usuarios, facultades y programas; acciones con ícono de ver, editar y eliminar.
- Descarga de Estadísticas en Excel (.xlsx): resumen con fórmulas que se recalculan al filtrar la
  hoja Docentes, y hojas por desempeño, proceso, programa, distribución y evaluadores.
- Certificado de firma electrónica en el PDF del acta, con el logo institucional y los datos del
  docente. El archivo del logo lo pone la institución en el servidor; no está en el repositorio.
- Reserva de cita desde un calendario de días disponibles, con modal para confirmar o cambiar.
- Modal para agregar compromisos, con sugerencias y plazos rápidos.
- La coordinación cancela o cambia de horario una cita desde la agenda; el docente recibe el
  aviso y vuelve a confirmar (V40).
- Mi perfil (`GET /sesiones/actual/perfil`): datos propios con su programa por nombre; el
  cambio de contraseña voluntario es un modal con DocenteIA. El menú muestra el ícono de
  configuración en lugar de la llave.
- DocenteIA acompaña el análisis de comentarios: guía de tres pasos, resumen de lo encontrado y
  mensajes para cada estado de la solicitud.
- Avisos como notificaciones flotantes y confirmaciones en modal en lugar de `window.confirm`.
- Modelo local por reglas para analizar comentarios: léxico versionado, negaciones, intensificadores,
  ironía, jerga y descriptores D02 a D12 por criterio. Es el proveedor por defecto y no usa red.
- Análisis de los comentarios del formulario anónimo, solo con la recolección cerrada.
- Resultado estructurado versionado: sentimiento, temas, fortalezas, oportunidades, tendencias contra
  el periodo anterior, criterios y advertencias.
- Endpoints de análisis por docente, comentarios anonimizados y resumen agregado por programa.
- Validación con 90 comentarios sintéticos etiquetados y métricas por criterio.
- Instrucción `analisis-v2` contra inyección y prueba opcional con un proveedor real.

### Cambiado

- Documentación alineada con el código: puertos (con Mailpit y los puertos reservados de Windows),
  roles existentes, migraciones (V1–V28 y V34–V41), umbral de compromisos como decisión pendiente,
  mínimos de 5 y 10, y limitaciones que ya no aplicaban.
- SEGURIDAD.md documenta los roles, las rutas sin sesión, la contraseña temporal y qué va al log;
  `MatrizAutorizacionTest` fija la lista de rutas públicas, rechaza cualquier ruta nueva bajo
  `/sesiones/` sin sesión y prueba la contraseña temporal en todas las rutas.
- El login ya no muestra el panel de usuarios de prueba; se escribe el correo de la cuenta.
- El docente ve su nota, compromisos, hallazgos y acta del periodo en la reunión, no antes.
- El acta se firma después de registrar la asistencia.
- Paleta con acentos turquesa, violeta, coral, cielo y rosa para estados y calendario.
- Calendario compacto con vista Día y Semana; las flechas mueven un día o una semana.
- /inicio es la ruta principal de todos los roles.
- Cuestionarios de docente y estudiante por pasos, con avance, transición y revisión final.
- Agenda con la semana o el día visible rotulado, tira de días, Ir a hoy, calendario del mes y
  línea de la hora actual.
- Nombres de personas, programas, facultades y cursos con mayúscula inicial y tildes en las
  palabras académicas (V38, V39).
- Listas de Estadísticas y Usuarios en tandas con «Mostrar más» y tarjetas compactas en el celular.
- El filtro de calidad (vacíos, cortos, duplicados, órdenes al modelo) también se aplica al CSV.
- Los comentarios se envían ordenados por la huella del texto, no por orden de carga.
- `temperature` es opcional en el proveedor chat-completions.
- Ahorro de tokens con proveedor externo: medición en `uso_ia`, muestra estratificada de hasta 150
  comentarios recortados a 300 caracteres, caché por huella, citas por id, instrucción `analisis-v3`
  compacta, presupuesto por día y periodo, regeneración con motivo y enfriamiento, y endpoint de uso.
  Con 1.000 comentarios la llamada pasa de 81.840 a 10.307 tokens de entrada.
- Salida máxima del proveedor en 4.000 tokens: con 1.500 la respuesta salía cortada.
- Cada solicitud de análisis elige `modelo: LOCAL` o `LLM`; `GET /analisis-comentarios/modelos` dice
  qué hay disponible y cuánto consume el modelo de lenguaje (V41).

- La interfaz dice *evaluadores* donde antes decía *fuentes*, y *programas* donde decía *facultades*;
  los campos de la API no cambian.
- Carga más liviana: cada pantalla se descarga al abrirla (171 a 111 KB comprimido al entrar), el JSON
  de la API viaja comprimido y las relaciones se cargan en lotes.
- El PDF del acta dice "Firma registrada": puede ser dibujada, escrita o un PNG subido.

### Retirado

- Carga de evaluaciones por CSV: la API `/importaciones` y su pantalla. Las tablas y los datos se
  conservan; la nota de estudiantes llega por el formulario anónimo.
- Pantallas de matrícula y cobertura, auditoría y facultades. El backend de matrícula sigue (inscribe a
  los estudiantes en sus cursos) y el registro de auditoría también.
- Rutas abiertas bajo `/sesiones/**`: solo son públicas `POST /sesiones`, `POST /sesiones/google` y las
  dos de recuperación de contraseña.

### Corregido

- V36 ya no falla sobre una base con actas firmadas o anuladas (callbacks de Flyway; ensayado sobre un
  respaldo de v0.3.3).
- El análisis de comentarios desde la pantalla lee el formulario anónimo de estudiantes, la única fuente
  en producción; antes partía de comentarios importados por CSV. El panorama cuenta esos comentarios.
- Sin SMTP configurado, recuperar contraseña no promete un correo: pide solicitar la clave a la
  Vicerrectoría (`GET /sesiones/recuperacion/disponibilidad`).
- Un corte de red o una recarga rápida al validar la sesión ya no la cierran; solo un 401 lo hace.

- Las pruebas E2E siguen la interfaz nueva (expediente, acta con firma dibujada, asistencia a la cita,
  cuestionarios por pasos) y abren las etapas para el día en que corren: pasan las 11.
- Un error inesperado al procesar un análisis ya no vuelca su mensaje al log: se registran el tipo
  y el lugar, porque el mensaje de un error de base de datos puede repetir texto de comentarios.
- Pedir otro análisis con `POST /analisis-ia/solicitudes` cuando el docente ya tiene uno completado
  ya no evita el enfriamiento de 60 minutos ni el motivo: se trata como una regeneración. Tras un
  error o evidencia insuficiente se puede reintentar sin esperar. La pantalla pide el motivo.
- Activar un periodo sin matrículas deja a los estudiantes sin profesores; ahora pide una
  confirmación explícita (`?confirmar=true`) y la pantalla de Periodos la solicita.
- No se puede crear ni renombrar un periodo con el mismo semestre en otro formato (`2026-02` y
  `2026-2`).
- La auditoría registra la activación y desactivación de periodos y los envíos de heteroevaluación
  rechazados; las acciones de envío de formulario, heteroevaluación y matrícula ya tienen etiqueta.
- Tras salir e iniciar con otro rol ya no se abre el panel del rol anterior.
- Las páginas ya no se desbordan en el celular; las tablas se apilan como tarjetas.

## 0.3.3

### Agregado

- Heteroevaluación por formulario: 11 ítems, anónima por diseño y con k-anonimato de 5 respuestas.
- Matrícula por CSV con plantilla, cobertura por docente y programa, y cobertura en el panel I11.
- Avisos al estudiante y de cobertura baja, auditoría del envío y tope de solicitudes.

### Cambiado

- Las respuestas del estudiante dejan de guardarse junto a su usuario (V28).
- La carga CSV de estudiantes vuelve como respaldo y no pisa la nota que ya viene del formulario.

## 0.3.2

### Agregado

- Catálogo del Anexo 1 de la Resolución 074: 6 criterios y 14 descriptores.
- Formularios de autoevaluación (14 ítems) y coevaluación (6 ítems), con borrador y envío irreversible.
- La media de cada formulario alimenta el promedio ponderado y la categoría.
- Avance de coevaluaciones para coordinación y conteos por programa para Vicerrectoría.
- Recordatorios de formularios pendientes, aviso de autoevaluación enviada y auditoría del envío.

### Cambiado

- Las pantallas de calificar por aspectos dan paso a los formularios.
- Mi evaluación muestra solo la autoevaluación propia por criterio.

## 0.3.1

### Agregado

- Notificaciones en la app y por correo con outbox, reintentos, recordatorios y preferencias.
- Auditoría inmutable con vista para Vicerrectoría.
- CI con pruebas, cobertura y escaneo de secretos.
- Health checks, cierre ordenado y scripts de respaldo y restauración.
- Matriz de autorización ejecutable y pruebas de humo en navegador.

### Cambiado

- Los errores usan RFC 7807 y llevan `correlationId`.
- El login limita intentos y responde igual exista o no el usuario.
- JWT con algoritmo explícito, y `prod` rechaza secretos de desarrollo.
- Cabeceras de seguridad en la API y en nginx.
- Bloqueo optimista en actas, hallazgos, citas y promedios.
- Índices en llaves foráneas y tope de 1000 filas en los listados.

### Corregido

- N+1 en `GET /docentes/seguimiento`.
- La publicación de hallazgos ya no se puede repetir.
- Los logs ya no incluyen el mensaje de la base, que podía traer datos.

- El seed ya no genera notificaciones ni correos, y las etapas que llevan días abiertas no se avisan.
- La cola de correo no se atrasa por zona horaria y las fechas de los contenedores salen en hora de
  Colombia.
- La campana se actualiza al leer notificaciones en la bandeja.

### Seguridad

- Tomcat 10.1.59, Jackson 2.21.7, PostgreSQL JDBC 42.7.12 y Guava 33.7.2.
