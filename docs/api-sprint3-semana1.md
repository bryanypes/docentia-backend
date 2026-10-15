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
