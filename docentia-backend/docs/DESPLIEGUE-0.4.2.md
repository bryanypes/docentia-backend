# Despliegue de DOCENTIA 0.4.2

0.4.2 se instala sobre 0.4.1. Si el servidor está en una versión anterior, primero sigue `DESPLIEGUE-0.4.1.md` (y, si viene de 0.3.3, `DESPLIEGUE-0.4.0.md`), usando `v0.4.2` donde esas guías dicen otra etiqueta.

## Qué trae

- Al enviar una evaluación (heteroevaluación, autoevaluación o coevaluación) ya no queda el asistente de pasos. Se ve solo «Tu evaluación ha sido recibida», con el resumen por criterio y **Volver al inicio**. Lo mismo al abrir una evaluación ya enviada.
- **Sin migraciones ni variables nuevas.** La base sigue en V43 y el backend no cambia de comportamiento; solo sube su número de versión.

## Regla principal: backend y frontend juntos

**Backend y frontend se actualizan JUNTOS y desde la misma etiqueta `v0.4.2`**, igual que en 0.4.1. Así el número de versión que publica la API coincide con la web desplegada.

## Qué commit debe tener cada repositorio

La etiqueta `v0.4.2` es anotada, con el mensaje «Versión 0.4.2», y existe en los cuatro repositorios. `v0.4.0` y `v0.4.1` no se movieron.

| Repositorio | Qué contiene | Qué desplegar |
|---|---|---|
| `bryanypes/docentia-backend` (monorepo) | `docentia-backend/` y `DocenteIA/` | etiqueta `v0.4.2` |
| `bryanypes/docentia-docker` | `backend/` y `frontend/`, con el `docker-compose.yml` de despliegue | etiqueta `v0.4.2` |
| `vXro1/DocenteIA` | solo el frontend | etiqueta `v0.4.2` |
| `bryanypes/docentia-frontend` | solo el frontend | etiqueta `v0.4.2` |

Los SHA exactos están en la sección «SHA publicados» al final de esta guía y en la nota de la versión v0.4.2 en GitHub.

Si el servidor usa submódulos que apuntan a estos repositorios, cada submódulo debe quedar en el commit de `v0.4.2` de su repositorio (los comandos están en `DESPLIEGUE-0.4.1.md`). Para comprobarlo en cualquier clon: `git rev-parse v0.4.2^{commit}`.

## Pasos

```bash
git fetch --tags origin
git checkout v0.4.2
docker compose up -d --build --wait api web
sh scripts/humo-postdespliegue.sh https://nova.uniautonoma.edu.co/Evadocente-app
```

- El arranque de `api` no aplica migraciones: el log dice `Schema "public" is up to date`.
- El script de humo debe terminar sin fallas y mostrar `versión publicada: 0.4.2`.
- A mano: un docente que ya envió su autoevaluación ve «Tu evaluación ha sido recibida» al abrir **Mi autoevaluación**.

## Vuelta atrás

Sin migraciones, basta con volver a la etiqueta anterior y reconstruir los dos servicios juntos:

```bash
git checkout v0.4.1
docker compose up -d --build --wait api web
```

No se usa `docker compose down -v`: borra el volumen de la base.

## SHA publicados

| Repositorio | Etiqueta | Commit (SHA) | Rama `main` |
|---|---|---|---|
| `bryanypes/docentia-backend` (monorepo) | `v0.4.2` | `afe2850e3303cd27d65bfbb3f77c776d60d2c919` | mismo commit |
| `bryanypes/docentia-docker` | `v0.4.2` | `e0b103fb849ce261518041e0364e2f5ba8a75e2e` | mismo commit |
| `vXro1/DocenteIA` | `v0.4.2` | `f02d6f2fb2510b0dc1323a0dd2a6b99e3f45c5e6` | mismo commit |
| `bryanypes/docentia-frontend` | `v0.4.2` | `9584aac53081f7ed2b3405c5ce9f151de5e23a72` | mismo commit |

Si el servidor usa submódulos, el del backend apunta a `afe2850` (monorepo) o a `e0b103f` (docentia-docker), y el del frontend a `f02d6f2` (vXro1/DocenteIA) o a `9584aac` (docentia-frontend), según de cuál repositorio se tome. Backend y frontend se actualizan juntos desde `v0.4.2`.

`develop` tiene, además, el commit que agregó esta tabla (solo documentación). Lo que se despliega es la etiqueta.
