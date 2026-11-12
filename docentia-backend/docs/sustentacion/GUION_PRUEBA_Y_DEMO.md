# Guion de prueba y demostración

> **Términos en pantalla:** la interfaz dice **evaluadores** (estudiantes, coordinación y autoevaluación) donde la API y esta documentación técnica dicen *fuentes*. Los campos de la API no cambian (por ejemplo, `fuentesPendientes`).

Presentación a Vicerrectoría, jueves 12 de noviembre de 2026. Se presenta **en local con Docker y datos
de demostración**, sobre la rama `release/0.4.0`. No se usa el servidor de producción, donde la
recolección real sigue abierta hasta el viernes 13.

Todo lo de este documento se ejecutó la noche del 11 de noviembre sobre un clon limpio de
`release/0.4.0`. Los tiempos y los mensajes entre comillas son los que salieron en esa corrida.

## a. Preparación

### Noche anterior (unos 5 minutos)

En PowerShell, en la carpeta donde está el repositorio:

```powershell
cd C:\ruta\a\docentia-backend          # por confirmar: tu carpeta
git fetch origin
git checkout release/0.4.0
git pull

# .env con los puertos de la demo, sin BOM (Docker no lee bien un .env con BOM)
$puertos = @{ POSTGRES_PORT = '5443'; API_PORT = '8199'; WEB_PORT = '5173'; MAILPIT_PORT = '8226' }
$lineas = Get-Content .env.example | ForEach-Object { $l = $_; foreach ($k in $puertos.Keys) { if ($l -match "^$k=") { $l = "$k=$($puertos[$k])" } }; $l }
[IO.File]::WriteAllLines("$PWD\.env", $lineas)
Select-String -Path .env -Pattern '_PORT='

netsh interface ipv4 show excludedportrange protocol=tcp   # ningún rango debe cubrir 5173, 5443, 8199 u 8226
docker compose down -v
docker compose up --build -d
```

| Medición (11 de noviembre) | Tiempo |
|----------------------------|--------|
| `docker compose up --build -d` con las imágenes base ya descargadas | 34 s; los 4 servicios sanos a los 45 s |
| `docker compose build --no-cache` (máquina sin caché de build) | 89 s, más el arranque |
| Reinicio de datos: `docker compose down -v` y `docker compose up -d --wait` | 42 s |

`docker compose down -v` borra la base del proyecto `docentia` de esta máquina: es lo que queremos para
la demo, pero borra cualquier otro dato local de DOCENTIA.

### Cómo saber que quedó sano

```powershell
docker compose ps                                          # api, db, mailpit y web en (healthy)
curl.exe -s http://localhost:8199/api/actuator/health      # {"status":"UP",...}
start http://localhost:5173                                # pantalla de inicio de sesión
start http://localhost:8226                                # Mailpit
```

### Una hora antes

1. Abre Docker Desktop y espera a que diga que el motor está corriendo. Durante la preparación se cerró
   solo dos veces: si `docker compose ps` responde «failed to connect to the docker API», vuelve a
   abrirlo y espera.
2. Reinicia los datos para que la presentación parta de la semilla:
   `docker compose down -v; docker compose up -d --wait` (42 s).
3. Entra una vez con `vicerrectoria@uniautonoma.edu.co` y confirma en **Periodos** que las cuatro etapas
   de 2026-2 dicen abierta. El 12 de noviembre lo están: recolección hasta el 13, revisión de hallazgos
   hasta el 20, agenda hasta el 27 y firma hasta el 4 de diciembre.
4. No cierres Docker Desktop ni el navegador hasta terminar.

### Entre ensayos

Cada ensayo cambia datos (formularios enviados, actas firmadas). Para repetir:
`docker compose down -v; docker compose up -d --wait`. No hace falta volver a construir.

### Si la demo fuera después del 13 de noviembre

La semilla tiene fechas fijas: después del 13 la recolección queda cerrada y los formularios responden
«etapa cerrada». Se arregla sin tocar código: como vicerrectoría, en **Periodos**, edita 2026-2 y
extiende la etapa que vayas a mostrar hasta el fin del periodo (5 de diciembre). Se probó el cambio de
fechas y su vuelta atrás (PUT 200).

