# Operación

## Levantar el sistema completo

Requiere Docker Desktop. Esta es la forma soportada:

```bash
git clone https://github.com/bryanypes/docentia-backend.git
cd docentia-backend
docker compose up --build -d
```

| Servicio | Dirección |
|----------|-----------|
| Aplicación | <http://localhost:5173> |
| API | <http://localhost:8080/api> |
| Correos de desarrollo (Mailpit) | <http://localhost:8025> |
| PostgreSQL | `localhost:5433` |

## Desarrollo sin Docker

```bash
# Backend, con JDK 21 y un PostgreSQL local
cd docentia-backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Frontend, en el repositorio DocenteIA
DOCENTIA_API_URL=http://localhost:8080 npm run dev
```

El frontend vive en el repositorio `DocenteIA`. La carpeta `DocenteIA/` del monorepo es la copia que usa
Docker y se sincroniza en cada versión. `docentia-docker` reúne el backend y el frontend de cada versión publicada.

## Usuarios de demo

Contraseña: `Docentia2026*`.

| Rol | Correo |
|-----|--------|
| Vicerrectoría | `vicerrectoria@uniautonoma.edu.co` |
| Coordinación | `coordinacion.ingenieria@uniautonoma.edu.co` |
| Docente con nota 2,85 | `nora.munoz@uniautonoma.edu.co` |
| Docente con comentarios | `julian.hoyos@uniautonoma.edu.co` |

## Primer usuario en producción

Fuera del perfil `dev` no hay seed, y solo la vicerrectoría crea usuarios. El primer vicerrector se crea
desde el entorno, una sola vez:

| Variable | Obligatoria | Regla |
|----------|-------------|-------|
| `DOCENTIA_BOOTSTRAP_EMAIL` | Sí, para activar el arranque | Correo que termine en `@uniautonoma.edu.co`; se guarda en minúsculas |
| `DOCENTIA_BOOTSTRAP_NOMBRE` | Sí | Hasta 150 caracteres |
| `DOCENTIA_BOOTSTRAP_CONTRASENA` | No | De 12 a 72 caracteres. Queda **temporal**: al entrar solo se puede cambiar. Sin ella la cuenta entra solo con Google, y entonces `DOCENTIA_GOOGLE_CLIENT_ID` es obligatorio |

1. Pon las tres variables en el `.env` del servidor (nunca en el repositorio) y arranca la API.
2. Si la tabla de usuarios está vacía, se crea un `VICERRECTOR` y el log dice
   `Se creó el primer usuario de vicerrectoría (id N)`, sin correo ni contraseña.
3. Entra con ese correo y cambia la contraseña. Desde **Usuarios** crea el resto de cuentas.
4. Quita las variables `DOCENTIA_BOOTSTRAP_*` del `.env`. Si se quedan, en cada arranque el log avisa
   `ya hay usuarios: no se crea nada`; nunca se crea un segundo usuario ni se cambia una clave.

Una configuración inválida (correo externo, contraseña corta, sin contraseña y sin Google) detiene el
arranque con un mensaje que nombra la variable y nunca repite su valor. Dos réplicas que arrancan a la
vez no crean dos usuarios: la creación toma un candado de PostgreSQL. Pruebas: `PrimerUsuarioTest` y
`PrimerUsuarioValidacionTest`.

## Variables de entorno

| Variable | Obligatoria en producción | Uso |
|----------|---------------------------|-----|
| `SPRING_PROFILES_ACTIVE` | Sí, `prod` | `dev` carga datos de ejemplo |
| `DOCENTIA_JWT_SECRETO` | Sí, 32 bytes o más | Firma de sesiones |
| `DOCENTIA_DB_URL`, `DOCENTIA_DB_USUARIO`, `DOCENTIA_DB_CONTRASENA` | Sí | Base de datos |
| `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE`, `_CONTRASENA` | Solo el primer arranque | Primer vicerrector (ver arriba) |
| `DOCENTIA_DB_POOL_MAXIMO` | No, 15 | Pool de conexiones |
| `DOCENTIA_CORS_ORIGENES` | Si hay otro dominio | Orígenes exactos |
| `DOCENTIA_APP_URL` | Sí | Enlaces de los correos |
| `DOCENTIA_MAIL_HOST`, `_PORT`, `_USER`, `_PASS`, `_FROM`, `_TLS` | Para enviar correo | SMTP |
| `DOCENTIA_COMPROMISOS_UMBRAL` | No, 3.8 | Umbral de compromisos |
| `DOCENTIA_ACTAS_ROL_CORRECTOR` | No, `VICERRECTOR` | Quién corrige actas |
| `DOCENTIA_IA_*` | No | Proveedor de análisis |
| `DOCENTIA_API_URL` | Solo en desarrollo | Proxy de Vite hacia el backend |
| `VITE_GOOGLE_CLIENT_ID`, `DOCENTIA_GOOGLE_CLIENT_ID` | Para Google | Inicio de sesión |

