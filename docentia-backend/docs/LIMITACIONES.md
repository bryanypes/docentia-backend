# Limitaciones

Lo que el sistema no hace hoy y por qué.

| Qué no hace | Por qué |
|-------------|---------|
| Formulario de estudiantes | No hay rol de estudiante ni matrículas; la heteroevaluación llega por CSV |
| Escala de 1 a 5 confirmada | La resolución no la define; queda pendiente de Vicerrectoría |
| Reabrir un formulario enviado | Queda fijo al enviarse |
| HU-09, evidencias complementarias | Quedó fuera por el orden de corte |
| Etiquetas de los seis criterios en los hallazgos | Quedó fuera por el orden de corte |
| Integración con la API de Campus/NOVA | No se usa en este alcance; el intercambio es por CSV |
| SSO institucional | Solo correo y contraseña, y Google opcional |
| Proveedor de IA real probado | El análisis corre con el modelo simulado; el cliente externo solo tiene pruebas contra un servidor simulado |
| Notificaciones push móviles o en tiempo real | La campana consulta cada 60 segundos y no hay websockets |
| Cancelar una cita sin tomar otra | No hay endpoint; solo se cambia reservando otra franja |
| Exportar datos a CSV | No se implementó |
| Varias instancias con límite de login compartido | El contador vive en memoria |
| Revocar un token antes de que venza | La sesión dura 8 horas |
| Paginación en base de datos de todos los listados | Los listados pequeños devuelven hasta 1000 filas; notificaciones y auditoría sí paginan |
