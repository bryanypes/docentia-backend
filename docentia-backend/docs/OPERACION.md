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

La fuente del frontend es la carpeta `DocenteIA/` de este monorepo: ahí se desarrolla, la prueba el CI y
la construye Docker. El repositorio `DocenteIA` es un espejo que se actualiza al publicar una versión.
`docentia-docker` reúne backend y frontend de una versión publicada; hoy está en 0.3.2 y no tiene la
heteroevaluación ni el modelo local, así que no sirve para desplegar.

## Usuarios de demo

Contraseña: `Docentia2026*`.

| Rol | Correo |
|-----|--------|
| Vicerrectoría | `vicerrectoria@uniautonoma.edu.co` |
| Coordinación | `coordinacion.ingenieria@uniautonoma.edu.co` |
| Docente con nota 2,85 | `nora.munoz@uniautonoma.edu.co` |
| Docente con comentarios | `julian.hoyos@uniautonoma.edu.co` |

## Primer administrador (paso obligatorio del despliegue)

Fuera del perfil `dev` no hay seed y solo la **administración** crea usuarios. Si no existe ningún
ADMINISTRADOR activo, el primero se crea desde el entorno:

| Variable | Obligatoria | Regla |
|----------|-------------|-------|
| `DOCENTIA_BOOTSTRAP_EMAIL` | Sí, para activar el arranque | Correo que termine en `@uniautonoma.edu.co`; se guarda en minúsculas |
| `DOCENTIA_BOOTSTRAP_NOMBRE` | Sí | Hasta 150 caracteres |
| `DOCENTIA_BOOTSTRAP_CONTRASENA` | No | De 12 a 72 caracteres. Queda **temporal**: al entrar solo se puede cambiar. Sin ella la cuenta entra solo con Google, y entonces `DOCENTIA_GOOGLE_CLIENT_ID` es obligatorio |

1. Pon las tres variables en el `.env` del servidor (nunca en el repositorio) y arranca la API.
2. Si no hay ningún administrador activo, se crea uno y el log dice
   `Se creó el primer administrador (id N)`, sin correo ni contraseña.
3. Entra con ese correo, cambia la contraseña y, desde **Usuarios**, crea las cuentas de vicerrectoría,
   coordinación, docentes y visualización.
4. Quita las variables `DOCENTIA_BOOTSTRAP_*` del `.env`. Si se quedan, en cada arranque el log avisa
   `ya hay un administrador: no se crea nada`.

Una configuración inválida detiene el arranque con un mensaje que nombra la variable sin repetir su
valor. Dos réplicas que arrancan a la vez no crean dos administradores: la creación toma un candado de
PostgreSQL. Pruebas: `PrimerUsuarioTest` y `PrimerUsuarioValidacionTest`. Roles y permisos:
[ROLES.md](ROLES.md).

## Publicación bajo una subruta (proxy inverso)

En producción DOCENTIA se publica en `https://nova.uniautonoma.edu.co/Evadocente-app/`. El contenedor `web`
sirve en la raíz; el proxy de la universidad quita el prefijo y reenvía. Se probó en local con este mismo
esquema: presentación, login, recarga en una ruta profunda (`/Evadocente-app/estadisticas`), logo y API.

**Al compilar la web** (en el `.env` del servidor, antes de `docker compose build web`):

| Variable | Valor en producción | Para qué |
|---|---|---|
| `VITE_BASE_PATH` | `/Evadocente-app/` | Prefijo de los recursos y `basename` del router |
| `VITE_API_URL` | `/Evadocente-app/api` | Dónde llama la web a la API |
| `DOCENTIA_APP_URL` | `https://nova.uniautonoma.edu.co/Evadocente-app` | Enlaces de los correos (recuperar contraseña, avisos) |
| `DOCENTIA_CORS_ORIGENES` | vacío | La web y la API comparten origen a través del proxy; no hace falta CORS |

Si la subruta o el dominio cambian, solo cambian estas variables y se vuelve a compilar `web`.

**Proxy de la universidad (nginx de ejemplo):**

```nginx
location = /Evadocente-app { return 301 /Evadocente-app/; }
location /Evadocente-app/ {
    proxy_pass http://SERVIDOR_DOCENTIA:5173/;   # la barra final quita el prefijo
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    proxy_read_timeout 120s;                     # la generación del análisis puede tardar
}
```

- El nginx del contenedor `web` reenvía `/api/` a la API y agrega `X-Forwarded-For`; la API toma como IP del
  cliente el último salto de esa cabecera, que es el que agrega el proxy propio.
