> **Reemplazado en 0.3.3.** Este documento describe el primer esquema de heteroevaluación,
> que guardaba al estudiante junto a sus respuestas. Lo vigente está en [DECISIONES.md](DECISIONES.md)
> y [API.md](API.md).

# Heteroevaluación (estudiante → docente)

SPEC de HU-01. Las etapas 1 a 3 ya tienen pruebas. Si el código y este texto chocan, mandan
el código y se corrige aquí en el mismo cambio.

Contrato de personas y clases: el JSON de Campus (facultad / estudiante / docente).
DOCENTIA no es el sistema académico: **persiste el recorte necesario para evaluar**.
El seed `src/main/resources/seed/campus-clases-demo.json` usa **el mismo esquema**, con
correos de demo y **sin documento, persona_id ni usuario_id reales**.

## Qué cambia

Hoy la fuente `ESTUDIANTES` (50 %) llega por CSV del coordinador. Esa carga de **notas** se corta.

El estudiante entra a la app, ve solo **Evaluación a profesor** y responde el Anexo 1
(11 ítems HETERO) por cada clase en la que está matriculado. Al enviar, se agrega la media
de los formularios `ENVIADA` del docente en el periodo y corre `PromedioService`.
Autoevaluación, coevaluación, actas, citas y análisis siguen igual.

## Contrato Campus (entrada)

Tres formas. Quien sincronice o siembre debe respetar estos nombres de campo.

### Facultad (catálogo)

```json
{
  "codFacultad": "FC001",
  "facultad": "Ciencias Administrativas, Contables y Económicas",
  "codCoordinacion": "FC001-11",
  "coordinacion": "COORDINACION FINANZAS",
  "coordinador": {
    "nombre": "COORDINACION FINANZAS",
    "correo": "coordinacion.finanzas@uniautonoma.edu.co"
  },
  "sinCoordinadorReal": false,
  "programas": [
    { "codPrograma": "CU027", "programa": "ADMINISTRACION DE EMPRESAS PROFESIONAL" },
    { "codPrograma": "CU012", "programa": "FINANZAS Y NEGOCIOS INTERNACIONALES" }
  ]
}
```

En esta entrega **no** se crea la entidad Coordinación. `codCoordinacion` queda
informativo. El coordinador de DOCENTIA sigue ligado a `idFacultad`. El programa
guarda `codigo` (`CU027`) y el nombre.

### Persona estudiante (`roles[].rol = "Estudiante"`, `clases[].relacion = "Matriculado"`)

```json
{
  "correo": "karen.mancilla@uniautonoma.edu.co",
  "nombres": "Karen Verónica",
  "apellidos": "Mancilla Solarte",
  "roles": [
    {
      "rol": "Estudiante",
      "codigo": "E-18764",
      "es_facultad": false,
      "clases": [
        {
          "relacion": "Matriculado",
          "cod_curso": "19072209",
          "curso": "CREATIVIDAD E INNOVACIÓN EMPRENDIMIENTO",
          "tipo_curso": "S001",
          "grupo": "E",
          "jornada": "S001",
          "docentes": [
            {
              "codigo": "D-0355",
              "correo": "martin.valencia@uniautonoma.edu.co",
              "nombres": "Martín José",
              "apellidos": "Valencia Gutiérrez"
            }
          ]
        }
      ]
    }
  ]
}
```

Identidad en DOCENTIA: `correo` (único). `codigo` del rol es el código de estudiante.
Cada elemento de `clases[]` es una **clase**: `cod_curso` + `grupo` + `jornada` +
`tipo_curso`, con `docentes[]`. El listado del estudiante sale de esas clases.

No se persisten `documento`, `persona_id`, `usuario_id` ni `correo_generico` (Ley 1581).

### Persona docente (`roles[].rol = "Docente"`, `clases[].relacion = "Dicta"`)

```json
{
  "correo": "zulema.leon@uniautonoma.edu.co",
  "nombres": "Zulema Yidney",
  "apellidos": "León Escobar",
  "roles": [
    {
      "rol": "Docente",
      "codigo": "D-17090",
      "es_facultad": false,
      "clases": [
        {
          "relacion": "Dicta",
          "cod_curso": "12190626",
          "curso": "DESARROLLO DE APLICACIONES MÓVILES",
          "tipo_curso": "S001",
          "grupo": "A",
          "jornada": "S001",
          "docentes": [
            {
              "codigo": "D-17090",
              "correo": "zulema.leon@uniautonoma.edu.co",
              "nombres": "Zulema Yidney",
              "apellidos": "León Escobar"
            }
          ],
          "num_estudiantes": 13
        },
        {
          "relacion": "Dicta",
          "cod_curso": "12190626",
          "curso": "DESARROLLO DE APLICACIONES MÓVILES",
          "tipo_curso": "S001",
          "grupo": "B",
          "jornada": "S001",
          "docentes": [
            {
              "codigo": "D-17090",
              "correo": "zulema.leon@uniautonoma.edu.co",
              "nombres": "Zulema Yidney",
              "apellidos": "León Escobar"
            }
          ],
          "num_estudiantes": 20
        }
      ]
    }
  ]
}
```

