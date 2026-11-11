# Cambios

## Sin publicar

### Agregado

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
  hoja Docentes, y hojas por desempeño, proceso, programa o facultad, distribución y fuentes.
- Certificado de firma electrónica en el PDF del acta, con el logo institucional y los datos del
  docente.
- Reserva de cita desde un calendario de días disponibles, con modal para confirmar o cambiar.
- Modal para agregar compromisos, con sugerencias y plazos rápidos.
- La coordinación cancela o cambia de horario una cita desde la agenda; el docente recibe el
  aviso y vuelve a confirmar (V40).
- Mi perfil (`GET /sesiones/actual/perfil`): datos propios con facultad y programa por nombre; el
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

- El login ya no muestra el panel de usuarios de prueba; se escribe el correo de la cuenta.
- El docente ve su nota, compromisos, hallazgos y acta del periodo en la reunión, no antes.
- El acta se firma después de registrar la asistencia.
- Paleta con acentos turquesa, violeta, coral, cielo y rosa para estados y calendario.
- Calendario compacto con vista Día y Semana; las flechas mueven un día o una semana.
- Matrícula y cobertura con resumen, importación en tres pasos y semáforo por docente.
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

### Corregido

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