## b. Credenciales

Contraseña de todas las cuentas: `Docentia2026*` (sale de `application-dev.yml`, que lee
`CargadorDatosDesarrollo`; `usuariosDesarrollo.js` ya no existe en esta versión). Solo existen con el
perfil `dev`.

| Rol | Usuario | Para qué se usa |
|-----|---------|-----------------|
| Vicerrectoría | `vicerrectoria@uniautonoma.edu.co` | Inicio institucional, ranking, docentes destacados, Periodos (decide fechas), corrección de actas |
| Administración | `administrador.demo@uniautonoma.edu.co` | Usuarios, Programas, Ponderaciones y Periodos. No ve notas ni actas |
| Visualización | `visualizador.demo@uniautonoma.edu.co` | Solo el panel por programa, sin datos individuales |
| Coordinación | `coordinacion.ingenieria@uniautonoma.edu.co` | Flujo completo de la Facultad de Ingeniería (8 docentes) |
| Coordinación | `coordinacion.derecho@uniautonoma.edu.co` | Alcance: solo ve a Héctor Quilindo y Luisa Mosquera |
| Docente | `nora.munoz@uniautonoma.edu.co` | 2,85, bajo desempeño: acta de compromiso, cita y firma |
| Docente | `julian.hoyos@uniautonoma.edu.co` | 4,34, alto: análisis de comentarios (13 comentarios) |
| Docente | `diana.astaiza@uniautonoma.edu.co` | 4,31: análisis insuficiente (6 de 10 encuestas) |
| Docente | `ramiro.paz@uniautonoma.edu.co` | 1,90, muy bajo, autoevaluación sin responder |
| Docente | `laura.demo@uniautonoma.edu.co` | 3,50 con 4 respuestas de estudiantes: el caso en vivo |
| Docente | `oscar.chantre@uniautonoma.edu.co` | Sin notas: sirve para mostrar un promedio PARCIAL |
| Estudiante | `estudiante.demo5@uniautonoma.edu.co` | Dos evaluaciones pendientes, una de ellas a Laura |
| Docente | `docente.flujo@uniautonoma.edu.co` | Mariana Ruiz Ortega: sin ninguna nota; 11 estudiantes ya la evaluaron. Flujo limpio de punta a punta |
| Estudiante | `estudiante.flujo@uniautonoma.edu.co` | Santiago Gómez Ríos: solo le falta evaluar a Mariana |
| Administración | `administrador.demo@uniautonoma.edu.co` | Usuarios, programas, ponderaciones y fechas |
| Visualización | `visualizador.demo@uniautonoma.edu.co` | Solo el Panel por programa |

Además hay 36 docentes de estadística (`docente.estadistica1` a `36`) en cinco programas, con notas en
todos los niveles, para que Estadísticas y el Inicio tengan volumen.

**Flujo corto para presentar (Mariana):** `estudiante.flujo` la evalúa (el comentario es obligatorio) →
`docente.flujo` hace su autoevaluación → `coordinacion.ingenieria` la coevalúa (en Coevaluaciones,
buscarla por nombre) y pide el análisis de comentarios → `vicerrectoria` ve su resultado, los
destacados y las estadísticas.

## c. Guion de prueba

Orden recomendado; cada flujo deja datos que usa el siguiente. Hazlo completo la noche anterior y
reinicia los datos al terminar. Tiempos de referencia de la corrida del 11 de noviembre.