- El HTTPS termina en el proxy de la universidad. La web envía HSTS; el navegador solo la aplica bajo HTTPS.

## Variables de entorno

| Variable | Obligatoria en producción | Uso |
|----------|---------------------------|-----|
| `SPRING_PROFILES_ACTIVE` | Sí, `prod` | `dev` carga datos de ejemplo |
| `DOCENTIA_JWT_SECRETO` | Sí, 32 bytes o más | Firma de sesiones |
| `DOCENTIA_DB_URL`, `DOCENTIA_DB_USUARIO`, `DOCENTIA_DB_CONTRASENA` | Sí | Base de datos |
| `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE`, `_CONTRASENA` | Solo el primer arranque | Primer administrador (ver arriba) |
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

Restaurar reemplaza la base y reinicia la API. Los dos scripts usan el `docker compose` de la carpeta
donde se corren; para otro proyecto de Compose, exporta antes `COMPOSE_PROJECT_NAME`.

## Despliegue de 0.4.0 (sábado 14 de noviembre de 2026)

Pasa el servidor de 0.3.3 (migración V28) a 0.4.0 (V41). Las migraciones nuevas son V34 a V41: agregan
tablas y columnas, normalizan mayúsculas y tildes de nombres (V38 y V39) y no borran filas. No tienen
vuelta atrás: el retorno es restaurar el respaldo.

Se hace el sábado porque la recolección de 2026-2 cierra el viernes 13 y nadie debería estar enviando
formularios. Duración estimada: 1 hora, más el ensayo.

**Por confirmar antes de empezar** (no está en el repositorio): carpeta del despliegue en el servidor,
versión que corre hoy, perfil activo y quién tiene acceso. Lo que sigue supone que el servidor corre el
`docker-compose.yml` de este repositorio.

### 0. Antes del sábado

- [ ] `develop` con CI verde, `release/0.4.0` revisada y la decisión de publicarla (fusión a `main` y
      etiqueta `v0.4.0`). Sin etiqueta no se despliega.
- [ ] El PR #8 cerrado sin fusionar: trae V29 a V33, que en `develop` son V36 a V40.
- [ ] Copia de las variables nuevas para el `.env` del servidor: compara `.env.example` de 0.3.3 y de
      0.4.0. Nuevas: `DOCENTIA_IA_ENVIAR_TEMPERATURA`, `DOCENTIA_BOOTSTRAP_*` (vacías: ya hay usuarios) y,
      si los estudiantes van a entrar, `DOCENTIA_GOOGLE_CLIENT_ID` y `VITE_GOOGLE_CLIENT_ID`.

### 1. Diagnóstico (solo lectura)

En la carpeta del despliegue:

```bash
git describe --tags                       # versión desplegada
grep SPRING_PROFILES_ACTIVE .env          # debe ser prod
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'   < docentia-backend/docs/consultas-verificacion.sql > antes.txt
```

En `antes.txt`: la última migración debe ser 28, `migraciones_v29_a_v33` 0, `migraciones_fallidas` 0 y
`usuarios_de_demo` 0. **Si algo no coincide, no se despliega**: V29–V33 aplicadas romperían Flyway, y
usuarios de demo o el perfil `dev` significan cuentas con contraseña pública en producción.

### 2. Respaldo

```bash
docker compose stop web                    # nadie entra mientras tanto
sh scripts/respaldar.sh respaldo-0.3.3-$(date +%Y%m%d-%H%M).dump
docker compose exec -T db pg_restore --list < respaldo-0.3.3-*.dump | head   # el archivo se lee
docker tag docentia-api:latest docentia-api:0.3.3
docker tag docentia-web:latest docentia-web:0.3.3
```

Copia el `.dump` fuera del servidor antes de seguir.

### 3. Ensayo de las migraciones sobre una copia

En otra carpeta del mismo servidor (o en otra máquina con Docker), con otro proyecto de Compose y
otros puertos, para no tocar la base real:

```bash
git clone -b v0.4.0 https://github.com/bryanypes/docentia-backend.git ensayo && cd ensayo
cp .env.example .env    # POSTGRES_PORT, API_PORT, WEB_PORT y MAILPIT_PORT libres;
                        # SPRING_PROFILES_ACTIVE=prod y un DOCENTIA_JWT_SECRETO de prueba
export COMPOSE_PROJECT_NAME=docentia-ensayo
docker compose up -d --wait db mailpit
sh scripts/restaurar.sh ../respaldo-0.3.3-*.dump   # sin contenedor de api, su stop/start no hace nada
docker compose up -d --build --wait api
docker compose logs api | grep -E "Migrating|Successfully applied"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'   < docentia-backend/docs/consultas-verificacion.sql > ensayo.txt
```

