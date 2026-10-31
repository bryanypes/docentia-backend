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
Docker y se sincroniza en cada versión. `docentia-docker` ya no se usa.

## Usuarios de demo

Contraseña: `Docentia2026*`.

| Rol | Correo |
|-----|--------|
| Vicerrectoría | `vicerrectoria@uniautonoma.edu.co` |
| Coordinación | `coordinacion.ingenieria@uniautonoma.edu.co` |
| Docente con nota 2,85 | `nora.munoz@uniautonoma.edu.co` |
| Docente con comentarios | `julian.hoyos@uniautonoma.edu.co` |

## Variables de entorno

| Variable | Obligatoria en producción | Uso |
|----------|---------------------------|-----|
| `SPRING_PROFILES_ACTIVE` | Sí, `prod` | `dev` carga datos de ejemplo |
| `DOCENTIA_JWT_SECRETO` | Sí, 32 bytes o más | Firma de sesiones |
| `DOCENTIA_DB_URL`, `DOCENTIA_DB_USUARIO`, `DOCENTIA_DB_CONTRASENA` | Sí | Base de datos |
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