### A. Arranque y acceso

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| A1 | Abrir `http://localhost:5173` | Página de presentación de DOCENTIA (qué es, para quién, tres pasos) con el botón **Iniciar sesión** | [ ] PASA [ ] FALLA |
| A2 | Entrar con vicerrectoría, coordinación ingeniería, coordinación derecho, `nora.munoz` y `estudiante.demo5` | Todos llegan a `/inicio`: «Resumen de la evaluación» (los tres de gestión), «Mi evaluación 2026-2» (docente), «Mis evaluaciones» (estudiante). Unos 2 s cada uno | [ ] PASA [ ] FALLA |
| A3 | **Salir** | Vuelve a la pantalla de inicio de sesión | [ ] PASA [ ] FALLA |
| A4 | Entrar con `nora.munoz` y una clave errada; luego con un correo que no existe | Las dos veces: «Correo o contraseña incorrectos». No dice cuál de los dos falló | [ ] PASA [ ] FALLA |
| A5 | En el inicio de sesión, **¿Olvidaste tu contraseña?** → escribir `nora.munoz@uniautonoma.edu.co` → **Enviar enlace**; repetir con un correo que no existe | Las dos veces el mismo mensaje. En Mailpit (`http://localhost:8226`) llega «DOCENTIA: Restablecer tu contraseña» solo para Nora; el enlace abre la pantalla para fijar la nueva (8 a 72 caracteres) y sirve una vez | [ ] PASA [ ] FALLA |

### B. Vicerrectoría

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| B1 | **Periodos** | 2026-2 activo con las cuatro etapas y sus fechas, todas abiertas el 12 de noviembre | [ ] PASA [ ] FALLA |
| B2 | Editar 2026-2: cambiar el cierre de Revisión de hallazgos al 21 de noviembre, guardar, y devolverlo al 20 | Se guarda las dos veces; queda en Auditoría | [ ] PASA [ ] FALLA |
| B3 | En el menú de vicerrectoría ya **no** están Usuarios, Programas, Ponderaciones ni carga de estudiantes | La vicerrectoría decide y controla; la operación es de administración | [ ] PASA [ ] FALLA |
| B5 | **Inicio**, **Estadísticas**, **Docentes**, **Panel por programa** | Indicadores de 2026-2 frente a 2026-1 y, debajo, la lista de **Docentes destacados** (nombre, programa, puntaje); Docentes ordenados por puntaje, primero Camilo Vidal (4,75), con filtros de clasificación y orden; el panel muestra cobertura y distribución sin nombres | [ ] PASA [ ] FALLA |
| B6 | Después del flujo C–D: **Docentes → Nora → Acta**, motivo de corrección y **Corregir con una versión nueva** | Aparece «versión 2» e «Historial de versiones (1)» | [ ] PASA [ ] FALLA |
| B7 | Revisar el menú de vicerrectoría tras la corrección del acta | **Auditoría** no aparece como pantalla en el menú | [ ] PASA [ ] FALLA |

### B-bis. Administración (`administrador.demo`) y visualización (`visualizador.demo`)

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| BA1 | Entrar con `administrador.demo` | Llega a **Usuarios**; el menú solo tiene Usuarios, Programas, Ponderaciones y Periodos | [ ] PASA [ ] FALLA |
| BA2 | **Usuarios → Nuevo usuario**: crear un docente con contraseña temporal; salir y entrar con esa cuenta | Se crea; al entrar, lo único que permite es cambiar la contraseña | [ ] PASA [ ] FALLA |
| BA3 | Escribir `http://localhost:5173/docentes` | «Sin acceso»: administración no ve docentes, notas ni actas | [ ] PASA [ ] FALLA |
| BV1 | Entrar con `visualizador.demo` | Llega a **Panel por programa** de toda la universidad; es la única entrada del menú | [ ] PASA [ ] FALLA |
| BV2 | Escribir `http://localhost:5173/docentes` y `http://localhost:5173/usuarios` | «Sin acceso» en las dos | [ ] PASA [ ] FALLA |

Sobre firmar actas: firman **coordinación y docente**, en la reunión. Vicerrectoría no firma: es el rol
que corrige un acta ya firmada (`DOCENTIA_ACTAS_ROL_CORRECTOR=VICERRECTOR`).

