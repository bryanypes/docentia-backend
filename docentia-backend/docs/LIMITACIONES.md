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
| Comentarios del estudiante en el análisis de IA | Recortados para no romper el anonimato; ver DECISIONES.md |
| Grupos focales y evidencias (HU-09) | Fuera de alcance |
| Escala de 1 a 5 confirmada | Pendiente de Vicerrectoría |
| Proveedor de IA real probado | El análisis corre con el modelo simulado; el cliente externo solo tiene pruebas contra un servidor simulado |
| Notificaciones push móviles o en tiempo real | La campana consulta cada 60 segundos y no hay websockets |
| Cancelar una cita sin tomar otra | No hay endpoint; solo se cambia reservando otra franja |
| Exportar datos a CSV | No se implementó |
| Varias instancias con límite de login compartido | El contador vive en memoria |
| Revocar un token antes de que venza | La sesión dura 8 horas |
| Paginación en base de datos de todos los listados | Los listados pequeños devuelven hasta 1000 filas; notificaciones y auditoría sí paginan |
