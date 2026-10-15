# API DOCENTIA — Sprint 3, semana 1 (14–20 oct)

Endpoints nuevos o que cambiaron en esta semana. Todo lo que no aparece aquí sigue igual.

## Convenciones

- Base: `http://localhost:8080/api`. En el frontend, Vite reenvía `/api` al backend.
- Autenticación: `Authorization: Bearer <token>` con el token de `POST /api/sesiones`.
- Fechas `yyyy-MM-dd`; fechas con hora `yyyy-MM-ddTHH:mm:ss` (hora de Colombia).
- Los decimales llegan como números JSON (`4.34`), nunca como texto.
- Todo error responde con el mismo cuerpo:

  ```json
  { "mensaje": "Texto para mostrar al usuario", "errores": { "campo": "motivo" } }
  ```

  `errores` viene vacío (`{}`) cuando el problema no es de un campo concreto.

| Código | Cuándo                                                                                |
| ------ | ------------------------------------------------------------------------------------- |
| 400    | Datos inválidos (campos en `errores`) o JSON ilegible                                  |
| 401    | Sin token, token vencido o alterado                                                   |
| 403    | El rol no tiene permiso para la acción                                                |
| 404    | No existe **o está fuera del alcance** del usuario (mismo mensaje en los dos casos)  |
| 409    | Choca con el estado actual: duplicado, etapa cerrada, regla de negocio                |
| 413    | Archivo demasiado grande                                                              |
| 422    | Archivo o mapeo de columnas que no se puede importar                                  |

---

## K1 — Usuarios con rol y alcance

Un usuario tiene **un solo rol**. El vínculo depende del rol:

| Rol           | `idFacultad`   | `idPrograma`   |
| ------------- | -------------- | -------------- |
| `COORDINADOR` | obligatorio    | no se envía    |
| `DOCENTE`     | opcional (*)   | obligatorio    |
| `VICERRECTOR` | no se envía    | no se envía    |

(*) La facultad del docente es la de su programa. Si el formulario la envía, debe coincidir con la
del programa; si no coincide responde 400.

El alcance ya viaja en el login (`POST /api/sesiones` → `usuario.rol`, `usuario.idFacultad`,
`usuario.idPrograma`) y dentro del JWT; el backend filtra con eso sin volver a consultar la base.
**Si se edita el rol o el vínculo de alguien, su sesión abierta conserva el alcance anterior hasta
que el token vence (8 h) o vuelve a iniciar sesión.**

Todos los endpoints de `/usuarios` son solo para `VICERRECTOR` (no existe rol administrador).
Coordinador y docente reciben 403.

### `GET /api/usuarios?rol=`

`rol` opcional (`COORDINADOR`, `VICERRECTOR`, `DOCENTE`). Responde `200` con una lista de
`UsuarioResponse`, ordenada por nombre:

```json
[
  {
    "idUsuario": 4,
    "nombreCompleto": "Julián Camilo Hoyos",
    "correo": "julian.hoyos@uniautonoma.edu.co",
    "rol": "DOCENTE",
    "idFacultad": 1,
    "nombreFacultad": "Facultad de Ingeniería",
    "idPrograma": 1,
    "nombrePrograma": "Ingeniería de Software",
    "esActivo": true
  }
]
```

### `GET /api/usuarios/{idUsuario}`

`200` con un `UsuarioResponse`; `404` si no existe.

### `POST /api/usuarios`

```json
{
  "nombreCompleto": "Ana Ruiz",
  "correo": "ana.ruiz@uniautonoma.edu.co",
  "rol": "COORDINADOR",
  "idFacultad": 1,
  "idPrograma": null,
  "contrasena": "minimo-8-caracteres"
}
```

`201` con el `UsuarioResponse`. El correo se guarda en minúsculas.

| Código | Motivo                                                                                |
| ------ | ------------------------------------------------------------------------------------- |
| 400    | Falta nombre, correo, rol o contraseña; correo mal formado; contraseña de menos de 8 caracteres; vínculo que no corresponde al rol (ver tabla) |
| 404    | `idFacultad` o `idPrograma` no existen                                                |
| 409    | Ya existe un usuario con ese correo (sin distinguir mayúsculas)                        |

La contraseña es temporal: el login con correo y contraseña existe mientras llega el SSO.

