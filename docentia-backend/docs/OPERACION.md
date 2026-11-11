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

## Solución de problemas

| Síntoma | Causa y solución |
|---------|------------------|
| `port is already allocated` | Copia `.env.example` como `.env` y cambia `API_PORT`, `WEB_PORT`, `POSTGRES_PORT` o `MAILPIT_PORT` |
| La API no arranca en `prod` | Falta `DOCENTIA_JWT_SECRETO` o tiene un valor de desarrollo |
| La firma responde 409 | Falta la cita, el promedio no está completo o la etapa está cerrada; el mensaje lo dice |
| No llegan correos | Revisa Mailpit. Los envíos salen cada 15 segundos y se reintentan cinco veces |
| Quieres repetir la demo | `docker compose down -v` y vuelve a levantar |
| Un error 5xx en pantalla | Busca el código de soporte en `docker compose logs api` |
