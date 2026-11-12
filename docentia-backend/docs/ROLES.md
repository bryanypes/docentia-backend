# Roles y permisos

Generado desde el código: el menú sale de `Sidebar.jsx` y la API de `MatrizAutorizacionTest`, que
falla si una ruta del backend no está clasificada o si un rol recibe algo distinto de lo que dice esta
tabla. Para cambiar un permiso hay que cambiar el `@PreAuthorize`, la matriz de la prueba y este archivo.

Retirado en 0.4.0: la carga de evaluaciones por CSV (la API `/importaciones` ya no existe) y las
pantallas de matrícula, auditoría y facultades. Las API `/auditoria` y `/facultades` siguen sin pantalla.

## Menú por rol

| Rol | Entradas del menú |
| --- | --- |
| Estudiante | Mis evaluaciones |
| Docente | Mi evaluación, Mi autoevaluación, Mi cita, Mi acta |
| Coordinación | Inicio, Estadísticas, Docentes, Coevaluaciones, Análisis de comentarios, Agenda de citas, Acciones sugeridas, Panel por programa |
| Vicerrectoría | Inicio, Estadísticas, Docentes, Análisis de comentarios, Acciones sugeridas, Panel por programa, Periodos |
| Administración | Usuarios, Programas, Ponderaciones, Periodos |
| Visualización | Panel por programa |

Todos los roles con sesión tienen además Notificaciones y Mi perfil.

## API por rol (114 rutas)

Las rutas van bajo `/api`. Una ruta sin sesión responde 401; con un rol no permitido, 403.

| Ruta | Quién la usa |
| --- | --- |
| `GET /acciones-sugeridas` | Coordinación, Vicerrectoría |
| `POST /actas` | Coordinación |
| `GET /actas` | Docente, Coordinación, Vicerrectoría |
| `GET /actas/{}` | Docente, Coordinación, Vicerrectoría |
| `POST /actas/{}/compromisos` | Coordinación |
| `DELETE /actas/{}/compromisos/{}` | Coordinación |
| `POST /actas/{}/firmas` | Docente, Coordinación |
| `POST /actas/{}/correcciones` | Vicerrectoría |
| `GET /actas/{}/pdf` | Docente, Coordinación, Vicerrectoría |
| `POST /analisis-ia/solicitudes` | Coordinación |
| `GET /analisis-ia/solicitudes` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/solicitudes/{}` | Coordinación, Vicerrectoría |
| `GET /analisis-ia/solicitudes/{}/comentarios-filtrados` | Coordinación, Vicerrectoría |
| `POST /analisis-ia/solicitudes/{}/publicacion` | Coordinación |
| `GET /analisis-ia/resultados/propio` | Docente |
| `GET /analisis-ia/panorama` | Coordinación, Vicerrectoría |
| `POST /analisis-ia/hallazgos/{}/validacion` | Coordinación |
| `POST /analisis-ia/hallazgos/{}/descarte` | Coordinación |
| `PUT /analisis-ia/hallazgos/{}` | Coordinación |
| `GET /analisis-ia/hallazgos/{}/versiones` | Coordinación |
| `GET /analisis-comentarios/docentes/{}` | Coordinación, Vicerrectoría |
| `GET /analisis-comentarios/resumen` | Coordinación, Vicerrectoría |
| `POST /analisis-comentarios/docentes/{}/regeneracion` | Coordinación, Vicerrectoría |
| `GET /analisis-comentarios/uso` | Vicerrectoría, Administración |
| `GET /analisis-comentarios/modelos` | Coordinación, Vicerrectoría |
| `GET /comentarios-estudiantes/docentes/{}` | Coordinación, Vicerrectoría |
| `GET /auditoria` | Vicerrectoría |
| `GET /autoevaluacion` | Docente |
| `PUT /autoevaluacion` | Docente |
| `GET /docentes/{}/calificaciones/coordinacion` | Coordinación |
| `PUT /docentes/{}/calificaciones/coordinacion` | Coordinación |
| `GET /docentes/{}/calificaciones/correcciones` | Coordinación, Vicerrectoría |
| `GET /formularios/autoevaluacion` | Docente |
| `PUT /formularios/autoevaluacion` | Docente |
| `POST /formularios/autoevaluacion/enviar` | Docente |
| `GET /formularios/coevaluacion/pendientes` | Coordinación, Vicerrectoría |
| `GET /formularios/coevaluacion/{}` | Coordinación |
| `PUT /formularios/coevaluacion/{}` | Coordinación |
| `POST /formularios/coevaluacion/{}/enviar` | Coordinación |
| `GET /estudiante/evaluaciones` | Estudiante |
| `GET /estudiante/evaluaciones/{}` | Estudiante |
| `POST /estudiante/evaluaciones/{}/enviar` | Estudiante |
| `GET /cobertura-heteroevaluacion` | Coordinación, Vicerrectoría |
| `POST /matriculas/csv` | Coordinación, Vicerrectoría |
| `GET /matriculas/plantilla` | Coordinación, Vicerrectoría |
| `GET /franjas/disponibles` | Docente |
| `POST /franjas/{}/reserva` | Docente |
| `GET /citas/mia` | Docente |
| `POST /citas/mia/confirmacion` | Docente |
| `POST /citas/{}/asistencia` | Coordinación |
| `POST /citas/{}/cancelacion` | Coordinación |
| `POST /citas/{}/reprogramacion` | Coordinación |
| `GET /procesos-evaluacion` | Coordinación, Vicerrectoría |
| `GET /procesos-evaluacion/propio` | Docente |
| `GET /procesos-evaluacion/{}` | Coordinación, Vicerrectoría |
| `PATCH /actas/{}/tipo` | Coordinación |
| `GET /configuracion/evaluacion` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /docentes` | Coordinación, Vicerrectoría |
| `GET /docentes/seguimiento` | Coordinación, Vicerrectoría |
| `GET /docentes/{}` | Coordinación, Vicerrectoría |
| `GET /docentes/{}/plan-seguimiento` | Coordinación, Vicerrectoría |
| `GET /expedientes/propio` | Docente |
| `GET /expedientes/propio/comparativo` | Docente |
| `GET /expedientes/{}` | Coordinación, Vicerrectoría |
| `GET /expedientes/{}/comparativo` | Coordinación, Vicerrectoría |
| `GET /facultades` | Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /facultades/{}` | Coordinación, Vicerrectoría, Administración, Visualización |
| `POST /facultades` | Administración |
| `PUT /facultades/{}` | Administración |
| `DELETE /facultades/{}` | Administración |
| `GET /franjas` | Coordinación |
| `POST /franjas` | Coordinación |
| `POST /franjas/lote` | Coordinación |
| `PATCH /franjas/{}/cierre` | Coordinación |
| `DELETE /franjas/{}` | Coordinación |
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
| `GET /plan-seguimiento/propio` | Docente |
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
| `GET /sesiones/actual` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /sesiones/actual/perfil` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `PUT /sesiones/actual/contrasena` | Estudiante, Docente, Coordinación, Vicerrectoría, Administración, Visualización |
| `GET /usuarios` | Vicerrectoría, Administración |
| `GET /usuarios/{}` | Vicerrectoría, Administración |
| `POST /usuarios` | Administración |
| `PUT /usuarios/{}` | Administración |
| `PATCH /usuarios/{}/estado` | Administración |
| `GET /verificacion-actas/{}` | Público (sin sesión) |
