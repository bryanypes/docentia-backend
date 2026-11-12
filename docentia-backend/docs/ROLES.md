# Roles y permisos

> **Retirado en 0.4.0:** la carga de evaluaciones por CSV (la API `/importaciones` ya no existe) y las pantallas de matrícula, auditoría y facultades. Las API `/auditoria` y `/facultades` siguen para consulta y para el alcance de la coordinación, pero no tienen pantalla.

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

## Después: ADMINISTRADOR y VISUALIZADOR

La vicerrectoría deja la operación y conserva la decisión y el control.

| Rol | Qué hace | Qué no hace |
|-----|----------|-------------|
| Vicerrectoría | Inicio institucional, ranking de docentes, estadísticas y Excel, docentes destacados, análisis, acciones sugeridas, panel por programa, auditoría, corrección de actas; decide las fechas de los periodos (puede ampliar la recolección) | Ya no crea ni edita usuarios, programas, facultades ni ponderaciones: solo los lee |
| Administración | Crea, edita, activa y desactiva usuarios (y fija claves temporales); programas y facultades; ponderaciones; ejecuta cambios de fechas de periodos; consulta el uso del proveedor de IA | No ve notas, docentes, comentarios, análisis ni actas |
| Visualización | Indicadores generales y panel por programa, de toda la universidad | Nada individual: sin docentes, notas, comentarios ni actas; sin botones de escritura |
| Coordinación | Sus docentes, coevaluación, análisis, agenda, actas y seguimiento de su facultad | Nada administrativo |

La carga de datos por CSV ya no se usa: las notas salen solo de los formularios. Sus pantallas se
quitaron de la web para todos los roles; el backend de importaciones sigue existiendo (pendiente de
retirar).

Por confirmar con Vicerrectoría: la visualización no incluye Estadísticas ni el ranking porque esas
pantallas muestran a cada docente con su nota; si se quieren, hace falta una versión agregada.

### Menú por rol (después)

| Rol | Entradas del menú |
|-----|-------------------|
| Estudiante | Mis evaluaciones |
| Docente | Mi evaluación, Mi autoevaluación, Mi cita, Mi acta |
| Coordinación | Inicio, Estadísticas, Docentes, Coevaluaciones, Análisis de comentarios, Agenda de citas, Acciones sugeridas, Panel por programa |
| Vicerrectoría | Inicio, Estadísticas, Docentes, Análisis de comentarios, Acciones sugeridas, Panel por programa, Periodos |
| Administración | Usuarios, Programas, Ponderaciones, Periodos |
| Visualización | Panel por programa |

### API por rol (después)

D = docente, C = coordinación, V = vicerrectoría, E = estudiante, A = administración, L = visualización, P = sin sesión. Comprobado por `MatrizAutorizacionTest` para cada rol (permitido y 403).

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
| `GET /configuracion/evaluacion` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /docentes` | Coordinación, Vicerrectoría |
| `GET /docentes/seguimiento` | Coordinación, Vicerrectoría |
| `GET /docentes/{}` | Coordinación, Vicerrectoría |
| `GET /docentes/{}/plan-seguimiento` | Coordinación, Vicerrectoría |
| `GET /expedientes/propio` | Docente |
| `GET /expedientes/{}` | Coordinación, Vicerrectoría |
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
| `GET /notificaciones` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /notificaciones/no-leidas/conteo` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `PATCH /notificaciones/{}/leida` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `PATCH /notificaciones/leer-todas` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /notificaciones/preferencias` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `PUT /notificaciones/preferencias` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /periodos-academicos` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /periodos-academicos/activo` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /periodos-academicos/activo/calendario` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /periodos-academicos/activo/ventanas` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /periodos-academicos/{}` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
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
| `POST /sesiones` | Sin sesión |
| `POST /sesiones/google` | Sin sesión |
| `POST /sesiones/recuperacion` | Sin sesión |
| `POST /sesiones/recuperacion/restablecimiento` | Sin sesión |
| `GET /sesiones/actual` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /sesiones/actual/perfil` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `PUT /sesiones/actual/contrasena` | Docente, Coordinación, Vicerrectoría, Estudiante, Administración, Visualización |
| `GET /usuarios` | Vicerrectoría, Administración |
| `GET /usuarios/{}` | Vicerrectoría, Administración |
| `POST /usuarios` | Administración |
| `PUT /usuarios/{}` | Administración |
| `PATCH /usuarios/{}/estado` | Administración |
| `GET /verificacion-actas/{}` | Sin sesión |
