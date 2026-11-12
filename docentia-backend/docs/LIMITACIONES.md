# Limitaciones

Lo que el sistema no hace hoy y por qué.

| Qué no hace | Por qué |
|-------------|---------|
| Sync en vivo con Campus/NOVA | El JSON de personas y clases es el contrato; el seed lo simula. No hay cliente HTTP a Campus |
| Escala de 1 a 5 confirmada | La resolución no la define; se aplica 1 a 5 y queda pendiente de Vicerrectoría |
| Reabrir un formulario enviado | Queda fijo al enviarse |
| HU-09, evidencias complementarias y grupos focales | Fuera de alcance por el orden de corte |
| Etiquetas de los seis criterios en los hallazgos | Quedó fuera por el orden de corte |
| SSO institucional real | El estudiante entra con Google institucional o, en desarrollo, con la contraseña de demo; sin SSO real, su acceso en producción depende de Google |
| Matrícula real de Campus/NOVA | Se carga por CSV o con el seed |
| Métricas del análisis con comentarios reales | La validación usa 90 comentarios sintéticos escritos por el mismo autor del léxico; las cifras sobreestiman. Hace falta una muestra real etiquetada |
| Sarcasmo, dobles sentidos y jerga nueva | El modelo local solo reconoce lo que está en el léxico; lo demás queda NEUTRO o mal clasificado |
| Comentarios en otro idioma | El léxico es solo en español |
| Proveedor externo en CI | La prueba real es opcional y se corrió una vez en local con datos sintéticos; CI usa servidores simulados |
| Ver los comentarios excluidos del formulario | De la fuente FORMULARIO solo queda el conteo por motivo, para no guardar nada que identifique la respuesta |
| El modelo de lenguaje no ve todos los comentarios | Ve una muestra de hasta 150, recortados a 300 caracteres; el modelo local sí lee y cuenta todos. `limitesAplicados` lo dice |
| Estimación de tokens por caracteres | Calibrada con un proveedor (2,3 caracteres por token); otro proveedor puede contar distinto. El número real lo da su respuesta |
| Costo en dinero | No se calcula sin precios configurados |
| Caché compartida entre réplicas | Vive en la base, así que sí se comparte; dos corridas idénticas exactamente a la vez pueden llamar dos veces |
| Notificaciones push móviles o en tiempo real | La campana consulta cada 60 segundos y no hay websockets |
| El docente cancela su cita | No cancela: la declina (libera la franja) o reserva otra. La coordinación sí cancela o cambia el horario |
| Exportar datos a CSV | No se implementó; Estadísticas exporta a Excel |
| Varias instancias con límite de login compartido | El contador vive en memoria |
| Revocar un token antes de que venza | La sesión dura 8 horas |
| Paginación en base de datos de todos los listados | Los listados pequeños devuelven hasta 1000 filas; notificaciones y auditoría sí paginan |
| Nota de estudiantes con menos de 5 respuestas | Por k-anonimato no se usa: sin CSV de respaldo el promedio queda PARCIAL y el acta no se firma. Política pendiente (DECISIONES P1 y P4) |
| Análisis de comentarios del formulario con 5 a 9 respuestas | El análisis exige 10 respuestas; con 5 a 9 el docente tiene nota de estudiantes pero no análisis |
| Cerrar etapas a mano | No hay botón ni tarea: cada etapa abre y cierra por las fechas que fija vicerrectoría |