### C. Coordinación (`coordinacion.ingenieria`)

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| C1 | **Docentes**; luego entra con `coordinacion.derecho` y abre `http://localhost:5173/docentes/7` | Ingeniería ve sus 8 docentes. Derecho ve solo a Héctor y Luisa, y Nora le responde «no encontrado» | [ ] PASA [ ] FALLA |
| C4 | Entrar con `ramiro.paz` → **Mi autoevaluación**, responder los 14 ítems con 5, revisar, marcar la casilla y **Enviar formulario** | «Formulario enviado». En el expediente de Ramiro (coordinación) pasa de 1,90 a 2,52: el cambio sale de la autoevaluación por formulario | [ ] PASA [ ] FALLA |
| C5 | **Coevaluaciones** → fila de Óscar Chantre → responder los 6 ítems → enviar | Óscar queda **PARCIAL**: falta la autoevaluación. Al volver a abrir su coevaluación: «Ya evaluaste a este docente» | [ ] PASA [ ] FALLA |
| C6 | **Docentes → Nora → Generar acta** → **Agregar compromiso** (texto y plazo) → **Guardar compromiso** | Acta de compromiso: «Nota menor a 3,80» y el compromiso en la lista | [ ] PASA [ ] FALLA |
| C7 | **Agenda de citas** → vista Día → clic en una celda vacía de hoy → **Abrir 1 franja**. Abre tres: para Nora, Julián y Óscar | Cada una queda abierta, de 30 minutos | [ ] PASA [ ] FALLA |
| C8 | Al final: `Docentes → Nora → Seguimiento del plan` | Muestra los compromisos del periodo **anterior** (2026-1) y su estado; en la semilla Nora no tiene. Los de esta acta se siguen en el periodo siguiente | [ ] PASA [ ] FALLA |

### D. Análisis de comentarios y el docente

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| D1 | Coordinación → **Análisis de comentarios** → periodo 2026-2, docente Julián → **Solicitar análisis** | En unos 6 s: «Borrador del análisis» con 3 hallazgos, citas textuales y 2 comentarios excluidos por el filtro. Modelo local: no sale nada a internet | [ ] PASA [ ] FALLA |
| D2 | Marcar el primero **válido**, **Descartar** y **Confirmar descarte** en los otros, **Publicar revisión** | «Revisión publicada» | [ ] PASA [ ] FALLA |
| D3 | Docente Diana → **Solicitar análisis** | «Evidencia insuficiente. Llegaron 6 de 10 encuestas con comentario que necesito para analizar.» | [ ] PASA [ ] FALLA |
| D4 | Volver a Julián → **Solicitar un análisis nuevo** | El botón está deshabilitado hasta escribir un motivo de 10 caracteres; con motivo: «Este análisis se generó hace poco. Puedes regenerarlo 60 minutos después de la última solicitud» | [ ] PASA [ ] FALLA |
| D5 | `julian.hoyos` → **Mi cita** → elegir el horario → **Reservar**; luego `nora.munoz` y `oscar.chantre` igual | «Cita reservada: 12 de nov de 2026, …» | [ ] PASA [ ] FALLA |
| D6 | Coordinación → **Docentes → Julián → Proceso de evaluación → Asistió · habilitar resultado** | Resultado «Habilitado» | [ ] PASA [ ] FALLA |
| D7 | `julian.hoyos` → **Mi evaluación** | Su nota y «Alto desempeño», los pesos 50/30/20, la acción institucional y el hallazgo publicado. **No** ve notas por fuente ni el borrador | [ ] PASA [ ] FALLA |

El docente no pide ni regenera el análisis: eso es de coordinación. El docente solo ve lo que
coordinación publicó, y solo después de su reunión.

### E. Estudiante (`estudiante.demo5`)

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| E1 | **Mis evaluaciones** | «2 pendientes» y el aviso de que las respuestas son anónimas | [ ] PASA [ ] FALLA |
| E2 | **Evaluar a Laura Demo Estudiantes** → responder los 11 ítems con **5 (Muy de acuerdo)** por pasos → **Revisar** → escribir el **comentario** (obligatorio, mínimo 10 caracteres; probar antes «.........» para ver el aviso) → casilla → **Enviar evaluación** | «¡Gracias por tu evaluación!». Con un comentario sin contenido el botón no se habilita | [ ] PASA [ ] FALLA |
| E3 | Volver a Mis evaluaciones | Laura aparece como enviada y no se puede volver a evaluar (por la API: «Ya enviaste esta evaluación», 409). Al enviar también la de Camilo: «Ya completaste todas tus evaluaciones. ¡Gracias por participar!» | [ ] PASA [ ] FALLA |
| E4 | Escribir `http://localhost:5173/docentes` | «Sin acceso» | [ ] PASA [ ] FALLA |

