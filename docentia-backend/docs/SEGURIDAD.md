# Seguridad

## Modelo de amenazas

| Actor | Puede | Amenaza principal |
|-------|-------|-------------------|
| Docente | Ver lo propio | Ver notas por fuente de su evaluación o datos de un colega |
| Coordinador | Gestionar su facultad | Salir de su alcance hacia otra facultad |
| Vicerrector | Consultar toda la institución | Sobreexposición de datos individuales |
| Estudiante | Responder la heteroevaluación de sus clases | Ser identificado por lo que respondió |
| Sin sesión | Iniciar sesión y verificar un código | Fuerza bruta, enumeración de usuarios |

## Roles y rutas

Existen seis roles: `ESTUDIANTE`, `DOCENTE`, `COORDINADOR`, `VICERRECTOR`, `ADMINISTRADOR` y
`VISUALIZADOR` (V43). La tabla rol por ruta está en `ROLES.md`, generada desde `MatrizAutorizacionTest`.

| Regla | Control | Evidencia |
|-------|---------|-----------|
| Rol por endpoint | `@PreAuthorize` en cada controlador; la matriz completa (114 rutas por seis roles) vive en la prueba, que falla si aparece una ruta sin clasificar | `MatrizAutorizacionTest.todoEndpointEstaClasificado`, `cadaRolRecibeLoQueLeToca` |
| Rutas sin sesión | Solo `POST /sesiones`, `POST /sesiones/google`, `POST /sesiones/recuperacion`, `POST /sesiones/recuperacion/restablecimiento`, `GET /sesiones/recuperacion/disponibilidad` (solo dice si hay correo), `GET /verificacion-actas/{codigo}` y `GET /actuator/health`, `/health/liveness`, `/health/readiness` e `/info`. No hay comodines: cualquier otra ruta bajo `/sesiones/` pide sesión | `MatrizAutorizacionTest.lasRutasPublicasSonSoloLasDeEntradaYVerificacion`, `loQueNoEsPublicoPideSesion` |
| Contraseña temporal | La sesión no lleva el rol; solo puede leer `GET /sesiones/actual` y cambiar la clave en `PUT /sesiones/actual/contrasena` | `MatrizAutorizacionTest.conContrasenaTemporalSoloSeCambiaLaClave`, `CuentaUsuarioTest` |
| Alcance | El coordinador solo ve su facultad y el docente lo propio; fuera de alcance responde 404, igual que lo inexistente | `AlcanceRecursosTest` |

En resumen: vicerrectoría administra (periodos, ventanas, ponderaciones, facultades, programas, usuarios,
auditoría) y consulta toda la institución; coordinación califica, coevalúa, carga CSV, analiza
comentarios, abre franjas y firma actas de su facultad; el docente ve lo propio, se autoevalúa, reserva
cita y firma su acta; el estudiante solo responde la heteroevaluación de sus clases.

**Datos sensibles:** notas por fuente, promedio, categoría, comentarios de estudiantes, hallazgos y
actas. La regla A4 impide que el docente vea notas individuales de cada fuente o un promedio parcial.

## Rol estudiante

| Amenaza | Control |
|---------|---------|
| Saber quién respondió qué | Tablas separadas sin vínculo, UUID aleatorio, sin fecha en la respuesta, nada en logs ni auditoría |
| Deducir respuestas de grupos pequeños | k-anonimato: nada se usa ni se muestra con menos de 5 respuestas por docente |
| Evaluar a un docente ajeno o hacerlo dos veces | Solo asignaciones de su matrícula activa (404 en otro caso) y clave única de participación |
| Ver resultados | El estudiante solo recibe estructura y estado; 403 en todo lo demás |
| Abuso del formulario | Tope de lecturas y envíos por minuto (429) |

**Riesgo aceptado.** Un administrador de la base podría intentar correlacionar por el orden físico de las
filas en disco. La app nunca expone ese orden y las respuestas se listan por UUID.

### Riesgo residual del anonimato

El modelo separa quién respondió (`participaciones_heteroevaluacion`: asignación, estudiante y fecha) de
qué respondió (`respuestas_heteroevaluacion`: UUID aleatorio, asignación, docente, periodo y versión del
instrumento, sin estudiante, sin fecha y sin secuencia). Aun así queda un riesgo que hay que decir:

- **La respuesta guarda la asignación (la clase evaluada).** En una clase donde respondió **un solo
  estudiante**, quien tenga acceso directo a la base puede cruzar las dos tablas por `asignacion_id` y saber
  qué respondió esa persona. Con dos o tres estudiantes, el cruce reduce mucho las posibilidades.
- **La aplicación nunca lo muestra.** Ninguna pantalla ni ruta entrega respuestas por clase. La nota de
  estudiantes solo se calcula y se muestra con al menos **5 respuestas por docente**, y el análisis de
  comentarios exige al menos **10**. La cobertura solo da conteos, sin nombres.
- **Quién puede hacer el cruce:** solo quien administra el servidor o la base, no los usuarios de DOCENTIA.

Mitigaciones propuestas (no aplicadas; se deciden con la institución):

