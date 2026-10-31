# DOCENTIA

Sistema de evaluación docente de la Corporación Universitaria Autónoma del Cauca.

| Carpeta             | Contenido                                   |
| ------------------- | ------------------------------------------- |
| `docentia-backend/` | API REST: Spring Boot, Java 21 y PostgreSQL |
| `DocenteIA/`        | Aplicación web: React con Vite              |

## Levantar todo con Docker

Solo necesitas Docker Desktop abierto:

```bash
docker compose up --build -d
```

| Servicio   | Dirección                     |
| ---------- | ----------------------------- |
| Aplicación | <http://localhost:5173>       |
| API        | <http://localhost:8080/api>   |
| PostgreSQL | `localhost:5433`              |

Los usuarios de demo están en `docentia-backend/README.md`; su contraseña es `Docentia2026*`.

```bash
docker compose logs -f api   # ver el arranque del backend
docker compose down          # apagar y conservar los datos
docker compose down -v       # apagar y borrar los datos; al subir vuelve el seed
```

Si algún puerto está ocupado, copia `.env.example` como `.env` y cambia `POSTGRES_PORT`, `API_PORT`
o `WEB_PORT`. El archivo `.env` no se sube al repositorio.

El recorrido de demo está en `DocenteIA/docs/checklist-demo-sprint3.md`, y el contrato de la API en
`docentia-backend/docs/`.

## Ramas

Se trabaja con GitFlow: `main` y `develop` son permanentes y cada historia sale de `develop` en una
rama `feature/{id}-{descripcion-corta}`.
