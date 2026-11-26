# Roles y permisos

Generado desde el código con `scripts/generar-roles.py`: el menú sale de `Sidebar.jsx` y la API de
`MatrizAutorizacionTest`, que falla si una ruta del backend no está clasificada o si un rol recibe algo
distinto de lo que dice esta tabla. Para cambiar un permiso hay que cambiar el `@PreAuthorize` y la
matriz de la prueba, y volver a generar este archivo.

Retirado en 0.4.0: la carga de evaluaciones por CSV (la API `/importaciones` ya no existe) y las
pantallas de matrícula, auditoría y facultades. Las API `/auditoria` y `/facultades` siguen sin pantalla.

## Menú por rol

| Rol | Entradas del menú |
| --- | --- |
| Estudiante | Mis evaluaciones |
| Docente | Mi evaluación, Mi autoevaluación, Mi cita, Mi acta, Solicitudes de corrección |
| Coordinación | Inicio, Estadísticas, Docentes, Coevaluaciones, Análisis de comentarios, Agenda de citas, Acciones sugeridas, Panel por programa, Solicitudes de corrección; Mi evaluación como docente: Mi evaluación, Mi autoevaluación, Mi cita con vicerrectoría, Mi acta |
| Vicerrectoría | Inicio, Estadísticas, Docentes, Análisis de comentarios, Acciones sugeridas, Panel por programa, Periodos, Solicitudes de corrección, Actas firmadas; Evaluación de coordinadores: Coevaluaciones, Agenda de citas |
| Administración | Usuarios, Programas, Ponderaciones, Periodos, Solicitudes de corrección, Actas firmadas, Citas programadas |
| Visualización | Panel por programa |

Todos los roles con sesión tienen además Notificaciones y Mi perfil.

## API por rol (131 rutas)

Las rutas van bajo `/api`. Una ruta sin sesión responde 401; con un rol no permitido, 403. Un
recurso que existe pero queda fuera del alcance del usuario responde 404, igual que uno que no existe.

