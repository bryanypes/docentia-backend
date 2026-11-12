# Pendientes de la entrega 0.4.0

Estado al miércoles 11 de noviembre de 2026. La recolección de 2026-2 sigue abierta hasta el viernes 13.
Hoy solo se preparó: no se reescribió historial, no se tocó `main`, no se crearon etiquetas y no se
entró al servidor.

## Lo hecho

Repositorio `bryanypes/docentia-backend` (monorepo), todo fusionado a `develop` con CI verde:

| PR | Tarea | Qué cambió |
|----|-------|------------|
| [#13](https://github.com/bryanypes/docentia-backend/pull/13) | 1a | `POST /analisis-ia/solicitudes` ya no evita el enfriamiento: con un análisis COMPLETADO, pedir otro exige motivo (400) y una hora de espera (429), igual que `/regeneracion`. Tras ERROR o INSUFICIENTE se reintenta sin esperar. La pantalla pide el motivo |
| [#14](https://github.com/bryanypes/docentia-backend/pull/14) | 1b, 1c | Lista cerrada de rutas públicas; nada bajo `/sesiones/` queda abierto; contraseña temporal probada en las 116 rutas y los 4 roles. Un error inesperado del análisis ya no vuelca su mensaje al log. Pruebas de que ni comentarios ni correos de estudiantes llegan al log. SEGURIDAD.md al día |
| [#15](https://github.com/bryanypes/docentia-backend/pull/15) | 2 | Primer vicerrector en producción con `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE` y `_CONTRASENA`: una sola vez, solo con la tabla vacía, clave temporal, candado de PostgreSQL y nada en el log |
| [#16](https://github.com/bryanypes/docentia-backend/pull/16) | 3 | Aviso al arrancar en `prod` sin `DOCENTIA_GOOGLE_CLIENT_ID`; OPERACION.md explica Google (cliente, orígenes, las dos variables, reglas del token y qué pasa si falta) |
| [#17](https://github.com/bryanypes/docentia-backend/pull/17) | 4 | README, OPERACION, DECISIONES, LIMITACIONES, ARQUITECTURA y README del backend alineados con el código: puertos, roles, V1–V28 y V34–V41, umbral 3,8 frente a 4,00 como pendiente, mínimos 5 y 10, datos reales de la semilla |
| [#18](https://github.com/bryanypes/docentia-backend/pull/18) | 7a | E2E con la interfaz nueva: 11 de 11, tres corridas sobre semilla limpia (la última sobre `release/0.4.0`) |
| [#19](https://github.com/bryanypes/docentia-backend/pull/19) | 5 | `docentia-backend/docs/sustentacion/`: RESUMEN_EJECUTIVO.md y GUIA_TECNICA.md |
| [#20](https://github.com/bryanypes/docentia-backend/pull/20) | 6a, 6b | Runbook del despliegue del sábado en OPERACION.md y `docentia-backend/docs/consultas-verificacion.sql` (solo lectura) |

Repositorio `vXro1/DocenteIA`:

| PR | Tarea | Qué cambió |
|----|-------|------------|
| [#3](https://github.com/vXro1/DocenteIA/pull/3) | 7b | `develop` igualado con `DocenteIA/` del monorepo (127 archivos; conserva su `.github` y `.gitleaks.toml`) |

Rama `release/0.4.0` (tarea 8), **sin PR, sin etiqueta y sin fusionar**: versión 0.4.0 en `pom.xml` y
`DocenteIA/package.json`, CHANGELOG con resumen y tabla de migraciones V34–V41.

**Verificación final** (tarea 9), sobre `release/0.4.0`:

| Qué | Resultado |
|-----|-----------|
| Backend `./mvnw verify` (Linux, como el CI) | 1233 pruebas, 0 fallos, 5 omitidas (las opcionales de siempre); `docentia-0.4.0.jar` |
| Frontend | `oxlint` sin avisos, 121 de 121 pruebas, `vite build` correcto, `npm audit` sin vulnerabilidades |
| E2E con `docker compose` desde cero | 11 de 11; migra a V41 y arranca |
| `docker compose build --no-cache` y `up` con `.env.example` (tarea 6c) | Los cuatro servicios `healthy` (solo se cambiaron los puertos: el 5433 lo usa otro contenedor de esta máquina) |
| Ensayo del despliegue en local | v0.3.3 con datos → `respaldar.sh` → `restaurar.sh` en `develop` con `prod`: 8 migraciones, conteos iguales; vuelta atrás también ensayada |
| gitleaks, historial completo de ambos repos | Sin hallazgos |
| Gate de trazas en los commits de hoy | Vacío en todos los commits de esta entrega |

## Lo que no se hizo y por qué

- **7c, `evaluadoresPendientes`: no hacía falta.** En `develop` el frontend usa `fuentesPendientes` (13
  archivos), igual que el backend. `evaluadoresPendientes` solo existe en el frontend del PR #12, y su
  propio backend no lo devuelve. Se arregla en ese PR, que no se tocó.
- **7b, `docentia-docker`: solo diagnóstico.** Solo tiene `main`, y hoy no se toca `main`. Diferencias
  con `develop`:
  - está en 0.3.2 y sus migraciones llegan a V25 (sin heteroevaluación)
  - el proveedor de IA por defecto es `simulado` (un modelo falso para pruebas)
  - las carpetas se llaman `backend/` y `frontend/`
  - el frontend difiere en 109 archivos

  **No sirve para desplegar.** Opciones: archivarlo o regenerarlo desde la etiqueta `v0.4.0` cuando exista.
- **Orden de las tareas.** La 7a (E2E) se hizo antes que la 5, para que el guion de demostración siga un
  recorrido comprobado.
- **Nada contra producción.** Quedan sin probar: el cliente OAuth real de Google, el runbook con los datos
  reales y la versión y el perfil que corren hoy en el servidor.
- **`.gitignore` nombra la carpeta de configuración de un editor** (línea anterior a esta entrega). No se
  quitó: si alguien tiene esa carpeta, se subiría al repositorio. Decidir si va al `.gitignore` global de
  cada uno.
- **Spotless** no corre en CI. `PeriodoAcademicoControllerTest` (de `develop`) no cumple el formato; los
  archivos de esta entrega sí.
- **El logo del PDF** no está en el repositorio (`resources/marca/LEEME.md`): la institución debe ponerlo
  en el servidor o en `DOCENTIA_ACTAS_LOGO`.

## Lo que el equipo debe hacer o decidir

### Antes del viernes 13 (cierre de la recolección)

1. **Docentes con menos de 5 respuestas de estudiantes.** No reciben nota de estudiantes del formulario. Sin
   CSV de respaldo quedan PARCIAL y **su acta no se puede firmar**. La consulta 3 de
   `consultas-verificacion.sql` los lista con y sin nota por CSV. Opciones (decisión P4 en DECISIONES.md):
   - cargar el CSV de respaldo **antes del viernes**: la carga también cierra con la recolección
   - ampliar la ventana de recolección (punto 2)
   - aceptar que esos docentes queden sin acta este periodo
   - pedir a Vicerrectoría que el promedio se renormalice sin estudiantes, lo que es un cambio de código
2. **¿Ampliar la ventana de recolección?** Se hace en **Periodos** como vicerrectoría y queda en la
   auditoría. Mientras siga abierta, no se puede analizar la fuente FORMULARIO y la coevaluación y la
   autoevaluación siguen editables. Revisar que la revisión de hallazgos siga abierta después del cierre:
   el análisis la exige.
3. **Cerrar el PR #8** (vXro1) sin fusionar. Apunta a `main` con V29–V33, que en `develop` son V36–V40: si
   entra, Flyway falla.

### Sábado 14

4. **Limpieza de `develop`.** Los commits af95d74, f33ae60 y be4487d (6 de noviembre) traen una línea de
   coautoría que el gate rechaza, y `release/0.4.0` los hereda. Opciones:
   - **(a) reescribir esos tres mensajes** en `develop` con force-push. Hay que avisar a todo el equipo
     para que vuelva a sincronizar, y rehacer `release/0.4.0` sobre el `develop` nuevo.
   - **(b) publicar 0.4.0 en `main` con squash.** No reescribe nada, pero `main` y `develop` dejan de
     compartir historia y el siguiente merge de vuelta da más trabajo.

   Recomendación: (a), hecha por una sola persona con el equipo avisado, antes de fusionar la release.
5. **Publicar 0.4.0:** revisar `release/0.4.0`, fusionarla a `main`, etiquetar `v0.4.0` y devolverla a
   `develop`.
6. **Desplegar** siguiendo OPERACION.md → *Despliegue de 0.4.0*. Antes hay que confirmar:
   - carpeta del despliegue, versión que corre hoy y perfil (debe ser `prod`)
   - que no haya usuarios de demo (consulta 5)
   - que no haya V29–V33 aplicadas (consulta 1)

   Si algo falla, no se despliega.

### Sin fecha fija

7. **PR #12** (Daniel, `feature/admin-refactor`). No está listo para fusionar:
   - el frontend lee `evaluadoresPendientes`, que el backend no devuelve
   - con el PR solo ADMINISTRADOR crea usuarios y edita periodos, pero en producción no hay
     administrador. El arranque del #15 crea un VICERRECTOR, y habría que extenderlo a ADMINISTRADOR
   - abre `POST /sesiones/**` sin sesión, y `MatrizAutorizacionTest` ahora lo rechaza
   - 19 commits con líneas de coautoría que el gate rechaza
   - V42 dice "solo lectura" y el PR dice "escritura"
   - necesita ponerse al día con `develop`

   Decidir si se rehace por partes o se cierra.
8. **Decisiones de Vicerrectoría** (DECISIONES.md):
   - P2: umbral de compromisos (3,8 en código, 4,00 en el backlog)
   - P3: quién corrige un acta
   - P5: mínimos de 5 y 10 respuestas
   - escala de 1 a 5
   - matrícula real de Campus/NOVA
   - uso del modelo de lenguaje externo con datos reales (Ley 1581)
9. **Google:** crear el cliente OAuth de la universidad con la URL pública real (por confirmar) y
   configurar `DOCENTIA_GOOGLE_CLIENT_ID` y `VITE_GOOGLE_CLIENT_ID`. Sin eso, los estudiantes no
   entran en producción.
10. **Repo DocenteIA:** actualizar su `main` al publicar 0.4.0, como dice OPERACION.md.

## Ensayo de la presentación del 12 de noviembre

Recorrido completo sobre un clon limpio de `release/0.4.0` con los puertos de la demo (5443, 8199, 5173,
8226): 42 pasos de los flujos A a I, todos pasan. No hubo fallas que bloquearan la demo y no se cambió
código. Guion: `docentia-backend/docs/sustentacion/GUION_PRUEBA_Y_DEMO.md`.

Quedó fuera o por revisar después de la presentación:

- **La semilla no trae ningún docente PARCIAL.** En la demo se crea uno en vivo con la coevaluación de
  Óscar Chantre.
- **Importar el CSV de ejemplo cambia a Julián** de 4,34 a 4,27, y la guía técnica dice 4,34. En la
  presentación no se importa.
- **Anonimato.** La respuesta del estudiante guarda la asignación (clase y docente). En una clase de un
  solo estudiante, quien tenga acceso directo a la base podría cruzarla con la participación. Revisar si
  se agrega como riesgo aceptado en SEGURIDAD.md o si se guarda solo el docente.
- **La semilla depende de la fecha:** desde el 14 de noviembre la recolección queda cerrada. Se extiende
  por configuración en Periodos, sin tocar código.
- **Por confirmar a mano:** que la pantalla de carga CSV no deje importar un archivo con errores y qué
  muestra la interfaz cuando la sesión vence.
- **Docker Desktop se cerró solo dos veces** durante la preparación en esta máquina.

## Correcciones del 12 de noviembre (rama `demo/correcciones-12nov`)

Rama sin fusionar, un commit por punto. Lo que quedó fuera o por confirmar:

- **O. «Cambiar fuentes (tres fuentes) por …»**: el pedido está incompleto (falta el texto nuevo). No se hizo.
- **Retirar el backend de importaciones** (CSV de notas y de matrícula): la interfaz ya no lo usa, pero
  las rutas, servicios y tablas siguen. Tarea futura, con su migración.
- **G. Logo del acta**: el código ya lee `resources/marca/logo-uniautonoma.png`; falta que la
  universidad entregue el archivo.
- **C. Facultad → Programa**: los paneles y agrupaciones ya hablan de programas. El filtro de facultad
  conserva su nombre porque lista facultades reales y la coordinación sigue vinculada a una facultad;
  vincular coordinación a programas exige migración y decisión.
- **M. Visualización**: no incluye Estadísticas ni el ranking, porque esas pantallas muestran a cada
  docente con su nota. Por confirmar con Vicerrectoría si se quiere una versión agregada.
- **M. Vicerrectoría** lee usuarios, catálogos y ponderaciones por la API, pero esas pantallas salieron
  de su menú para no mostrar botones que ya no puede usar.
- **I. «Ver evaluaciones anteriores»**: se ve la coevaluación enviada del periodo; las de periodos
  anteriores no tienen pantalla.
- **V42 y V43** (recuperar contraseña y roles): si el PR #12 trae migraciones con esos números, se
  renumeran allá.

## Correcciones pedidas para la presentación del 12 de noviembre (primer intento, superado)

La ronda de correcciones llegó después de la hora de corte (1:00 a.m. del 12 de noviembre): ninguna se
empezó y la presentación se hace desde `release/0.4.0` sin cambios. Quedan para después de la
presentación, en este orden de prioridad:

| Punto | Pedido | Nota |
|-------|--------|------|
| A | Ocultar del menú «Matrícula y cobertura», «Auditoría» y «Facultades» para todos los roles | Solo `Sidebar.jsx`; sin borrar rutas |
| J | Quitar de vicerrectoría el acceso a «Cargas de estudiantes» | Se resuelve junto con A |
| B | «Docentes destacados» como lista (nombre, programa, puntaje) | Revisar si `GET /kpis` trae los datos o hay que ampliarlo |
| C | Etiquetas «Facultad» → «Programa» en pantallas de coordinación y vicerrectoría | Solo textos visibles |
| E | Estudiante: mensaje al terminar todo, etapa cerrada o sin abrir, formulario más compacto | Ya es por pasos; falta el mensaje final y el de etapa |
| F | Comentario del estudiante obligatorio y con calidad (frontend y backend) | Hoy es opcional; revisar la semilla |
| H, I | Orden y filtros en Docentes y Coevaluaciones; «Ya evaluaste a este docente» | Docentes ya ordena por puntaje; faltan filtros |
| K | Nombre del periodo siempre desde los datos; buscar textos fijos | Por confirmar si hay textos fijos |
| D | Página pública en «/» | Nueva |
| G | Logo del acta | `resources/marca/` no tiene `logo-uniautonoma.png`: lo debe aportar la universidad |
| N | Confirmar que el informe de IA se conserva | Por lo probado, el resultado queda guardado y versionado; por confirmar en pantalla |
| L | Recuperar contraseña | Necesita migración V42, token con hash, vencimiento, límite y pruebas |
| M | Roles ADMINISTRADOR y VISUALIZADOR | Rehacer desde cero, sin el código del PR #12; si usa V42/V43, coordinar con L y con el PR #12 |
| O | «Cambiar fuentes (tres fuentes) por …» | El pedido está incompleto: falta saber por qué texto se cambia |

## Continuación del 12 de noviembre

- **P1, parcial:** se agregó paginación 10/20/50 con filtros y página en la URL para docentes,
  coevaluaciones, usuarios, facultades, programas, periodos, acciones sugeridas, estadísticas,
  paneles por programa, panorama de análisis, comentarios excluidos, notificaciones y auditoría.
  Pruebas puntuales, lint y build pasan. Falta revisar las demás listas y probar las pantallas
  autenticadas en 1366×768 y 1280×720 (estimación: 45–90 min).
- **Verificación visual limitada:** el inicio público no tiene desbordamiento horizontal en 1366×768
  ni 1280×720 y no produjo errores de consola. Falta revisar las pantallas autenticadas en navegador.
- **P2, parcial:** el 401 muestra «Tu sesión venció» y corregir un acta firmada pide confirmación.
  La prueba de acta, lint y build pasan; falta recorrer las rutas con servicios reales.
- **P0/P2 pendientes de ensayo:** ejecutar A–J y verificar consola, errores 5xx, expiración de sesión,
  reintentos y doble envío después de estos cambios (estimación: 60–90 min con los servicios de demo).
- **E8, implementado:** comparación por fuente contra promedios reales del personal activo del programa.
  Coordinación y vicerrectoría ven el comparativo en el expediente; el docente solo después de habilitar
  su resultado. El visualizador recibe 403. Prueba de integración y autorización focalizada pasan.
- **E9, parcial:** el expediente permite imprimir en una página una síntesis con nota, clasificación,
  fuentes, compromisos y estado del proceso, solo para coordinación y vicerrectoría. Falta el logo
  oficial (la universidad debe aportarlo) y validar el ajuste de una página con expedientes extensos.
- **Verificación tras los extras:** frontend: prueba del comparativo, cuatro pruebas de proceso, lint y
  build pasan. Backend: la suite completa ejecutó 1487 pruebas; una prueba de concurrencia enviaba
  comentario nulo pese a ser obligatorio. Se corrigió el dato de prueba y pasó el selector afectado;
  el comparativo también pasó sus pruebas focalizadas. No se repitió la suite completa.
- **P0/P2 pendientes de ensayo:** ejecutar A–J y verificar consola, errores 5xx, expiración de sesión,
  reintentos y doble envío en los flujos autenticados (estimación: 60–90 min con los servicios de demo).
- **Sin cambio:** el texto de reemplazo de «fuentes» sigue pendiente de la decisión del equipo.
