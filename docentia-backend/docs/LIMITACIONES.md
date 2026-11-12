# Limitaciones

Lo que el sistema no hace hoy y por qué.

| Qué no hace | Por qué |
|-------------|---------|
| Sync en vivo con Campus/NOVA | El JSON de personas y clases es el contrato; el seed lo simula. No hay cliente HTTP a Campus |
| Escala de 1 a 5 confirmada | La resolución no la define; queda pendiente de Vicerrectoría |
| Reabrir un formulario enviado | Queda fijo al enviarse |
| HU-09, evidencias complementarias | Quedó fuera por el orden de corte |
| Etiquetas de los seis criterios en los hallazgos | Quedó fuera por el orden de corte |
| SSO institucional real | El estudiante entra con Google institucional o, en desarrollo, con la contraseña de demo; sin SSO real, su acceso en producción depende de Google |
| Matrícula real de Campus/NOVA | Se carga por CSV o con el seed |
| Grupos focales y evidencias (HU-09) | Fuera de alcance |
| Escala de 1 a 5 confirmada | Pendiente de Vicerrectoría |
| Métricas del análisis con comentarios reales | La validación usa 90 comentarios sintéticos escritos por el mismo autor del léxico; las cifras sobreestiman. Hace falta una muestra real etiquetada |
| Sarcasmo, dobles sentidos y jerga nueva | El modelo local solo reconoce lo que está en el léxico; lo demás queda NEUTRO o mal clasificado |
| Comentarios en otro idioma | El léxico es solo en español |
| Proveedor externo en CI | La prueba real es opcional y se corrió una vez en local con datos sintéticos; CI usa servidores simulados |
| Ver los comentarios excluidos del formulario | De la fuente FORMULARIO solo queda el conteo por motivo, para no guardar nada que identifique la respuesta |
| Pantallas del análisis de comentarios | Este cambio entrega API y contrato; la interfaz va aparte |
| Notificaciones push móviles o en tiempo real | La campana consulta cada 60 segundos y no hay websockets |
| Cancelar una cita sin tomar otra | No hay endpoint; solo se cambia reservando otra franja |
| Exportar datos a CSV | No se implementó |
| Varias instancias con límite de login compartido | El contador vive en memoria |
| Revocar un token antes de que venza | La sesión dura 8 horas |
| Paginación en base de datos de todos los listados | Los listados pequeños devuelven hasta 1000 filas; notificaciones y auditoría sí paginan |
