# DOCENTIA: guía técnica

Guía para la sustentación: cómo está hecho el sistema, por qué, cómo se levanta y cómo se demuestra.
El detalle de cada tema está en los documentos de `docentia-backend/docs/` que se citan en cada sección.

## 1. Arquitectura

| Capa | Tecnología |
|------|------------|
| Web | React 19 con Vite, servida por nginx; nginx reenvía `/api` al backend |
| API | Spring Boot 3.5 sobre Java 21: Web, Security (JWT HS256), Data JPA, Validation, Mail, Actuator |
| Datos | PostgreSQL 16, esquema versionado con Flyway (36 migraciones); Hibernate solo valida |
| PDF | OpenPDF |
| Correo | Cola en base de datos; Mailpit en desarrollo, SMTP con TLS en producción |
| Empaquetado | `docker-compose.yml` con cuatro servicios: `db`, `api`, `web` y `mailpit` |

El backend sigue capas: los controladores traducen HTTP, los servicios tienen las reglas y los
repositorios el acceso a datos. Hay tres procesos en segundo plano, todos sobre la base con
`FOR UPDATE SKIP LOCKED` para que dos réplicas no tomen la misma fila:

- **Procesador de análisis:** toma las solicitudes en cola y llama al modelo fuera de la transacción.
- **Procesador de correo:** envía en lotes de 20, con 5 intentos y espera creciente.
- **Generador de recordatorios:** cada 15 minutos crea los avisos que dependen del calendario.

Diagramas en [ARQUITECTURA.md](../ARQUITECTURA.md).

## 2. Modelo de datos

44 tablas, agrupadas así:

| Grupo | Tablas |
|-------|--------|
| Estructura | `facultades`, `programas`, `usuarios`, `periodos_academicos`, `etapas_periodo`, `ponderaciones`, `cursos`, `clases`, `clases_docente`, `matriculas` |
| Instrumento (Anexo 1) | `criterios_evaluacion` (6), `descriptores_evaluacion` (14), `descriptor_actor` (AUTO 14, COEVAL 6, HETERO 11), `evaluaciones_formulario`, `respuestas_descriptor`, `comentarios_criterio` |
| Heteroevaluación anónima | `participaciones_heteroevaluacion`, `respuestas_heteroevaluacion`, `respuestas_descriptor_hetero` |
| Notas | `calificaciones`, `correcciones_calificacion`, `promedios_docente`, `importaciones`, `plantillas_mapeo`, `comentarios_estudiantes` |
| Análisis de comentarios | `solicitudes_analisis`, `hallazgos_analisis`, `citas_hallazgo`, `versiones_hallazgo`, `comentarios_filtrados`, `comentarios_analizados`, `resultados_analisis_comentarios`, `cache_respuestas_ia`, `uso_ia` |
| Cierre del proceso | `franjas_horarias`, `citas`, `actas`, `firmas_acta`, `compromisos`, `seguimiento_compromisos` |
| Transversal | `notificaciones`, `preferencias_notificacion`, `cola_correo`, `auditoria` |

Reglas que viven en la base, no solo en el código: un solo periodo activo (índice parcial único), un
acta firmada no cambia y la auditoría no se edita ni se borra (triggers), franjas sin solape del mismo
coordinador y una sola participación por estudiante y asignación (índices únicos).

## 3. Flujo de evaluación

El periodo tiene cuatro etapas con fechas que fija vicerrectoría. Cada acción consulta
`VentanaEtapaService`; fuera de su ventana la API responde 409 con la etapa y sus fechas. No hay
tareas que abran o cierren etapas: decide la fecha (zona America/Bogota, fechas inclusivas).

| Etapa | Quién hace qué |
|-------|----------------|
| Recolección de calificaciones | Estudiantes responden la heteroevaluación; coordinación, la coevaluación; el docente, su autoevaluación. |
| Revisión de hallazgos | Coordinación pide el análisis de comentarios, valida, edita o descarta hallazgos y publica |
| Agenda de citas | Coordinación abre horarios de 30 minutos; el docente reserva, confirma o declina |
| Firma de actas | En la reunión, coordinación registra la asistencia; el docente ve su resultado y ambos firman |

**La nota.** Cada formulario enviado vuelve su media la nota de esa fuente. El promedio es
`0,5 × estudiantes + 0,3 × coordinación + 0,2 × autoevaluación`, redondeado a dos decimales, y se
recalcula con cada nota nueva. Con las tres fuentes queda COMPLETO; con alguna faltante, PARCIAL
(renormalizado); sin ninguna, SIN_NOTA. Categorías: muy alto desde 4,60, alto desde 3,80, aceptable
desde 3,00, bajo desde 2,20 y muy bajo por debajo.