### `PUT /api/usuarios/{idUsuario}`

Mismo cuerpo que `POST`. `contrasena` es opcional: si llega vacía o nula se conserva la actual.
Permite cambiar el rol; el vínculo anterior se reemplaza. `200` con el `UsuarioResponse`. Errores
iguales a `POST`, más `404` si el usuario no existe.

---

## B2 — Etapas del periodo y ventanas

### Cambio que rompe el frontend: nombres de las etapas

`tipoEtapa` ahora solo acepta estos cuatro valores (en `POST`/`PUT /api/periodos-academicos` y en
las respuestas). Hay que actualizar `src/utils/etapasPeriodo.js` y su test:

| `tipoEtapa`                  | Nombre para mostrar            | Acciones que bloquea              |
| ---------------------------- | ------------------------------ | --------------------------------- |
| `RECOLECCION_CALIFICACIONES` | Recolección de calificaciones  | Calificar (coordinación y vicerrectoría), cargar CSV de estudiantes |
| `REVISION_HALLAZGOS`         | Revisión de hallazgos          | (historia E, pendiente)           |
| `AGENDA_CITAS`               | Agenda de citas                | (historia F, pendiente)           |
| `FIRMA_ACTAS`                | Firma de actas                 | (historia G, pendiente)           |

Los valores viejos (`EVALUACION_ESTUDIANTES`, `CALIFICACION_COORDINACION`,
`CALIFICACION_VICERRECTORIA`, `RETROALIMENTACION`) responden 400. En las bases existentes la
migración V10 juntó las tres etapas de calificación en la recolección (de la primera apertura al
último cierre) y convirtió la retroalimentación en revisión de hallazgos.

Las etapas siguen siendo opcionales al crear el periodo; una etapa sin fechas cuenta como cerrada.
Las fechas son inclusivas y se comparan contra la fecha de Colombia.

Solo hay un periodo activo: activar uno desactiva el anterior (sin cambios).

### `GET /api/periodos-academicos/activo/ventanas`

Cualquier rol autenticado. Sirve para habilitar o deshabilitar botones según la fecha.

```json
[
  {
    "etapa": "RECOLECCION_CALIFICACIONES",
    "nombreEtapa": "Recolección de calificaciones",
    "fechaApertura": "2026-09-28",
    "fechaCierre": "2026-11-13",
    "abierta": true
  },
  { "etapa": "REVISION_HALLAZGOS", "nombreEtapa": "Revisión de hallazgos",
    "fechaApertura": "2026-11-16", "fechaCierre": "2026-11-20", "abierta": false },
  { "etapa": "AGENDA_CITAS", "nombreEtapa": "Agenda de citas",
    "fechaApertura": null, "fechaCierre": null, "abierta": false },
  { "etapa": "FIRMA_ACTAS", "nombreEtapa": "Firma de actas",
    "fechaApertura": "2026-11-30", "fechaCierre": "2026-12-04", "abierta": false }
]
```

Siempre llegan las cuatro, en ese orden. `409` si no hay periodo activo.

### Respuesta cuando la etapa está cerrada (409)

Cualquier acción bloqueada por fecha (hoy: `PUT /api/docentes/{id}/calificaciones/coordinacion`,
`PUT /api/docentes/{id}/calificaciones/vicerrectoria` y la carga de CSV) responde:

```json
{
  "mensaje": "La etapa Recolección de calificaciones del periodo 2026-2 está abierta del 2026-10-21 al 2026-10-30; hoy es 2026-10-20",
  "errores": {},
  "etapa": "RECOLECCION_CALIFICACIONES",
  "nombreEtapa": "Recolección de calificaciones",
  "fechaApertura": "2026-10-21",
  "fechaCierre": "2026-10-30",
  "fechaActual": "2026-10-20"
}
```

Si la etapa no está programada, `fechaApertura` y `fechaCierre` llegan `null` y el mensaje dice que
la vicerrectoría debe programarla. El 404 de alcance tiene prioridad: un coordinador que intenta
calificar a un docente de otra facultad recibe 404 aunque la etapa esté cerrada.

**Para las historias E, F y G:** el bloqueo es una sola línea en el servicio,
`ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.X)`, antes de modificar datos.