### F. Anonimato: consulta para mostrar

Con el stack arriba, en PowerShell:

```powershell
@'
begin transaction read only;
select table_name as tabla, column_name as columna, data_type as tipo
from information_schema.columns
where table_schema = 'public'
  and table_name in ('respuestas_heteroevaluacion', 'respuestas_descriptor_hetero', 'participaciones_heteroevaluacion')
order by table_name, ordinal_position;
select id, docente_id, periodo_id, instrumento_version from respuestas_heteroevaluacion order by id limit 5;
rollback;
'@ | docker compose exec -T db psql -U docentia -d docentia
```

Lo que sale y cómo leerlo:

- `respuestas_heteroevaluacion`: `id` (UUID aleatorio), `asignacion_id`, `docente_id`, `periodo_id`,
  `instrumento_version`, `comentario`. **No** tiene id de estudiante, fecha ni secuencia; el id es un
  UUID, así que tampoco hay orden de llegada.
- `respuestas_descriptor_hetero`: `respuesta_id`, `descriptor_id`, `valor`.
- `participaciones_heteroevaluacion` (otra tabla): `asignacion_id`, `estudiante_id`, `enviada_en`. Solo
  sirve para que nadie responda dos veces y para la cobertura.
- Ninguna llave une una respuesta con una participación ni con un estudiante.

[ ] PASA [ ] FALLA

### G. Caso en vivo: Laura de 3,50 a 4,50

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| G1 | Coordinación → **Docentes → Laura Demo** | 3,50, desempeño aceptable; Estudiantes 50 % con **3,00**, una nota de estudiantes que ya estaba guardada en la semilla: con 4 respuestas del formulario todavía no se usa, porque el mínimo es 5 | [ ] PASA [ ] FALLA |
| G2 | `estudiante.demo5` evalúa a Laura con 5 en los 11 ítems (E2) | Es la quinta respuesta | [ ] PASA [ ] FALLA |
| G3 | Coordinación → recargar el expediente de Laura | **4,50, Alto desempeño**; Estudiantes 50 % con **5,00**, ahora desde el formulario | [ ] PASA [ ] FALLA |

Si el estudiante responde otros valores, la nota final cambia: para que dé 4,50, todos en 5.

### H. Errores que defienden el sistema

| # | Acción | Esperado | Resultado |
|---|--------|----------|-----------|
| H1 | Coordinación → Análisis de Laura con fuente formulario (se ve en la API) | 409: «Los comentarios del formulario se pueden analizar cuando cierre la recolección» | [ ] PASA [ ] FALLA |
| H2 | Vicerrectoría cierra la Agenda al 10 de noviembre en **Periodos**; coordinación intenta abrir un horario; luego se devuelve la fecha | 409: «La etapa Agenda de citas del periodo 2026-2 está abierta del 2026-10-13 al 2026-11-10; hoy es …» | [ ] PASA [ ] FALLA |
| H3 | `nora.munoz` escribe `http://localhost:5173/usuarios` | «Sin acceso. Tu rol no tiene permiso para ver esta sección.» (la API responde 403 «No tienes permiso para realizar esta acción») | [ ] PASA [ ] FALLA |
| H4 | Sesión vencida (8 horas) o token alterado | La API responde 401 «Debes iniciar sesión para continuar». Se probó con un token vencido y con uno de firma alterada; en pantalla vuelve al inicio de sesión (por confirmar a mano) | [ ] PASA [ ] FALLA |
| H5 | Óscar (PARCIAL, con cita reservada) → coordinación → **Asistió · habilitar resultado** | «El docente aún no tiene las tres evaluaciones; no hay resultado para socializar». Sin reunión no hay firma: el acta queda bloqueada | [ ] PASA [ ] FALLA |

### I. Correos y notificaciones

Mailpit en `http://localhost:8226`. Los correos salen en lotes cada 15 segundos. En la corrida del
11 de noviembre llegaron 42; los que importan para la demo:

