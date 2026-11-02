# Cambios

## Sin publicar

### Agregado

- Estado general del proceso por docente, con línea de pasos y trazabilidad para coordinación,
  vicerrectoría y el propio docente.
- Confirmación o declinación de la cita por el docente; declinar libera la franja.
- Registro de asistencia por la coordinación, que habilita al docente su resultado y el acta.
- Acta de compromiso y acta de retroalimentación, con el tipo en el PDF.
- Agenda como calendario semanal: se arrastra para abrir franjas y cada reserva muestra su estado.
- Columna y filtro de proceso en Docentes, y cobertura de estudiantes que respondieron.

### Cambiado

- El docente ve su nota, compromisos, hallazgos y acta del periodo en la reunión, no antes.
- El acta se firma después de registrar la asistencia.
- Paleta con acentos turquesa, violeta, coral, cielo y rosa para estados y calendario.

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
