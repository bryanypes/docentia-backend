# API de la Resolución 074

Todo bajo `/api`, con el token de sesión salvo donde se indica.

| Método y ruta | Roles | Respuesta o regla |
|---------------|-------|-------------------|
| `GET/PUT /autoevaluacion` | Docente | Calificación propia; 204 si no existe |
| `GET /configuracion/evaluacion` | Todos | Umbral, rol corrector y pesos predeterminados |
| `GET /acciones-sugeridas?periodo=` | Coordinador, vicerrector | Docente, categoría y acción; 403 al docente |
| `GET /kpis/panel?periodoId=&facultadId=` | Coordinador, vicerrector | Agregados por programa y total |
| `GET /periodos-academicos/activo/calendario` | Todos | Etapa vigente, etapas y semanas |
| `POST /actas` | Coordinador | `{idDocente}`; 409 si ya hay acta vigente |
| `GET /actas?idDocente=`, `GET /actas/{id}` | Todos, con alcance | El docente no ve nota parcial |
| `POST/DELETE /actas/{id}/compromisos` | Coordinador | Solo en borrador |
| `POST /actas/{id}/firmas` | Coordinador, docente | `{confirmacion:true}`; 409 sin cita o sin promedio completo; 422 sin compromiso exigido |
| `POST /actas/{id}/correcciones` | Rol corrector | `{motivo}`; versión nueva y anterior ANULADA |
| `GET /actas/{id}/pdf` | Con alcance | PDF del acta firmada |
| `GET /verificacion-actas/{codigo}` | Público | Validez del código, sin notas |
| `GET /docentes/{id}/plan-seguimiento` | Coordinador, vicerrector | Compromisos del periodo anterior |
| `GET /plan-seguimiento/propio` | Docente | Los propios |
| `PUT /compromisos/{id}/cumplimiento` | Coordinador | `{observacion}` obligatoria |
| `POST /analisis-ia/hallazgos/{id}/descarte` | Coordinador | `{motivo}` opcional |
| `PUT /analisis-ia/hallazgos/{id}` | Coordinador | `{texto}` |
| `GET /analisis-ia/hallazgos/{id}/versiones` | Coordinador | Historial con modelo e instrucción |
| `POST /analisis-ia/solicitudes/{id}/publicacion` | Coordinador | 409 con hallazgos sin decidir o sin incluidos |
| `GET /analisis-ia/resultados/propio` | Docente | Hallazgos publicados: tipo y texto |