| Asunto | A quién | Cuándo |
|--------|---------|--------|
| DOCENTIA: Evaluaciones pendientes | Estudiantes con evaluaciones por hacer | Al abrir la recolección y a 3 y 1 días del cierre |
| DOCENTIA: Autoevaluación pendiente / Coevaluaciones pendientes | Docentes / coordinaciones | A 3 y 1 días del cierre |
| DOCENTIA: Etapa próxima a cerrar | Coordinaciones | En los últimos 3 días de una etapa |
| DOCENTIA: Cobertura baja | Coordinación de la facultad con menos del 50 % | A 2 o 3 días del cierre |
| DOCENTIA: Análisis terminado / Evidencia insuficiente | Coordinación que lo pidió | Al terminar el análisis |
| DOCENTIA: Cita reservada / Cita agendada | Coordinación / docente | Al reservar |
| DOCENTIA: Resultado habilitado | Docente | Cuando coordinación registra la asistencia |
| DOCENTIA: Resultados publicados | Docente | Cuando ya puede ver los hallazgos |
| DOCENTIA: Acta firmada por el docente / Acta firmada | Coordinación / docente | Al firmar |
| DOCENTIA: Acta corregida | Docente | Cuando vicerrectoría corrige |

Las mismas notificaciones aparecen en la campana de cada usuario. [ ] PASA [ ] FALLA

### J. Documentos

`docentia-backend/docs/sustentacion/RESUMEN_EJECUTIVO.md`, `GUIA_TECNICA.md` y `docs/OPERACION.md`
se revisaron contra lo que se ve en pantalla. Una cosa a tener presente:

- La guía técnica dice «abrir un horario»; en pantalla es clic en una celda vacía de la Agenda y luego
  **Abrir 1 franja**.

[ ] PASA [ ] FALLA

## d. Guion de presentación (unos 18 minutos)

Parte de una base recién reiniciada. Antes de empezar, abre en pestañas: la app, Mailpit y una ventana
privada para cambiar de usuario sin cerrar sesión.

| Bloque | Min | Pantalla y usuario | Qué decir |
|--------|-----|--------------------|-----------|
| 1. El problema | 1 | Pantalla de inicio de sesión | «La Resolución 074 pide evaluar a cada docente con tres miradas y cerrar con un acta firmada. DOCENTIA lleva ese proceso de punta a punta.» |
| 2. Vista institucional | 2 | Vicerrectoría → Inicio, Periodos | «Así ve Vicerrectoría el periodo: cuántos docentes van evaluados y en qué etapa estamos. Las fechas las fija Vicerrectoría y el sistema las respeta solo.» |
| 3. El estudiante evalúa | 3 | `estudiante.demo5` → Mis evaluaciones → evaluar a Laura con 5 | «El estudiante ve solo a sus profesores. Sus respuestas no quedan unidas a su nombre: nadie, ni Vicerrectoría, puede saber quién respondió qué.» |
| 4. La nota cambia en vivo | 2 | Coordinación → Laura | «Con menos de cinco respuestas no usamos la opinión de los estudiantes, para proteger su anonimato. Con la quinta, la nota pasa de 3,50 a 4,50.» |
| 5. Comentarios | 3 | Coordinación → Análisis de comentarios → Julián; luego Diana | «El sistema agrupa lo que dicen los estudiantes en fortalezas y oportunidades, cada una con la frase exacta que la respalda. Corre dentro del servidor; no envía comentarios a ningún servicio externo. Coordinación decide qué se publica. Si hay pocas encuestas, lo dice y no inventa conclusiones.» |
| 6. El acta | 4 | Coordinación → Nora: generar acta, compromiso, abrir horario; Nora reserva; coordinación registra asistencia y firma; Nora firma y descarga el PDF | «Bajo 3,80 el acta exige compromisos con plazo. El docente ve su resultado en la reunión, no antes. Los dos firman y el PDF lleva un código que cualquiera puede verificar.» |
| 7. Control | 2 | Vicerrectoría → corregir el acta de Nora → ver la versión 2 en el expediente | «Un acta firmada no se puede cambiar. Si hay un error, Vicerrectoría crea una versión nueva con el motivo, y todo queda en el registro de auditoría, que no tiene pantalla pero se conserva.» |
| 8. Límites y siguientes pasos | 1 | Sin pantalla | «Quedan decisiones de Vicerrectoría: el umbral de 3,8 o 4,0, qué hacer con docentes con menos de cinco respuestas y el ingreso de estudiantes con la cuenta institucional.» |