La misma `cod_curso` con grupo A y grupo B son **dos clases**. `num_estudiantes` es
cobertura esperada, no una nota. Si `docentes[]` trae más de uno, el estudiante
responde un formulario por cada profesor de esa clase.

## Mapeo Campus → DOCENTIA

| Campo Campus | Destino |
|--------------|---------|
| `codFacultad` | `facultades.codigo` |
| `facultad` | `facultades.nombre` |
| `codPrograma` | `programas.codigo` |
| `programa` | `programas.nombre` |
| `correo` | `usuarios.correo` (login) |
| `nombres` + `apellidos` | `usuarios.nombre_completo` |
| `roles[].codigo` | `usuarios.codigo` |
| `Estudiante` / `Docente` | `usuarios.rol` `ESTUDIANTE` / `DOCENTE` |
| `cod_curso` | `cursos.codigo` |
| `curso` | `cursos.nombre` |
| `grupo`, `jornada`, `tipo_curso` | `clases` |
| `relacion: Matriculado` | `matriculas` |
| `relacion: Dicta` / `docentes[]` | `clases_docente` |
| `num_estudiantes` | `clases.inscritos_esperados` |
| `codCoordinacion` | no se persiste (informativo) |
| `documento`, `persona_id`, `usuario_id` | no se persisten |

## Unidad de evaluación

Un formulario HETERO por **(estudiante, clase, docente)** en el periodo activo.

- Clase = `cod_curso` + `grupo` + `jornada` (y periodo).
- Si el mismo profesor dicta grupo A y B, cada estudiante solo ve **su** grupo.
- El listado muestra profesor, curso, grupo y carrera (`programa` del estudiante).

La nota de fuente `ESTUDIANTES` del docente en el periodo es la **media de todos** los
formularios HETERO `ENVIADA` de ese docente (todos sus grupos y cursos). Coordinación
ve cobertura (enviadas / matriculados), nunca quién puntó qué.

## Instrumento

Los 11 descriptores con actor `HETERO` en V25 (`D02`–`D12`; no `D01`, `D13`, `D14`).
Misma escala, borrador, envío irreversible y ventana `RECOLECCION_CALIFICACIONES` que
autoevaluación y coevaluación. Reutilizar `FormularioInstrumento` en el frontend.

Comentarios por criterio: sí van a `comentarios_estudiantes` (anonimizados, sin id de
estudiante) para el análisis de IA.

## Modelo a persistir

Migración nueva: `V26__clases_matriculas_y_estudiante.sql` (o el número que siga a V25).

| Tabla | Campos relevantes |
|-------|-------------------|
| `facultades` | `codigo` (`FC001`), unique activo |
| `programas` | `codigo` (`CU027`), unique activo por facultad |
| `usuarios` | rol `ESTUDIANTE`; `codigo` opcional; `ck_usuarios_alcance` igual que docente (`id_programa`) |
| `cursos` | `codigo` (`cod_curso`), `nombre`, `id_programa`, `es_activa` |
| `clases` | `id_periodo`, `id_curso`, `grupo`, `jornada`, `tipo_curso`, `inscritos_esperados`, unique |
| `clases_docente` | clase + docente |
| `matriculas` | clase + estudiante, `es_activa`; unique activo |

`evaluaciones_formulario`: tipo `HETERO`; unique `(clase_id, docente_id, evaluador_id)`.
AUTO y COEVAL siguen unique `(periodo, docente, tipo)` y `clase_id` nulo.

`comentarios_estudiantes.id_importacion` pasa a opcional.

## Roles y JWT

| Rol | Vínculo | Inicio |
|-----|---------|--------|
| `ESTUDIANTE` | `idPrograma` (carrera); facultad la del programa | `/evaluacion-profesores` |
| Los demás | sin cambio | sin cambio |