**El acta.** Coordinación la genera desde el expediente. Bajo el umbral (3,80) solo cabe el acta de
compromiso y exige al menos un compromiso con plazo. Se firma con el promedio COMPLETO, la etapa abierta,
la cita agendada y la asistencia registrada. Cada firma lleva el trazo dibujado (o el nombre escrito
convertido en trazo). El PDF imprime el SHA-256 del contenido y un código que cualquiera verifica en
`/verificar-acta/{codigo}` sin ver notas. Un acta firmada no cambia; vicerrectoría la corrige creando
una versión nueva con motivo. Los compromisos se siguen en el periodo siguiente.

**Regla A4.** El docente nunca ve notas por fuente ni un promedio parcial, y su resultado y sus
hallazgos solo se le muestran después de la reunión.

Contratos en [API.md](../API.md) y [api-proceso-evaluacion.md](../api-proceso-evaluacion.md).

## 4. Anonimato de los estudiantes

- **Dos tablas sin vínculo.** `participaciones_heteroevaluacion` sabe quién ya respondió (para impedir
  el doble envío y medir cobertura). `respuestas_heteroevaluacion` guarda qué se respondió, con UUID
  aleatorio, sin estudiante, sin fecha y sin orden. Ninguna columna las une.
- **k-anonimato.** Con menos de 5 respuestas por docente no se usa ni se muestra ningún agregado.
- **Comentarios.** Los del formulario solo se leen para el análisis cuando la recolección ya cerró. La
  copia que queda no tiene fecha ni id de respuesta y se ordena por huella del texto.
- **Registros.** Ni el envío ni el análisis escriben comentarios, respuestas o correos en el log.

Decisiones en [DECISIONES.md](../DECISIONES.md) y controles en [SEGURIDAD.md](../SEGURIDAD.md).

## 5. Análisis de comentarios

```
comentarios → filtro de calidad → filtro de contenido → anonimización → modelo → verificación de citas
```

1. **Filtro de calidad:** vacíos, muy cortos, duplicados y órdenes dirigidas al modelo.
2. **Filtro de contenido:** ofensivos, sobre la persona o situaciones graves. Las graves se excluyen y
   coordinación recibe una advertencia con la cantidad.
3. **Anonimización:** quita nombres, correos, teléfonos, documentos, enlaces y el nombre del docente.
4. **Modelo:** por defecto, local y por reglas, con un léxico en español, sin red ni costo. El modelo
   de lenguaje (`chat-completions`) es opcional. Solo se usa si el servidor tiene URL, modelo y clave, y
   si coordinación lo elige en la solicitud (`LOCAL` o `LLM`).
5. **Verificación de citas:** cada hallazgo cita comentarios por id y el texto lo pone el sistema. Una
   cita que no corresponde queda sin verificar y el hallazgo no se puede validar.

Con el modelo de lenguaje: muestra de hasta 150 comentarios recortados a 300 caracteres, topes de 16.000
tokens de entrada y 4.000 de salida, presupuesto de 500.000 tokens por día y 3.000.000 por periodo,
caché por huella (repetir no gasta) y una hora de espera para regenerar, con motivo. Un análisis necesita
10 encuestas; el del formulario, además, la recolección cerrada.

Coordinación revisa cada hallazgo (válido, editado o descartado) y publica; el docente solo ve lo que se
publicó, después de su reunión. Detalle en [DECISIONES-ANALISIS-IA.md](../DECISIONES-ANALISIS-IA.md) y
[ANALISIS_IA_COSTOS.md](../ANALISIS_IA_COSTOS.md).

## 6. Seguridad

| Tema | Cómo |
|------|------|
| Sesión | JWT HS256 de 8 horas; en `prod` la app no arranca con un secreto de desarrollo o de menos de 32 caracteres |
| Roles | Seis: estudiante, docente, coordinación, vicerrectoría, administración y visualización. `@PreAuthorize` en cada ruta; `MatrizAutorizacionTest` comprueba las 115 rutas con cada rol y sin sesión (`ROLES.md`) |
| Alcance | Coordinación solo ve su facultad y el docente lo propio; fuera de alcance responde 404 |
| Rutas públicas | Solo inicio de sesión (correo o Google), verificación de actas y salud |
| Contraseña temporal | Toda cuenta creada por vicerrectoría debe cambiarla antes de usar el sistema |
| Fuerza bruta | 5 intentos por correo y 30 por IP en 10 minutos (429) |
| Errores | RFC 7807 sin trazas, con `correlationId` |
| Cabeceras | CSP, `X-Frame-Options`, `nosniff` y `Referrer-Policy` en la API y en nginx |
| Secretos | Solo por entorno; gitleaks en cada CI |
| Producción | Sin seed: el primer administrador se crea con `DOCENTIA_BOOTSTRAP_*`; los estudiantes entran con Google |

## 7. Cómo levantar todo

Requisito: Docker Desktop.

```bash
git clone -b develop https://github.com/bryanypes/docentia-backend.git
cd docentia-backend
docker compose up --build -d
```

