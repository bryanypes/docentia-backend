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
| `SPRING_PROFILES_ACTIVE` | Sí, `prod` | `dev` carga usuarios y datos de demostración; `prod` no carga ninguno |
| `DOCENTIA_JWT_SECRETO` | Sí, 32 bytes o más | Firma de sesiones. Sin él, o con el de desarrollo, la API no arranca |
| `DOCENTIA_DB_URL`, `DOCENTIA_DB_USUARIO`, `DOCENTIA_DB_CONTRASENA` | Sí | Base de datos |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | Sí, si la base va en Compose | Servicio `db` |
| `DOCENTIA_APP_URL` | Sí | URL pública para los enlaces de los correos. Con `prod`, la API no arranca si está vacía o en localhost |
| `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE`, `_CONTRASENA` | Solo el primer arranque | Primer administrador (ver arriba) |
| `DOCENTIA_MAIL_HOST`, `_PORT`, `_USER`, `_PASS`, `_FROM`, `_TLS` | Sí, para recuperar contraseña y avisos | SMTP. Sin host, la API arranca pero solo registra el asunto en el log |
| `DOCENTIA_GOOGLE_CLIENT_ID` | Sí, si entran estudiantes | Valida el token de Google. Sin él, `POST /api/sesiones/google` responde 503 |
| `VITE_GOOGLE_CLIENT_ID` | Sí, si entran estudiantes | Botón de Google; se incrusta al compilar `web` |
| `VITE_BASE_PATH` | Sí, `/Evadocente-app/` | Subruta pública; se incrusta al compilar `web` |
| `VITE_API_URL` | Sí, `/Evadocente-app/api` | Ruta de la API desde la web; se incrusta al compilar `web` |
| `DOCENTIA_CORS_ORIGENES` | Solo si la web vive en otro dominio | Orígenes exactos, separados por coma |
| `DOCENTIA_DB_POOL_MAXIMO` | No, 15 | Pool de conexiones |
| `DOCENTIA_COMPROMISOS_UMBRAL` | No, 3.8 | Bajo esta nota el acta exige compromisos (por confirmar: 3,8 o 4,00) |
| `DOCENTIA_ACTAS_ROL_CORRECTOR` | No, `VICERRECTOR` | Quién corrige actas firmadas (`VICERRECTOR` o `COORDINADOR`) |
| `DOCENTIA_HETEROEVALUACION_MINIMO` | No, 5 | Respuestas mínimas para usar y mostrar la nota de estudiantes |
| `DOCENTIA_PROCESO_EXIGIR_ASISTENCIA` | No, `true` | El docente ve su resultado y firma solo tras registrar la asistencia |
| `DOCENTIA_ACTAS_EXIGIR_FIRMA_DIBUJADA` | No, `true` | La firma lleva imagen (dibujada, escrita o PNG) |
| `DOCENTIA_ACTAS_LOGO` | No | Logo del PDF; por defecto el de la universidad incluido |
| `DOCENTIA_PERIODOS_MES_FIN_PRIMER_SEMESTRE` | No, 6 | Mes de corte del nombre automático del periodo (por confirmar) |
| `DOCENTIA_NOTIFICACIONES_RETENCION_DIAS` | No, 90 | Días que se guardan las notificaciones leídas |
| `DOCENTIA_IA_*` | No | Análisis de comentarios: `local` por defecto, sin red. El externo solo con URL, modelo y clave |
| `DOCENTIA_IA_PRESUPUESTO_TOKENS_DIA`, `_PERIODO` | No, 500 000 y 3 000 000 | Topes del modelo externo |
| `DOCENTIA_API_URL` | Solo en desarrollo | Proxy de Vite hacia el backend |

**Zona horaria:** no requiere variable. Las fechas de las ventanas y los cierres se calculan siempre con
`America/Bogota` (`RelojConfig`), sin importar la zona del servidor.

## Respaldo y restauración

```bash
sh scripts/respaldar.sh respaldo.dump
sh scripts/restaurar.sh respaldo.dump
```

Restaurar reemplaza la base y reinicia la API. Los dos scripts usan el `docker compose` de la carpeta
donde se corren; para otro proyecto de Compose, exporta antes `COMPOSE_PROJECT_NAME`.

- **Qué verifican.** `respaldar.sh` falla si el archivo queda vacío o si `pg_restore` no puede leer su
  índice, y al terminar muestra el tamaño y cuántas tablas tienen datos. `restaurar.sh` hace las mismas
  comprobaciones **antes** de borrar la base: con un archivo vacío o dañado no toca nada.
