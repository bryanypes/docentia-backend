# Cambios

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
