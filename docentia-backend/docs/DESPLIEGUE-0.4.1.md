# Despliegue de DOCENTIA 0.4.1

0.4.1 se instala sobre 0.4.0. Si el servidor todavía está en 0.3.3, primero sigue `DESPLIEGUE-0.4.0.md` (respaldo, migraciones y primer administrador) y en el paso d usa `v0.4.1` en lugar de `v0.4.0`. Lo demás de esa guía no cambia.

## Qué trae

- Login con conteo de intentos, cronómetro del bloqueo, aviso de correo externo y enlaces de ayuda a NOVA.
- Página de inicio compacta, página `/tutoriales` y estados de «sin docentes» y «sin materias».
- Corrección: el cálculo del bloqueo podía responder 500 si otra petición del mismo correo entraba al mismo tiempo.
- **Sin migraciones:** la base sigue en V43. No hace falta respaldo nuevo si ya se hizo el de 0.4.0, aunque se recomienda.

## Regla principal: backend y frontend juntos

**Backend y frontend se actualizan JUNTOS y desde la misma etiqueta `v0.4.1`.** El login nuevo lee `intentosUsados` e `intentosMaximos` del 401 y `segundosParaReintentar` del 429. Si cambia solo uno de los dos:

- Frontend 0.4.1 con backend 0.4.0: no muestra los intentos ni el cronómetro.
- Backend 0.4.1 con frontend 0.4.0: el frontend ignora los campos nuevos y pierde las mejoras.

Ninguna de las dos combinaciones rompe el ingreso, pero deja la versión a medias. No se despliega así.

## Qué commit debe tener cada repositorio

Todos parten del mismo contenido. La etiqueta `v0.4.1` es anotada, con el mensaje «Versión 0.4.1», y existe en los cuatro repositorios. `v0.4.0` no se movió.

| Repositorio | Qué contiene | Qué desplegar |
|---|---|---|
| `bryanypes/docentia-backend` (monorepo) | `docentia-backend/` y `DocenteIA/` | etiqueta `v0.4.1` |
| `bryanypes/docentia-docker` | `backend/` y `frontend/`, con el `docker-compose.yml` de despliegue | etiqueta `v0.4.1` |
| `vXro1/DocenteIA` | solo el frontend | etiqueta `v0.4.1` |
| `bryanypes/docentia-frontend` | solo el frontend | etiqueta `v0.4.1` |

Los SHA exactos de cada etiqueta están en la sección «SHA publicados» al final de esta guía.

Ningún repositorio usa submódulos de git: `docentia-docker` copia las carpetas. Si el servidor tiene su propio repositorio con submódulos que apuntan a estos, cada submódulo debe quedar en el commit de `v0.4.1` de su repositorio:

```bash
cd <submodulo-del-backend>  && git fetch --tags origin && git checkout v0.4.1
cd <submodulo-del-frontend> && git fetch --tags origin && git checkout v0.4.1
cd <repositorio-del-servidor> && git add <submodulo-del-backend> <submodulo-del-frontend>
git submodule status   # los dos deben mostrar el SHA de v0.4.1 de su repositorio
```

Para comprobar el commit en cualquier clon: `git rev-parse v0.4.1^{commit}`.

## Variable nueva

| Variable | Valor |
|---|---|
| `VITE_URL_NOVA` | Opcional. Portal de ayuda de los enlaces «Solicitar ayuda». Vacía: `https://nova.uniautonoma.edu.co` |

Es variable de construcción del frontend: si cambia, hay que reconstruir `web`.

## Pasos

```bash
git fetch --tags origin
git checkout v0.4.1
docker compose up -d --build --wait api web
sh scripts/humo-postdespliegue.sh https://nova.uniautonoma.edu.co/Evadocente-app
```

- El arranque de `api` no aplica migraciones: el log dice `Schema "public" is up to date` y `Started DocentiaApplication`.
- El script de humo debe terminar sin fallas y mostrar `versión publicada: 0.4.1`.
- A mano: un login con contraseña equivocada dice «Llevas 1 de 5 intentos» y no revela si el correo existe.

## Vuelta atrás

Como no hay migraciones, basta con volver a la etiqueta anterior y reconstruir los dos servicios juntos:

```bash
git checkout v0.4.0
docker compose up -d --build --wait api web
```

No se usa `docker compose down -v`: borra el volumen de la base.

## SHA publicados

Se completa al crear las etiquetas.