1. Guardar en la respuesta solo el docente y el periodo, no la asignación, cuando el docente tiene una sola
   clase o cuando la clase tiene pocos matriculados.
2. Retirar `asignacion_id` de las respuestas al cerrar la recolección, una vez calculadas las notas.
3. Exigir un mínimo de respuestas por clase, además del mínimo por docente, para mostrar la cobertura.
4. Restringir el acceso directo a la base a una sola cuenta de operación, con registro de consultas.

## Análisis de comentarios

| Amenaza | Control | Evidencia |
|---------|---------|-----------|
| Inyección de instrucciones en un comentario | Filtro `INSTRUCCION_SOSPECHOSA` antes del modelo; los comentarios van como datos en el mensaje del usuario; la instrucción `analisis-v2` desautoriza toda orden escrita en ellos; toda cita se verifica contra el texto | `ValidacionAnalisisTest.losIntentosDeInyeccionNoCambianElResultado`, `ModeloIAChatCompletionsTest`, `ProveedorRealOpcionalTest` |
| Datos personales hacia un proveedor externo | Proveedor externo apagado por defecto; el modelo local no usa red; antes de cualquier modelo se quitan nombres, correos, teléfonos, documentos, enlaces y el nombre del docente; clave solo en `.env` | `ValidacionAnalisisTest.ningunDatoPersonalLlegaALaSalida`, `AnonimizadorTest` |
| Ligar un comentario del formulario con quien lo envió | Lectura solo con la recolección cerrada (409 antes); copia sin fecha, secuencia ni id de respuesta; orden por huella; el texto no va a logs ni auditoría | `AnalisisComentariosTest` |
| Docente que ve comentarios o temas | 403 en los tres endpoints; política de roles en `PoliticaComentarios`; el docente solo ve hallazgos publicados (A4) | `MatrizAutorizacionTest`, `AnalisisComentariosTest.elDocenteNoVeCrudosYOtraFacultadRecibe404` |
| IDOR entre facultades | Alcance por facultad en el servicio; 404 igual que inexistente | `AnalisisComentariosTest.elDocenteNoVeCrudosYOtraFacultadRecibe404` |
| Deducir a un docente desde el resumen | El resumen no trae docentes y no se muestra con menos de 3 | `AnalisisComentariosTest.elResumenEsLaSumaYNoSeMuestraConPocosDocentes` |
| Abuso de consulta | 60 consultas por usuario y minuto (429), página máxima de 50, 2.000 comentarios por corrida | `AnalisisComentariosTest.losComentariosSePaginanFiltranYTienenTope` |
| Texto de comentarios en el registro de gasto o en logs | `uso_ia` solo guarda conteos, tokens y huella; los errores del proveedor se registran por código HTTP, sin cuerpo | `AhorroTokensTest.elUsoNoGuardaTextoNiPersonasYNadaVaAlRegistro`, `HuellaEntradaTest` |
| Gasto descontrolado del proveedor | Muestra con tope, caché por huella, presupuesto por día y periodo, enfriamiento al regenerar; las lecturas nunca llaman | `AhorroTokensTest` |
| Cita inventada por el modelo | Cita por id: el texto lo pone el sistema y un id no enviado queda sin verificar | `AhorroTokensTest.lasCitasPorIdSeResuelvenConElTextoEnviadoYUnIdInexistenteNoVale` |
| Situación grave escondida en un comentario | Se excluye del análisis y queda una advertencia con la cantidad para coordinación | `ValidacionAnalisisTest` |

## Controles

| Riesgo | Control | Evidencia |
|--------|---------|-----------|
| Acceso indebido | Autorización por método y alcance por facultad | `MatrizAutorizacionTest`, `AlcanceRecursosTest` |
| Token forjado | HS256 explícito; secreto de 32 bytes o más; `prod` rechaza secretos de desarrollo | `SeguridadHttpTest`, `ValidacionProduccion` |
| Fuerza bruta | 5 fallos por correo y 30 por IP en 10 minutos, respuesta 429 genérica | `SeguridadHttpTest` |
| Enumeración | Mismo mensaje y siempre se compara un hash BCrypt | `AutenticacionService` |
| Fuga en errores | RFC 7807 sin trazas, con `correlationId` | `SeguridadHttpTest` |
| Fuga por correo | Textos fijos por tipo | `NotificacionesTest` |
| Clickjacking y MIME | CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy` en API y nginx | `SeguridadHttpTest`, `nginx.conf` |
| Superficie operativa | Actuator expone solo `health` e `info`; no hay Swagger | `SeguridadHttpTest` |
| Secretos | Solo por entorno; gitleaks en CI | `.github/workflows/ci.yml` |
| CSV malicioso | Límite de tamaño, filas, tipo y codificación | `ImportacionTest` |

La única exportación es el Excel de Estadísticas, que arma el navegador con datos que la API ya le
entregó. Los textos se escriben como cadena (`inlineStr` en `shared/utils/xlsx.js`), así que Excel no
evalúa como fórmula un nombre que empiece por `=`; solo las celdas de resumen que arma el propio código
son fórmulas. Si se agrega una exportación a CSV, debe prefijar `'` a los valores que empiecen por
`=`, `+`, `-` o `@`.