| Ruta | Quién la usa |
| --- | --- |
| `GET /acciones-sugeridas` | Coordinación, Vicerrectoría |
| `POST /actas` | Coordinación, Vicerrectoría |
| `GET /actas` | Docente, Coordinación, Vicerrectoría |
| `GET /actas/{}` | Docente, Coordinación, Vicerrectoría |
| `GET /actas/codigo/{}` | Vicerrectoría, Administración |
| `GET /actas/registro` | Vicerrectoría, Administración |
| `POST /actas/{}/compromisos` | Coordinación, Vicerrectoría |
| `DELETE /actas/{}/compromisos/{}` | Coordinación, Vicerrectoría |
| `PUT /actas/{}/observaciones` | Coordinación, Vicerrectoría |
| `POST /actas/{}/firmas` | Docente, Coordinación, Vicerrectoría |
| `POST /actas/{}/correcciones` | Vicerrectoría |
| `GET /actas/{}/pdf` | Docente, Coordinación, Vicerrectoría |
| `POST /analisis-ia/solicitudes` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/solicitudes` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/solicitudes/{}` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/solicitudes/{}/comentarios-filtrados` | Coordinación, Vicerrectoría |
| `POST /analisis-ia/solicitudes/{}/publicacion` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/resultados/propio` | Docente, Coordinación |
| `GET /analisis-ia/panorama` | Coordinación, Vicerrectoría |
| `POST /analisis-ia/hallazgos/{}/validacion` | Coordinación, Vicerrectoría |
| `POST /analisis-ia/hallazgos/{}/descarte` | Coordinación, Vicerrectoría |
| `PUT /analisis-ia/hallazgos/{}` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/hallazgos/{}/versiones` | Coordinación, Vicerrectoría |
| `GET /analisis-comentarios/docentes/{}` | Coordinación, Vicerrectoría |
| `GET /analisis-comentarios/resumen` | Coordinación, Vicerrectoría |
| `POST /analisis-comentarios/docentes/{}/regeneracion` | Coordinación, Vicerrectoría |
| `GET /analisis-comentarios/uso` | Vicerrectoría, Administración |
| `GET /analisis-comentarios/modelos` | Coordinación, Vicerrectoría |
| `GET /comentarios-estudiantes/docentes/{}` | Coordinación, Vicerrectoría |
| `GET /auditoria` | Vicerrectoría |
| `GET /autoevaluacion` | Docente, Coordinación |
| `PUT /autoevaluacion` | Docente, Coordinación |
| `GET /docentes/{}/calificaciones/coordinacion` | Coordinación, Vicerrectoría |
| `PUT /docentes/{}/calificaciones/coordinacion` | Coordinación, Vicerrectoría |
| `GET /docentes/{}/calificaciones/correcciones` | Coordinación, Vicerrectoría |
| `GET /formularios/autoevaluacion` | Docente, Coordinación |
| `PUT /formularios/autoevaluacion` | Docente, Coordinación |
| `POST /formularios/autoevaluacion/enviar` | Docente, Coordinación |
| `GET /formularios/coevaluacion/pendientes` | Coordinación, Vicerrectoría |
| `GET /formularios/coevaluacion/{}` | Coordinación, Vicerrectoría |
| `PUT /formularios/coevaluacion/{}` | Coordinación, Vicerrectoría |
| `POST /formularios/coevaluacion/{}/enviar` | Coordinación, Vicerrectoría |
| `GET /estudiante/evaluaciones` | Estudiante |
| `GET /estudiante/evaluaciones/{}` | Estudiante |
| `POST /estudiante/evaluaciones/{}/enviar` | Estudiante |
| `GET /cobertura-heteroevaluacion` | Coordinación, Vicerrectoría |
| `POST /matriculas/csv` | Coordinación, Vicerrectoría |
| `GET /matriculas/plantilla` | Coordinación, Vicerrectoría |
| `GET /franjas/disponibles` | Docente, Coordinación |
| `POST /franjas/{}/reserva` | Docente, Coordinación |
| `GET /citas/mia` | Docente, Coordinación |
| `POST /citas/mia/confirmacion` | Docente, Coordinación |
| `POST /citas/{}/asistencia` | Coordinación, Vicerrectoría |
| `POST /citas/{}/cancelacion` | Coordinación, Vicerrectoría |
| `POST /citas/{}/reprogramacion` | Coordinación, Vicerrectoría |
| `GET /procesos-evaluacion` | Coordinación, Vicerrectoría |
| `GET /procesos-evaluacion/propio` | Docente, Coordinación |
| `GET /procesos-evaluacion/{}` | Coordinación, Vicerrectoría |
| `PATCH /actas/{}/tipo` | Coordinación, Vicerrectoría |
| `GET /configuracion/evaluacion` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /docentes` | Coordinación, Vicerrectoría |
| `GET /docentes/seguimiento` | Coordinación, Vicerrectoría |
| `GET /docentes/{}` | Coordinación, Vicerrectoría |
| `GET /docentes/{}/plan-seguimiento` | Coordinación, Vicerrectoría |
| `GET /expedientes/propio` | Docente, Coordinación |
| `GET /expedientes/propio/comparativo` | Docente, Coordinación |
| `GET /expedientes/reporte/pdf` | Coordinación, Vicerrectoría |
| `GET /expedientes/{}` | Coordinación, Vicerrectoría |
| `GET /expedientes/{}/comparativo` | Coordinación, Vicerrectoría |
| `GET /facultades` | Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /facultades/{}` | Coordinación, Vicerrectoría, Administración, Visualización |
| `POST /facultades` | Administración |
| `PUT /facultades/{}` | Administración |
| `DELETE /facultades/{}` | Administración |
| `GET /franjas` | Coordinación, Vicerrectoría |
| `POST /franjas` | Coordinación, Vicerrectoría |
| `POST /franjas/lote` | Coordinación, Vicerrectoría |
| `PATCH /franjas/{}/cierre` | Coordinación, Vicerrectoría |
| `DELETE /franjas/{}` | Coordinación, Vicerrectoría |
| `GET /kpis` | Coordinación, Vicerrectoría, Visualización |
| `GET /kpis/panel` | Coordinación, Vicerrectoría, Visualización |
| `GET /notificaciones` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /notificaciones/no-leidas/conteo` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PATCH /notificaciones/{}/leida` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PATCH /notificaciones/leer-todas` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /notificaciones/preferencias` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PUT /notificaciones/preferencias` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /periodos-academicos` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /periodos-academicos/activo` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /periodos-academicos/activo/calendario` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /periodos-academicos/activo/ventanas` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /periodos-academicos/{}` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `POST /periodos-academicos` | Vicerrectoría, Administración |
| `PUT /periodos-academicos/{}` | Vicerrectoría, Administración |
| `PUT /periodos-academicos/{}/activacion` | Vicerrectoría, Administración |
| `DELETE /periodos-academicos/{}/activacion` | Vicerrectoría, Administración |
| `GET /periodos-academicos/activo/ponderaciones` | Coordinación, Vicerrectoría, Administración |
| `PUT /periodos-academicos/activo/ponderaciones` | Administración |
| `GET /plan-seguimiento/propio` | Docente, Coordinación |
| `PUT /compromisos/{}/cumplimiento` | Coordinación |
| `GET /programas` | Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /programas/{}` | Coordinación, Vicerrectoría, Administración, Visualización |
| `POST /programas` | Administración |
| `PUT /programas/{}` | Administración |
| `DELETE /programas/{}` | Administración |
| `POST /sesiones` | Público (sin sesión) |
| `POST /sesiones/google` | Público (sin sesión) |
| `POST /sesiones/recuperacion` | Público (sin sesión) |
| `POST /sesiones/recuperacion/restablecimiento` | Público (sin sesión) |
| `GET /sesiones/recuperacion/disponibilidad` | Público (sin sesión) |
| `GET /sesiones/actual` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /sesiones/actual/perfil` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PUT /sesiones/actual/contrasena` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /sesiones/actual/tutorial` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PUT /sesiones/actual/tutorial` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `DELETE /sesiones/actual/tutorial` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /usuarios` | Vicerrectoría, Administración |
| `GET /usuarios/{}` | Vicerrectoría, Administración |
| `POST /usuarios` | Administración |
| `PUT /usuarios/{}` | Administración |
| `PATCH /usuarios/{}/estado` | Administración |
| `GET /verificacion-actas/{}` | Público (sin sesión) |
| `POST /solicitudes-reapertura` | Docente, Coordinación, Vicerrectoría |
| `GET /solicitudes-reapertura/mias` | Docente, Coordinación, Vicerrectoría |
| `GET /solicitudes-reapertura/por-verificar` | Coordinación, Vicerrectoría |
| `POST /solicitudes-reapertura/{}/verificacion` | Coordinación, Vicerrectoría |
| `GET /solicitudes-reapertura` | Administración |
| `POST /solicitudes-reapertura/{}/atencion` | Administración |
| `GET /solicitudes-reapertura/formularios` | Administración |
| `GET /citas/programadas` | Administración |
| `POST /solicitudes-reapertura/formularios/{}/reapertura` | Administración |
