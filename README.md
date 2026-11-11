# DOCENTIA

[![CI](https://github.com/bryanypes/docentia-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/bryanypes/docentia-backend/actions/workflows/ci.yml)

Sistema de evaluación docente de la Corporación Universitaria Autónoma del Cauca.

| Carpeta             | Contenido                                   |
| ------------------- | ------------------------------------------- |
| `docentia-backend/` | API REST: Spring Boot, Java 21 y PostgreSQL |
| `DocenteIA/`        | Aplicación web: React con Vite              |

## Levantar todo con Docker

Solo necesitas Docker Desktop abierto:

```bash
git clone -b main https://github.com/bryanypes/docentia-backend.git
cd docentia-backend
docker compose up --build -d
```

| Servicio   | Dirección                     |
| ---------- | ----------------------------- |
| Aplicación | <http://localhost:5173>       |
| API        | <http://localhost:8080/api>   |
| PostgreSQL | `localhost:5433`              |
| Correos (Mailpit) | <http://localhost:8025> |

Los usuarios de demo están en `docentia-backend/README.md`; su contraseña es `Docentia2026*`. Para
el flujo de actas, entra con `coordinacion.ingenieria@uniautonoma.edu.co` y `nora.munoz@uniautonoma.edu.co`,
y para correcciones con `vicerrectoria@uniautonoma.edu.co`. Para la evaluación de estudiantes, `estudiante.demo5@uniautonoma.edu.co`
(dos evaluaciones pendientes) y la docente `laura.demo@uniautonoma.edu.co`. Estas cuentas existen solo
en el perfil de desarrollo.

En desarrollo sin Docker, el frontend apunta al backend con `DOCENTIA_API_URL`
(`DOCENTIA_API_URL=http://localhost:8081 npm run dev`). El backend usa el perfil `dev` para cargar
los datos de ejemplo. `DOCENTIA_COMPROMISOS_UMBRAL` y `DOCENTIA_ACTAS_ROL_CORRECTOR` configuran las
reglas de la Resolución 074.

```bash
docker compose logs -f api   # ver el arranque del backend
docker compose down          # apagar y conservar los datos
docker compose down -v       # apagar y borrar los datos; al subir vuelve el seed
```

Si algún puerto está ocupado, copia `.env.example` como `.env` y cambia `POSTGRES_PORT`, `API_PORT`,
`WEB_PORT` o `MAILPIT_PORT`. El archivo `.env` no se sube al repositorio.

**Windows y puertos reservados.** Hyper-V y WSL reservan rangos de puertos que cambian al reiniciar. Si
Docker dice `An attempt was made to access a socket in a way forbidden by its access permissions` o
`ports are not available`, el puerto no está ocupado sino reservado. Para ver los rangos:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

Elige en `.env` puertos fuera de esos rangos (por ejemplo `API_PORT=18080`, `WEB_PORT=18173`,
`POSTGRES_PORT=6543`) y vuelve a levantar.

**Roles.** Hay cuatro: vicerrectoría, coordinación, docente y estudiante. Los roles Administrador y
Visualizador no existen en `develop` (están en el PR #12, sin fusionar).

Los correos de desarrollo se ven en <http://localhost:8025>. La operación completa está en
`docentia-backend/docs/OPERACION.md`; la arquitectura en `ARQUITECTURA.md`, la API en `API.md`, la
seguridad en `SEGURIDAD.md`, las notificaciones en `NOTIFICACIONES.md` y los límites en `LIMITACIONES.md`.

El recorrido de demo está en `DocenteIA/docs/checklist-demo-sprint3.md` y el contrato de la API en
`docentia-backend/docs/`. Las decisiones y la trazabilidad con la Resolución 074 están en
`docentia-backend/docs/DECISIONES.md` y `docentia-backend/docs/RES074-TRAZABILIDAD.md`.

## Ramas

Se trabaja con GitFlow: `main` y `develop` son permanentes y cada historia sale de `develop` en una
rama `feature/{id}-{descripcion-corta}`.