- **Frecuencia recomendada:** un respaldo diario fuera del horario de uso y uno antes de cada despliegue o
  cambio de periodo. Con `cron`: `0 2 * * * cd /ruta/docentia && sh scripts/respaldar.sh /respaldos/docentia-$(date +\%F).dump`.
- **Retención sugerida** (por confirmar con la institución): 14 diarios, 8 semanales y los de cada cierre
  de periodo sin vencimiento. Los respaldos se guardan fuera del servidor y fuera de cualquier carpeta con
  repositorio: tienen datos personales.
- **Cómo verificar uno guardado:** `docker compose exec -T db pg_restore -l < archivo.dump | grep -c "TABLE DATA"`
  debe dar más de 40. Una vez al mes conviene restaurarlo en otra base con otro proyecto de Compose,
  como en el ensayo de migraciones.
- **Probado el 12 de noviembre de 2026:** respaldo de 179 KB y 46 tablas en 1 s; restauración en 7 s con
  la API de vuelta en `UP`; un archivo vacío se rechazó sin tocar la base.

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

Debe decir `Executing SQL callback: beforeMigrate`, `Successfully applied 10 migrations` (V34 a V43) y
`Executing SQL callback: afterMigrate`; la API debe quedar `healthy` y la
sección 7 de `ensayo.txt` tener los mismos conteos que `antes.txt`. Al terminar:
`docker compose down -v` en la carpeta del ensayo.

> **Ensayado en local el 12 de noviembre de 2026** sobre v0.3.3 (V28) con su semilla más un acta
> FIRMADA, una ANULADA y una BORRADOR, y 4 respuestas anónimas de estudiantes con sus participaciones:
>
> | Paso | Resultado |
> |---|---|
> | `pg_dump -Fc` | 150 KB, 41 tablas con datos, 1 s |
> | Restauración en otra base | 3 s |
> | Migración con 0.4.0 y perfil `prod` | 10 migraciones (V34 a V43), arranque completo en 18 s |
> | Notas, promedios, ventanas, periodos, respuestas, participaciones y actas | Sumas md5 idénticas antes y después |
> | Usuarios existentes | Idénticos; se suma solo el primer administrador (`DOCENTIA_BOOTSTRAP_*`), con clave temporal |
> | Nombres de programas y cursos | Solo cambian los que estaban todo en mayúsculas o sin tilde (V38, V39), por ejemplo `ADMINISTRACION DE EMPRESAS PROFESIONAL` → `Administración de Empresas Profesional`; los ids no cambian |
> | Actas existentes | Toman su tipo (V36); la firmada sigue inmutable: un `update` sobre ella responde "Un acta firmada es inmutable" |
> | Log | Sin el correo ni la clave del administrador |
> | Tamaño | 11 MB antes y después |
>
> **Cómo funciona la corrección:** `beforeMigrate.sql` apaga el disparador de actas inmutables solo
> mientras se migra; `afterMigrate.sql` y `afterMigrateError.sql` lo vuelven a encender, también si una
> migración falla. Fuera de la migración no se toca (detalle en `SEGURIDAD.md`).
>
> **Error encontrado y corregido en el ensayo:** sin los callbacks `beforeMigrate.sql` y `afterMigrate.sql`,
> V36 fallaba sobre una base con actas firmadas o anuladas y la API quedaba sin arrancar en V35. Por eso
> el paso 1 debe contar cuántas actas FIRMADA y ANULADA hay en el servidor. Con datos reales del servidor
> **no se ha ensayado**: ese es el paso 3.

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

**Cuándo aplicarla** (cualquiera de estas, y que no se resuelva en la primera hora):

- La API no arranca o no llega a `UP` en `/api/actuator/health/readiness`.
- El log muestra `Migration of schema … failed` o `Application run failed`.
- Los conteos de la sección 7 de `consultas-verificacion.sql` no coinciden con los de antes.
- Nadie puede iniciar sesión con su contraseña, o una pantalla principal responde 5xx de forma repetida.

Un aviso de Google o de SMTP sin configurar **no** es motivo de vuelta atrás: se corrige con la variable.

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

**Probado el 12 de noviembre de 2026:** sobre el respaldo de v0.3.3, restaurar en una base limpia y
levantar la imagen de 0.3.3 tomó 19 s; la base volvió a V28 (`Schema is up to date`) y las sumas md5 de
notas, ventanas, respuestas y actas quedaron idénticas al respaldo.

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
