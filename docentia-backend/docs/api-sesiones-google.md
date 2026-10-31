# Inicio de sesión con Google

Complementa `POST /api/sesiones` (correo y contraseña). El frontend obtiene un **ID token** con
[Google Identity Services](https://developers.google.com/identity/gsi/web) y lo envía al API.

## Configuración

| Variable | Dónde | Uso |
| -------- | ----- | --- |
| `DOCENTIA_GOOGLE_CLIENT_ID` | Backend | Valida la audiencia del ID token |
| `VITE_GOOGLE_CLIENT_ID` | Build del frontend | Mismo Client ID de la consola OAuth |

Si `DOCENTIA_GOOGLE_CLIENT_ID` está vacío, `POST /api/sesiones/google` responde **503** y el botón
de Google no debe mostrarse en la UI.

El **Client Secret** no se usa en este flujo (solo ID token en el navegador).

## `POST /api/sesiones/google`

Público (sin JWT).

```json
{ "idToken": "<JWT de Google>" }
```

`201` con el mismo cuerpo que `POST /api/sesiones`:

```json
{ "token": "<JWT DOCENTIA>", "usuario": { "...": "..." } }
```

Reglas:

- El correo del token debe estar verificado por Google (`email_verified`).
- Solo se aceptan correos que terminan en `@uniautonoma.edu.co` (sin distinguir mayúsculas).
- Debe existir un usuario **activo** en DOCENTIA con ese correo. No se crean cuentas automáticamente.
- `debeCambiarContrasena` y el alcance en el JWT se comportan igual que en el login por contraseña.

| Código | Motivo |
| ------ | ------ |
| `400`  | Falta `idToken` o cuerpo ilegible |
| `401`  | Token inválido, dominio no institucional, correo no registrado o cuenta inactiva (mensaje genérico) |
| `503`  | Google no configurado en el servidor |

## Google Cloud (resumen)

1. Pantalla de consentimiento OAuth (Interno si hay Workspace `@uniautonoma.edu.co`).
2. Credencial **OAuth 2.0 — Aplicación web**.
3. Orígenes JavaScript autorizados: `http://localhost:5173` y la URL de producción.
4. Copiar el **Client ID** a las variables anteriores (nunca versionar secretos).
