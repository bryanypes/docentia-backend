> **Reemplazado en 0.3.3.** Este documento describe el primer esquema de heteroevaluación,
> que guardaba al estudiante junto a sus respuestas. Lo vigente está en [DECISIONES.md](DECISIONES.md)
> y [API.md](API.md).

# Decisiones — heteroevaluación (HU-01)

Decisiones tomadas al pasar la fuente **Estudiantes (50 %)** del CSV del coordinador al
formulario del estudiante en la app. La resolución, el JSON de Campus y el código mandan
sobre el Excel. Contrato: [api-heteroevaluacion.md](api-heteroevaluacion.md). Recorrido con
capturas: [recorrido-heteroevaluacion/FLUJO.md](recorrido-heteroevaluacion/FLUJO.md).

## Pendientes de Vicerrectoría

| # | Tema | Decisión aplicada | Quién la confirma |
|---|------|-------------------|-------------------|
| P3 | Escala de 1 a 5 | Misma que AUTO/COEVAL. La resolución no la define; etiquetas en `docentia.formularios.etiquetas-escala`. | Vicerrectoría |
| P4 | Canal del estudiante | Se responde en esta app, no en un formulario externo ni en Campus. | Vicerrectoría |
| P5 | Sync en vivo Campus/NOVA | El JSON es el contrato; el seed lo simula. No hay cliente HTTP a Campus. | Vicerrectoría / sistemas |

## Rol y sesión

- **Cuarto rol.** Se agregó `ESTUDIANTE`. Siguen existiendo solo `VICERRECTOR`, `COORDINADOR`,
  `DOCENTE` y `ESTUDIANTE`. No hay administrador.
- **Vínculo.** El estudiante lleva `idPrograma` (carrera), igual que el docente. La facultad es
  la del programa. Sin programa no se crea.
- **JWT.** Viajan `idPrograma` e `idFacultad`. Las clases **no** van en el token: se leen de
  `matriculas` en cada listado. Así un cambio de matrícula no exige volver a entrar.
- **Alcance.** Solo sus matrículas activas del periodo. Clase ajena, inactiva o inexistente:
  **404** con el mismo mensaje. Otro rol en las rutas HETERO: **403**.
- **Inicio.** `/evaluacion-profesores`. El menú del estudiante tiene un solo enlace:
  **Evaluación a profesor**.
- **Contraseña temporal.** Igual que el resto: solo puede leer la sesión y cambiar la clave.

## Unidad de evaluación

- **Qué se evalúa.** Un formulario HETERO por (estudiante, clase, docente) en el periodo activo.
  Clase = `cod_curso` + `grupo` + `jornada` (y periodo), como viene de Campus.
- **Grupo A y grupo B.** Son dos clases. Karen ve Zulema en A; Andrea la ve en B. El mismo
  profesor no se “aplana” a una sola fila.
- **Varios docentes en una clase.** Un formulario por cada uno de `docentes[]`.
- **Unique.** AUTO y COEVAL siguen únicos en `(periodo, docente, tipo)` con `clase_id` nulo.
  HETERO no cabe en ese índice: el unique es `(clase_id, docente_id, evaluador_id)` (V27).

## Instrumento y envío

- **Once ítems.** Actor `HETERO` de V25: `D02`–`D12`. No entran `D01`, `D13` ni `D14`.
- **Misma mecánica que AUTO/COEVAL.** Borrador, autoguardado, envío irreversible, ventana
  `RECOLECCION_CALIFICACIONES`. Fuera de la ventana: **409**. Incompleto: **422**.
- **Pantalla.** Se reutiliza `FormularioInstrumento`. No hay carga de archivo en el estudiante.

## Nota de la fuente ESTUDIANTES

- **Agregado.** Media de las medias de todos los HETERO `ENVIADA` del docente en el periodo
  (todos sus grupos y cursos), 4 decimales HALF_UP, origen `FORMULARIO`.
- **Quién no queda guardado.** `registrarDesdeFormulario` recibe evaluador nulo: la
  calificación de fuente no lleva el id del estudiante.
- **Precedencia.** Esa media reemplaza la nota ESTUDIANTES que hubiera llegado por CSV.
  El cambio queda en el historial de correcciones.
- **Coordinación.** Ve la nota agregada y cobertura. Nunca quién puntó qué.
- **Caso de demo.** Karen 5 en Zulema A + Andrea 3 en Zulema B = **4,00**.

## Comentarios e IA

- **Sí salen a análisis.** Los comentarios por criterio de un HETERO enviado van a
  `comentarios_estudiantes`, con huella y **sin** id de estudiante.
- **Sin importación.** `id_importacion` pasa a opcional (V27). Los del formulario no
  inventan una carga CSV.
- **Criterio.** HETERO no usa C1 (solo D01, actor AUTO/COEVAL). Los comentarios de esta
  fuente van en criterios que sí tienen descriptores HETERO (a partir de C2).
- **Ley 1581.** No se persisten `documento`, `persona_id`, `usuario_id` ni `correo_generico`
  del JSON de Campus. Identidad en DOCENTIA: correo.

## Corte del CSV

- **POST cortado.** `/importaciones/csv` y `.../validar` responden **409**
  `"La evaluación de estudiantes se responde en el formulario"`. El tope de tamaño (413)
  se sigue evaluando antes.
- **GET historial.** Sigue. El menú de coordinación y vicerrectoría ya no ofrece subir
  archivo. La ruta `/importaciones` muestra aviso, no selector.
- **Plantillas de mapeo.** Dejan de usarse para notas nuevas; no se borraron.

## Campus y persistencia

- **DOCENTIA no es el académico.** Solo guarda el recorte para evaluar. El contrato de
  entrada es el JSON de facultad / persona estudiante / persona docente.
- **Sin entidad Coordinación.** `codCoordinacion` es informativo. El coordinador de
  DOCENTIA sigue ligado a `idFacultad`.
- **Códigos.** Facultad `FC001`, programa `CU027`, curso `cod_curso`. Unique activo.
- **Baja.** `es_activa = false` en curso y matrícula. No hay `deleted_at`.
- **Migraciones.** V26 clases, matrículas y rol estudiante. V27 `clase_id` en el
  formulario, checks de tipo y unique HETERO. No se editó V25 ni anteriores.
- **Seed.** `campus-clases-demo.json`, mismo esquema, correos de demo. Contraseña
  `Docentia2026*`. Corre aunque la base ya tenga el seed de Ingeniería/Derecho: si no,
  Karen no existe y el login responde 401.

## Pantalla

- **Listado.** Profesor, curso, grupo, programa, estado, avance. Sin puntajes.
- **Formulario.** 11 ítems, etapa cerrada en aviso, 404 si la clase no es suya.
- **Textos.** Interfaz y errores en español.

## Fuera de esta entrega

- Sync en vivo con Campus/NOVA.
- Entidad Coordinación (`codCoordinacion`).
- Guardar cédula o `persona_id`.
- Formulario de estudiantes en un canal distinto a esta app.