Debe decir `Successfully applied 8 migrations … now at version v41`, la API quedar `healthy` y la
sección 7 de `ensayo.txt` tener los mismos conteos que `antes.txt`. Al terminar:
`docker compose down -v` en la carpeta del ensayo.

> **Ensayado en local el 11 de noviembre de 2026** con los mismos scripts: base de 0.3.3 con la semilla
> (V28), `respaldar.sh` (150 KB), `restaurar.sh` en un stack de `develop` con perfil `prod`: 8 migraciones
> aplicadas, API `UP` y los diez conteos de la sección 7 iguales. La vuelta atrás (restaurar el mismo
> respaldo en 0.3.3) dejó V28, la API `UP` y descartó lo escrito después del respaldo. Con datos reales
> del servidor **no se ha ensayado**: ese es el paso 3.

### 4. Despliegue

```bash
git fetch --tags && git checkout v0.4.0
# .env: agrega las variables nuevas del paso 0
docker compose build
docker compose up -d --wait
docker compose logs api | grep -E "Successfully applied|Started DocentiaApplication|ERROR|WARN"
```

### 5. Verificación

- [ ] `curl -s http://localhost:8080/api/actuator/health/readiness` responde `UP`.
- [ ] `consultas-verificacion.sql` → `despues.txt`: última migración 41 y conteos iguales a `antes.txt`.
- [ ] La web abre y vicerrectoría entra; **Periodos** muestra las etapas con sus fechas.
- [ ] Coordinación entra; **Análisis de comentarios** lista los modelos (`LOCAL` por defecto).
- [ ] El log no tiene `ERROR`. Si falta Google, aparece el aviso `Falta DOCENTIA_GOOGLE_CLIENT_ID`.
- [ ] `docker compose start web` si se detuvo en el paso 2.

### 6. Vuelta atrás

Si la verificación falla y no se resuelve en la primera hora:

```bash
docker compose down                         # sin -v: conserva el volumen hasta restaurar
git checkout v0.3.3
docker tag docentia-api:0.3.3 docentia-api:latest
docker tag docentia-web:0.3.3 docentia-web:latest
docker compose up -d --wait db
sh scripts/restaurar.sh respaldo-0.3.3-*.dump
docker compose up -d --wait
```

Lo que se haya escrito en la base después del respaldo se pierde; por eso la web queda detenida hasta
terminar la verificación.

## Salud

```bash
curl http://localhost:8080/api/actuator/health/readiness
curl http://localhost:8080/api/actuator/health/liveness
```

## Inicio de sesión con Google

Fuera de `dev` los estudiantes no tienen contraseña utilizable: la matrícula les asigna una
aleatoria que nadie conoce. **Sin Google ningún estudiante puede entrar a evaluar.** El personal puede
entrar con correo y contraseña.

| Qué | Dónde | Nota |
|-----|-------|------|
| Cliente OAuth 2.0 tipo **Aplicación web** | Google Cloud Console de la universidad | No se usa el secreto `GOCSPX-…`; solo el Client ID |
| Orígenes de JavaScript autorizados | Mismo cliente | `https://nova.uniautonoma.edu.co`: solo esquema y dominio, sin la subruta `/Evadocente-app/` ni barra final (Google compara el origen, no la ruta). `http://localhost:5173` solo para pruebas. **Por confirmar** que ya esté autorizado en la consola de Google de la universidad |
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
| `access a socket in a way forbidden` o `ports are not available` en Windows | El puerto está reservado por Hyper-V/WSL. `netsh interface ipv4 show excludedportrange protocol=tcp` y elige en `.env` puertos fuera de esos rangos |
| `port is already allocated` | Copia `.env.example` como `.env` y cambia `API_PORT`, `WEB_PORT`, `POSTGRES_PORT` o `MAILPIT_PORT` |
| La API no arranca en `prod` | Falta `DOCENTIA_JWT_SECRETO` o tiene un valor de desarrollo |
| La firma responde 409 | Falta la cita, el promedio no está completo o la etapa está cerrada; el mensaje lo dice |
| No llegan correos | Revisa Mailpit. Los envíos salen cada 15 segundos y se reintentan cinco veces |
| Quieres repetir la demo | `docker compose down -v` y vuelve a levantar |
| Un error 5xx en pantalla | Busca el código de soporte en `docker compose logs api` |