## Respaldo y restauración

```bash
sh scripts/respaldar.sh respaldo.dump
sh scripts/restaurar.sh respaldo.dump
```

Restaurar reemplaza la base y reinicia la API.

## Salud

```bash
curl http://localhost:8080/api/actuator/health/readiness
curl http://localhost:8080/api/actuator/health/liveness
```

## Inicio de sesión con Google

Fuera de `dev` los estudiantes no tienen contraseña utilizable: la matrícula por CSV les asigna una
aleatoria que nadie conoce. **Sin Google ningún estudiante puede entrar a evaluar.** El personal puede
entrar con correo y contraseña.

| Qué | Dónde | Nota |
|-----|-------|------|
| Cliente OAuth 2.0 tipo **Aplicación web** | Google Cloud Console de la universidad | No se usa el secreto `GOCSPX-…`; solo el Client ID |
| Orígenes de JavaScript autorizados | Mismo cliente | La URL pública exacta de la web, con esquema y sin barra final (por ejemplo `https://docentia.uniautonoma.edu.co`; **por confirmar** la URL real). `http://localhost:5173` solo para pruebas |
| `DOCENTIA_GOOGLE_CLIENT_ID` | `.env` del servidor, servicio `api` | La API valida que el token venga para ese cliente |
| `VITE_GOOGLE_CLIENT_ID` | `.env` del servidor, **argumento de build** del servicio `web` | Mismo valor. Se incrusta al compilar: cambiarlo exige `docker compose build web` |

La API acepta el token de Google solo si:

1. la firma y la audiencia (`DOCENTIA_GOOGLE_CLIENT_ID`) son válidas,
2. el correo está verificado por Google (`email_verified`),
3. el correo termina en `@uniautonoma.edu.co`, y
4. ese correo ya existe y está activo en DOCENTIA (el estudiante entra por la matrícula, el personal
   lo crea vicerrectoría).

En cualquier otro caso responde 401 con el mismo mensaje que una contraseña errada.

**Si falta la configuración:**

| Falta | Qué pasa |
|-------|----------|
| `VITE_GOOGLE_CLIENT_ID` | La pantalla de inicio no muestra el botón de Google |
| `DOCENTIA_GOOGLE_CLIENT_ID` | `POST /api/sesiones/google` responde **503** "El inicio de sesión con Google no está configurado en este ambiente". Con el perfil `prod`, la API escribe al arrancar un aviso `Falta DOCENTIA_GOOGLE_CLIENT_ID: los estudiantes no podrán iniciar sesión…` |
| El origen en Google Cloud | Google rechaza el botón con `origin_mismatch` en el navegador; la API no recibe nada |

La CSP de `nginx.conf` ya permite `accounts.google.com`. Pruebas: `SesionGoogleControllerTest`,
`SesionGoogleSinConfiguracionTest`, `ValidacionProduccionTest`. Con un cliente real de la universidad
**no se ha probado**: queda por confirmar en el ensayo del despliegue.

## Solución de problemas

| Síntoma | Causa y solución |
|---------|------------------|
| `port is already allocated` | Copia `.env.example` como `.env` y cambia `API_PORT`, `WEB_PORT`, `POSTGRES_PORT` o `MAILPIT_PORT` |
| La API no arranca en `prod` | Falta `DOCENTIA_JWT_SECRETO` o tiene un valor de desarrollo |
| La firma responde 409 | Falta la cita, el promedio no está completo o la etapa está cerrada; el mensaje lo dice |
| No llegan correos | Revisa Mailpit. Los envíos salen cada 15 segundos y se reintentan cinco veces |
| Quieres repetir la demo | `docker compose down -v` y vuelve a levantar |
| Un error 5xx en pantalla | Busca el código de soporte en `docker compose logs api` |