## Registros (logs)

Ningún comentario, respuesta ni dato que identifique a un estudiante va al log.

| Qué podría filtrarse | Control | Evidencia |
|----------------------|---------|-----------|
| Comentario o respuesta del estudiante | El envío no registra nada; la respuesta no tiene vínculo con quien respondió | `HeteroevaluacionTest.laRespuestaNoTieneNadaQueLaUnaAlEstudiante` (texto, UUID y correo ausentes del log) |
| Texto de comentarios en un error del análisis | Los fallos del proveedor se registran por código HTTP; los errores inesperados del procesador, solo por tipo y lugar, sin mensaje | `AhorroTokensTest.unFalloInesperadoDelAnalisisNoVuelcaComentariosAlRegistro`, `elUsoNoGuardaTextoNiPersonasYNadaVaAlRegistro` |
| Correo del destinatario en un fallo SMTP | Solo el tipo de error | `ProcesadorColaCorreo` |
| Valores de una fila rechazada por la base | Solo la clase de la causa | `ManejadorGlobalExcepciones` |

El manejador global sí registra la traza completa de un error no controlado en una petición, para
poder diagnosticarlo con el `correlationId`; los caminos que tocan comentarios no llegan ahí con texto.

## Actas firmadas durante las migraciones

Un disparador de la base (`tr_actas_inmutables`, V20) impide modificar o borrar un acta firmada o anulada.
Algunas migraciones agregan columnas y les dan valor a las actas existentes: V36 les pone el tipo. Para eso,
tres callbacks de Flyway en `db/migration` apagan el disparador solo mientras se migra:

| Callback | Cuándo corre | Qué hace |
|---|---|---|
| `beforeMigrate.sql` | Antes de aplicar migraciones | Apaga el disparador de la tabla `actas` del esquema que se migra |
| `afterMigrate.sql` | Al terminar bien, también en cada arranque sin migraciones pendientes | Lo vuelve a encender |
| `afterMigrateError.sql` | Si una migración falla | Lo vuelve a encender aunque la API no arranque |

Fuera de `migrate` no corre ningún callback: ni la aplicación ni `info` o `validate` tocan el disparador.
Las migraciones no cambian notas, hashes, códigos ni PDF de las actas. `MigracionesActasFirmadasTest`
lo prueba sobre un PostgreSQL embebido con un acta firmada y una anulada creadas en V35:

- la migración pasa;
- después, modificarlas sigue fallando;
- con una migración que falla a la mitad, el disparador queda encendido;
- solo existen esos tres callbacks.

## Escaneos de la versión 0.4.0 (12 de noviembre de 2026)

| Escaneo | Alcance | Resultado |
|---|---|---|
| gitleaks 8.18.4 | Todo el historial de `develop` | Sin secretos |
| Semgrep 1.95 (`p/java`, `p/react`, `p/javascript`, `p/secrets`) | 731 archivos, 165 reglas | Sin hallazgos |
| `npm audit` | Frontend, dependencias de producción y de desarrollo | 0 vulnerabilidades |
| Dependencias del backend (Trivy y OSV-Scanner) | 113 dependencias de runtime | **No se pudo ejecutar**: la red del equipo de pruebas no resolvió las bases de vulnerabilidades. Queda pendiente correrlo en un equipo con acceso (`trivy fs --scanners vuln .` o activar Dependabot en GitHub) |

Lo conocido del backend sigue en "Riesgos aceptados": las dos CVE de Spring MVC (`XsltView` y SSE con
fragmentos de vista) no aplican porque el proyecto no usa esas funciones.

## Riesgos aceptados

| Riesgo | Motivo | Mitigación futura |
|--------|--------|-------------------|
| El límite de intentos vive en memoria | Una sola instancia en la demo | Redis en producción |
| CVE-2026-47884, Spring MVC `XsltView` | La corrección solo existe en 7.0.9 y exige cambiar de versión mayor. El proyecto no usa `XsltView` | Migrar a Spring Boot 4 |
| CVE-2026-47890, SSE con fragmentos de vista | Igual que el anterior. El proyecto no usa SSE | Migrar a Spring Boot 4 |
| JWT sin revocación | Vigencia de 8 horas | Lista de revocación |
| `style-src 'unsafe-inline'` en nginx | Lo requiere Google Identity | Nonce o retirar Google |

## Datos personales

La Ley 1581 de 2012 orienta la minimización y la finalidad del tratamiento. El sistema las aplica así:
guarda solo los datos de la evaluación, anonimiza comentarios antes del análisis, analiza por
defecto con un modelo local sin red, deja el proveedor externo apagado y no incluye datos de evaluación
en los correos. Activar un proveedor externo es enviar texto de estudiantes, aunque anonimizado, a un
tercero: requiere revisar la política de tratamiento de datos de la institución y el contrato con el
proveedor. Este documento describe controles
técnicos; no afirma cumplimiento legal, que debe revisar la institución.