| Servicio | Dirección |
|----------|-----------|
| Aplicación | <http://localhost:5173> |
| API | <http://localhost:8080/api> |
| Correos (Mailpit) | <http://localhost:8025> |
| PostgreSQL | `localhost:5433` |

Si un puerto está ocupado o reservado (en Windows, Hyper-V reserva rangos), copia `.env.example` como
`.env` y cambia los puertos. `docker compose down -v` borra los datos y al subir vuelve la semilla.
Producción y respaldos: [OPERACION.md](../OPERACION.md).

## 8. Usuarios de demostración

Solo existen con el perfil `dev`. Contraseña de todos: `Docentia2026*`.

| Rol | Correo | Para mostrar |
|-----|--------|--------------|
| Vicerrectoría | `vicerrectoria@uniautonoma.edu.co` | Toda la universidad, periodos, usuarios, auditoría, corrección de actas |
| Coordinación | `coordinacion.ingenieria@uniautonoma.edu.co` | Facultad de Ingeniería: el flujo completo |
| Coordinación | `coordinacion.derecho@uniautonoma.edu.co`, `coordinacion.finanzas@uniautonoma.edu.co` | Alcance por facultad |
| Docente | `nora.munoz@uniautonoma.edu.co` | Nota 2,85, bajo desempeño: acta de compromiso |
| Docente | `julian.hoyos@uniautonoma.edu.co` | Nota 4,34, alto, con 13 comentarios para el análisis |
| Docente | `diana.astaiza@uniautonoma.edu.co` | Nota 4,31, solo 6 comentarios: «evidencia insuficiente» |
| Docente | `camilo.vidal@uniautonoma.edu.co` | Nota 4,75, muy alto |
| Docente | `ramiro.paz@uniautonoma.edu.co` | Nota 1,90, muy bajo; autoevaluación sin responder |
| Docente | `laura.demo@uniautonoma.edu.co` | 4 respuestas de estudiantes, una menos que el mínimo |
| Estudiante | `estudiante.demo5@uniautonoma.edu.co` | Dos evaluaciones pendientes, una de ellas de Laura |
| Estudiante | `karen.mancilla@uniautonoma.edu.co` | Tres clases importadas del JSON de Campus |

## 9. Guion de demostración por rol

Antes de empezar: `docker compose down -v && docker compose up --build -d`. La semilla cierra la
recolección el 13 de noviembre de 2026 y el periodo el 5 de diciembre. Si la demostración es después del
13, entra como vicerrectoría a **Periodos** y extiende las etapas que vayas a mostrar; si no, esas acciones
responden «etapa cerrada», que también es una regla del sistema. Este mismo recorrido lo hacen las
pruebas `DocenteIA/e2e` (11 de 11).

**Vicerrectoría** (`vicerrectoria`)

1. **Inicio:** indicadores de 2026-2 frente a 2026-1 y docentes destacados.
2. **Periodos:** las cuatro etapas de 2026-2, cada una con su estado de hoy.
3. **Ponderaciones:** 50/30/20; el botón solo se habilita si suman 100.
4. **Usuarios:** crear un docente. Al entrar con esa cuenta, el sistema exige cambiar la contraseña.

**Estudiante** (`estudiante.demo5`)

5. **Mis evaluaciones:** dos profesores pendientes y el aviso de que las respuestas son anónimas.
6. **Evaluar a Laura Demo Estudiantes:** 11 ítems por pasos, revisión y envío. Un segundo envío se rechaza.

**Coordinación** (`coordinacion.ingenieria`)

7. **Docentes → Laura Demo:** con la quinta respuesta, la nota de estudiantes sale del formulario.
8. **Panel por programa:** cobertura de respuestas por docente y programa, sin saber quién respondió qué.
9. **Docentes → Nora Muñoz:** 2,85, bajo desempeño, acción institucional. **Generar acta:** acta de
   compromiso; **Agregar compromiso** con plazo.
10. **Agenda de citas:** abrir un horario de 30 minutos para hoy.
11. **Análisis de comentarios → Julián → Solicitar análisis:** borrador con hallazgos y citas. Editar uno,
    marcarlo válido, descartar otro y **Publicar revisión**. En Diana: «evidencia insuficiente».
12. **Panel por programa:** cobertura y distribución sin nombres de docentes.

**Docente** (`nora.munoz`)

13. **Mi cita:** reservar el horario de hoy. **Mi acta** todavía no se puede firmar.

**Coordinación**, de nuevo

14. **Docentes → Nora → Proceso de evaluación:** «Asistió · habilitar resultado». **Firmar acta** con trazo.

**Docente**, de nuevo

15. **Mi evaluación:** ahora ve su resultado, sin notas por fuente. **Mi acta → Firmar acta.**
16. **Descargar PDF** y abrir `/verificar-acta/{código}` sin sesión.

**Vicerrectoría**, de nuevo

17. **Docentes → Nora:** corregir el acta con motivo; aparece la versión 2 y el historial.
18. **Auditoría:** la corrección, quién la hizo y cuándo.
