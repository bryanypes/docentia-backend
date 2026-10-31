# API DOCENTIA — contrato con el frontend

Endpoints agregados el 30 de octubre porque la aplicación web
([DocenteIA](https://github.com/vXro1/DocenteIA)) los necesita. Las convenciones son las de
`docs/api-sprint3-semana1.md`.

Inicio de sesión con Google: ver `docs/api-sesiones-google.md`.

---

## Contraseña temporal

La contraseña que la vicerrectoría asigna al crear una cuenta o al restablecerla
(`POST /api/usuarios`, o `PUT /api/usuarios/{id}` con `contrasena`) es **temporal**. Las cuentas que
ya existían y las del seed no quedan marcadas.

Hasta cambiarla, la sesión solo sirve para `GET /api/sesiones/actual` y
`PUT /api/sesiones/actual/contrasena`. Cualquier otra petición responde:

```json
{ "mensaje": "Debes cambiar tu contraseña temporal antes de continuar", "errores": {} }
```

con `403`. El campo nuevo `debeCambiarContrasena` llega en `usuario` al iniciar sesión, en
`GET /api/sesiones/actual` y en `UsuarioResponse`.

### `PUT /api/sesiones/actual/contrasena`

Cualquier usuario con sesión.

```json
{ "contrasenaActual": "Temporal-123", "contrasenaNueva": "Clave-propia-456" }
```

`200` con una sesión **nueva** (`{ token, usuario }`) sin la marca; el token anterior deja de servir
para el resto de la API si tenía la marca, así que hay que reemplazarlo.

| Código | Motivo |
| ------ | ------ |
| `400`  | `errores.contrasenaActual` si no coincide; `errores.contrasenaNueva` si es igual a la actual o no tiene entre 8 y 72 caracteres |
| `401`  | Sin sesión, o la cuenta ya no está activa |

---

## Activar y desactivar cuentas

### `PATCH /api/usuarios/{idUsuario}/estado`

Solo `VICERRECTOR` (los demás, `403`).

```json
{ "esActivo": false }
```

`200` con el `UsuarioResponse`. Una cuenta inactiva no puede iniciar sesión.

| Código | Motivo |
| ------ | ------ |
| `400`  | Falta `esActivo` (`errores.esActivo`) |
| `404`  | El usuario no existe |
| `409`  | `"No puedes desactivar tu propia cuenta"` |

**Las sesiones abiertas de esa cuenta siguen vivas hasta que el token vence (8 h)**, igual que con un
cambio de rol: el token no se consulta contra la base en cada petición.

---

## Seguimiento de docentes

### `GET /api/docentes/seguimiento?idFacultad=`

Roles: `COORDINADOR` (siempre su facultad; `idFacultad` se ignora) y `VICERRECTOR` (todas, o la del
filtro). `DOCENTE` → `403`.

```json
{
  "periodo": { "idPeriodo": 2, "nombre": "2026-2" },
  "docentes": [
    {
      "docente": { "idDocente": 4, "nombreCompleto": "Julián Camilo Hoyos", "correo": "…",
                   "idPrograma": 1, "nombrePrograma": "Ingeniería de Software",
                   "idFacultad": 1, "nombreFacultad": "Facultad de Ingeniería" },
      "promedio": { "estado": "COMPLETO", "valor": 4.34, "fuentesPendientes": [] }
    }
  ]
}
```

- Solo trae el promedio ponderado y las fuentes pendientes, nunca las notas por fuente.
- Sin periodo activo, `periodo` y cada `promedio` llegan en `null`.