JWT: igual que el docente (`idPrograma`, `idFacultad`). Las clases se leen de
`matriculas` (dato del listado, no claim). Alcance: solo sus matrículas. Ajeno → 404.
Navbar del estudiante: un solo enlace, **Evaluación a profesor**.

## API del estudiante

Todas con token de `ESTUDIANTE` y `?periodo=` opcional (activo si falta).

| Método y ruta | Efecto |
|---------------|--------|
| `GET /formularios/heteroevaluacion` | Lista: clase, grupo, docente, programa, estado, avance |
| `GET /formularios/heteroevaluacion/{idClase}/{idDocente}` | 11 ítems y respuestas propias |
| `PUT` igual | Borrador |
| `POST .../enviar` | 422 incompleto; 409 ventana o ya enviado; agrega fuente `ESTUDIANTES` |

Cualquier otro rol en estas rutas: **403**. Clase en la que no está matriculado: **404**.

## Corte del CSV de notas

`POST /importaciones/csv` y `.../validar` → **409**
`"La evaluación de estudiantes se responde en el formulario"`.

`GET /importaciones` puede quedar como historial. El menú deja de ofrecer subir archivo.

## Seed de demo (cómo probar el flujo)

Recorrido con capturas de la UI: [recorrido-heteroevaluacion/FLUJO.md](recorrido-heteroevaluacion/FLUJO.md).

Archivo: `src/main/resources/seed/campus-clases-demo.json`.
El perfil `dev` lo materializa además del seed de Ingeniería/Derecho que ya existe.
Contraseña de las cuentas nuevas: `Docentia2026*`.

| Correo | Rol | Qué permite probar |
|--------|-----|--------------------|
| `karen.mancilla@uniautonoma.edu.co` | Estudiante CU027 | Tres clases: Martín (E), Zulema (A) y Francisco (A) |
| `andrea.nieto@uniautonoma.edu.co` | Estudiante CU027 | Solo ve a Zulema en el **grupo B** (misma asignatura, otra clase) |
| `zulema.leon@uniautonoma.edu.co` | Docente | Dos grupos del mismo curso; su nota `ESTUDIANTES` agrega A y B |
| `martin.valencia@uniautonoma.edu.co` | Docente | Una clase |
| `francisco.ramirez@uniautonoma.edu.co` | Docente | Electiva I, grupo A |
| `coordinacion.finanzas@uniautonoma.edu.co` | Coordinador FC001 | Cobertura y promedio; no ve puntajes individuales |

Recorrido feliz: Karen envía 5 en las dos clases → Martín queda en 5; Andrea envía 3 a Zulema B
y Karen envía 5 a Zulema A → nota de Zulema = 4,00. El CSV no debe aceptar notas nuevas.

## Códigos HTTP

| Código | Motivo |
|--------|--------|
| 200 | Lectura o borrador guardado |
| 201 | Alta de curso, clase o matrícula |
| 204 | Baja lógica de curso o matrícula |
| 400 | Validación de campo |
| 403 | Rol distinto o contraseña temporal |
| 404 | Inactivo, ajeno o inexistente |
| 409 | Etapa cerrada, duplicado, ya enviado, CSV cortado |
| 422 | Ítems HETERO incompletos o códigos ajenos al actor |

## Orden SDD (una funcionalidad por cambio)

| # | Skill | Entrega |
|---|--------|---------|
| 1 | persistencia | V26 + seed JSON. **Hecho:** `CampusClasesPersistenciaTest`, `UsuarioTest` (estudiante). Sin endpoints HETERO |
| 2 | `auth-flow` | Login estudiante, matriz letra E, alta vía `/usuarios`. **Hecho:** `SesionEstudianteTest`, `MatrizAutorizacionTest` |
| 3 | `backend-feature` | Formulario HETERO, agregado, comentarios sin importación, CSV 409. **Hecho:** `FormulariosHeteroevaluacionTest`, `ImportacionCsvTest` |
| 4 | `frontend-feature` | Navbar y pantallas del estudiante. **Hecho:** `EvaluacionProfesores`, `HeteroevaluacionClase` |
| 5 | `frontend-feature` | Quitar subida CSV del menú; cobertura en gestión si aplica. **Hecho:** menú sin carga; historial GET |

Backend de cada slice en verde antes de su pantalla.

## Fuera de esta entrega

- Sync en vivo con Campus/NOVA (el JSON es el contrato; el seed lo simula).
- Entidad Coordinación (`codCoordinacion`).
- Guardar cédula o `persona_id`.
- Formulario de estudiantes en un canal distinto a esta app.