## e. Preguntas probables

**¿Se puede saber qué respondió un estudiante?** No desde la aplicación. La respuesta y el registro de
quién respondió están en tablas separadas, sin fecha ni orden. Con menos de cinco respuestas no se
muestra ningún agregado. Siendo exactos: la respuesta sí guarda la clase evaluada. En una clase con un
solo estudiante, alguien con acceso directo a la base de datos podría deducirlo; por eso ese acceso debe
quedar restringido.

**¿Qué pasa con un docente con menos de cinco respuestas?** No se usa la opinión de los estudiantes.
El promedio queda parcial y el acta no se puede firmar. Es una decisión pendiente de Vicerrectoría:
ampliar la ventana de recolección o renormalizar sin estudiantes.

**¿Quién firma el acta?** La coordinación y el docente, en la reunión, después de registrar la
asistencia. Vicerrectoría corrige un acta firmada, con motivo, creando una versión nueva.

**¿Qué no cubre?** Las evidencias complementarias y los grupos focales (HU-09). No hay conexión en
vivo con Campus/NOVA para la matrícula. Sin cliente de Google institucional, los
estudiantes no entran en producción. La escala de 1 a 5 y el umbral están por confirmar. No hay
avisos en tiempo real: la campana consulta cada minuto.

**¿Se usa inteligencia artificial con los comentarios?** Por defecto, un modelo local por reglas que
corre en el servidor y no envía nada fuera. Se puede activar un modelo de lenguaje externo, pero está
apagado, exige configuración explícita, anonimiza antes de enviar y tiene topes de gasto. Usarlo con
datos reales requiere revisar la política de datos de la universidad (Ley 1581). Las métricas del
modelo local se midieron con comentarios de prueba, no con comentarios reales.

**¿Cuándo estaría en producción?** La versión 0.4.0 está lista para revisión. El plan es desplegarla
después del cierre de la recolección del viernes 13, siguiendo el procedimiento de OPERACION.md. La
fecha final está por confirmar, y falta el cliente de Google de la universidad para el ingreso de
estudiantes.

## f. Plan B (menos de un minuto)

| Falla | Qué hacer |
|-------|-----------|
| Docker no levanta o `docker compose` dice «failed to connect to the docker API» | Abrir Docker Desktop y esperar a que el motor esté corriendo; `docker compose up -d --wait`. Pasó dos veces en la preparación |
| Puerto ocupado o reservado («access a socket in a way forbidden», «ports are not available») | `netsh interface ipv4 show excludedportrange protocol=tcp`. Si el rango reservado (por ejemplo 7642–8141) cubre un puerto, cámbialo en `.env` (por ejemplo `WEB_PORT=18173`) y `docker compose up -d`. La noche del 11 no había rangos sobre los puertos de la demo, pero cambian al reiniciar Windows |
| El correo no llega | Esperar 15 s (se envían por lotes). Mostrar la campana de la app, que tiene los mismos avisos |
| Pantalla en blanco | `Ctrl+F5`; si sigue, `docker compose ps` y `docker compose restart web` |
| Login falla con la clave correcta | Revisar `Docentia2026*` (D mayúscula, asterisco). Si responde que hay demasiados intentos: 5 fallos por correo bloquean 10 minutos. Usa otra cuenta o `docker compose restart api`, que borra el contador. Si no entra ninguna cuenta, la base quedó sin semilla: `docker compose down -v; docker compose up -d --wait` |
| Un paso dice «etapa cerrada» | Vicerrectoría → Periodos → extender esa etapa (sección a) |

## g. Qué no mostrar

- GitHub, ramas, pull requests ni el historial de commits.
- El servidor de producción ni sus datos: la recolección real sigue abierta.
- El archivo `.env`, claves, secretos o variables de entorno.
- La terminal con logs, salvo la consulta de anonimato (F).
- Mailpit con correos de ensayos anteriores: reinicia los datos antes de presentar.
