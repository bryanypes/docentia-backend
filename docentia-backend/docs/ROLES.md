# Roles y permisos

## Estado actual (antes de agregar ADMINISTRADOR y VISUALIZADOR)

Sacado del código de `release/0.4.0` más los cambios de esta rama hasta antes de los roles: la matriz de
`MatrizAutorizacionTest` (que falla si una ruta no está clasificada) y el menú de `Sidebar.jsx`.
Hoy no existe ningún rol de administrador: la vicerrectoría concentra la administración (usuarios,
catálogos, ponderaciones, periodos) junto con la decisión y el control.

### Menú por rol

| Rol | Entradas del menú |
|-----|-------------------|
| Estudiante | Mis evaluaciones |
| Docente | Mi evaluación, Mi autoevaluación, Mi cita, Mi acta |
| Coordinación | Inicio, Estadísticas, Docentes, Coevaluaciones, Análisis de comentarios, Agenda de citas, Acciones sugeridas, Panel por programa |
| Vicerrectoría | Inicio, Estadísticas, Docentes, Análisis de comentarios, Acciones sugeridas, Panel por programa, Periodos, Ponderaciones, Programas, Usuarios |

### API por rol

D = docente, C = coordinación, V = vicerrectoría, E = estudiante, P = sin sesión.

| Ruta | Roles |
|------|-------|
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
| `GET /analisis-comentarios/uso` | Vicerrectoría |
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
| `GET /configuracion/evaluacion` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /docentes` | Coordinación, Vicerrectoría |
| `GET /docentes/seguimiento` | Coordinación, Vicerrectoría |
| `GET /docentes/{}` | Coordinación, Vicerrectoría |
| `GET /docentes/{}/plan-seguimiento` | Coordinación, Vicerrectoría |
| `GET /expedientes/propio` | Docente |
| `GET /expedientes/{}` | Coordinación, Vicerrectoría |
| `GET /facultades` | Coordinación, Vicerrectoría |
| `GET /facultades/{}` | Coordinación, Vicerrectoría |
| `POST /facultades` | Vicerrectoría |
| `PUT /facultades/{}` | Vicerrectoría |
| `DELETE /facultades/{}` | Vicerrectoría |
| `GET /franjas` | Coordinación |
| `POST /franjas` | Coordinación |
| `POST /franjas/lote` | Coordinación |
| `PATCH /franjas/{}/cierre` | Coordinación |
| `DELETE /franjas/{}` | Coordinación |
| `POST /importaciones/csv/validar` | Coordinación |
| `POST /importaciones/csv` | Coordinación |
| `GET /importaciones` | Coordinación, Vicerrectoría |
| `GET /importaciones/plantillas` | Coordinación |
| `POST /importaciones/plantillas` | Coordinación |
| `DELETE /importaciones/plantillas/{}` | Coordinación |
| `GET /kpis` | Coordinación, Vicerrectoría |
| `GET /kpis/panel` | Coordinación, Vicerrectoría |
| `GET /notificaciones` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /notificaciones/no-leidas/conteo` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `PATCH /notificaciones/{}/leida` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `PATCH /notificaciones/leer-todas` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /notificaciones/preferencias` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `PUT /notificaciones/preferencias` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /periodos-academicos` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /periodos-academicos/activo` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /periodos-academicos/activo/calendario` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /periodos-academicos/activo/ventanas` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /periodos-academicos/{}` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `POST /periodos-academicos` | Vicerrectoría |
| `PUT /periodos-academicos/{}` | Vicerrectoría |
| `PUT /periodos-academicos/{}/activacion` | Vicerrectoría |
| `DELETE /periodos-academicos/{}/activacion` | Vicerrectoría |
| `GET /periodos-academicos/activo/ponderaciones` | Coordinación, Vicerrectoría |
| `PUT /periodos-academicos/activo/ponderaciones` | Vicerrectoría |
| `GET /plan-seguimiento/propio` | Docente |
| `PUT /compromisos/{}/cumplimiento` | Coordinación |
| `GET /programas` | Coordinación, Vicerrectoría |
| `GET /programas/{}` | Coordinación, Vicerrectoría |
| `POST /programas` | Vicerrectoría |
| `PUT /programas/{}` | Vicerrectoría |
| `DELETE /programas/{}` | Vicerrectoría |
| `POST /sesiones` | Sin sesión |
| `POST /sesiones/google` | Sin sesión |
| `POST /sesiones/recuperacion` | Sin sesión |
| `POST /sesiones/recuperacion/restablecimiento` | Sin sesión |
| `GET /sesiones/actual` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /sesiones/actual/perfil` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `PUT /sesiones/actual/contrasena` | Docente, Coordinación, Vicerrectoría, Estudiante |
| `GET /usuarios` | Vicerrectoría |
| `GET /usuarios/{}` | Vicerrectoría |
| `POST /usuarios` | Vicerrectoría |
| `PUT /usuarios/{}` | Vicerrectoría |
| `PATCH /usuarios/{}/estado` | Vicerrectoría |
| `GET /verificacion-actas/{}` | Sin sesión |
