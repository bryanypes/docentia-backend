# Despliegue de DOCENTIA 0.4.0

Pasos para quien tiene acceso al servidor. Toma menos de 30 minutos si el respaldo y las variables están listos. Todo se corre en la carpeta del `docker compose` de DOCENTIA en el servidor.

Lo que dice **por confirmar** depende de una respuesta del equipo o de la institución antes de empezar.

## a. Condiciones previas

- [ ] **La recolección de evaluaciones cerró:** después de las 00:00 del sábado 14 de noviembre (hora de Colombia). No se despliega antes sobre la base real.
- [ ] Hay un **respaldo comprobado** (paso b) y una copia fuera del servidor.
- [ ] Están los valores de producción:
  - el Client ID de Google (**por confirmar**);
  - el SMTP (**por confirmar**);
  - un `DOCENTIA_JWT_SECRETO` propio, de 32 bytes o más.
- [ ] Se sabe qué rama o etiqueta usa hoy el servidor y cómo se despliega (**por confirmar**). Esta guía supone un clon del repositorio con `docker compose`.
- [ ] Hay una ventana sin usuarios. La web queda detenida durante la migración.

## b. Respaldo

```bash
sh scripts/respaldar.sh respaldo-0.3.3-$(date +%Y%m%d-%H%M).dump
```

- El script falla si el archivo queda vacío o no se puede leer.
- Al terminar muestra el tamaño y cuántas tablas tienen datos; deben ser **más de 40**. En el ensayo local fueron 41 tablas y unos 150 KB con datos de prueba; la base real será más grande.
- Copia el archivo fuera del servidor y fuera de cualquier carpeta con repositorio: tiene datos personales.

## c. Estado previo (solo lectura)

```bash
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < docentia-backend/docs/consultas-verificacion.sql > antes.txt
```

Revisa en `antes.txt`:

| Sección | Qué esperar |
|---|---|
| 1. Versión de Flyway | Termina en **28**. Si aparece alguna de V29 a V33, **detente**: la base tiene migraciones de otra rama |
| 2. Ventanas del periodo activo | Las fechas reales; la recolección ya cerró |
| 3. Docentes con menos de 5 respuestas | Quedan con promedio PARCIAL y sin poder firmar acta; anótalos para Vicerrectoría |
| 5. Usuarios de la semilla de desarrollo | **0**. Si no, la base se creó con el perfil `dev`: detente y avisa |
| 8. Actas por estado | Si hay FIRMADA o ANULADA, 0.4.0 lo resuelve con sus callbacks de Flyway; no hace falta nada más |

Guarda `antes.txt` para compararlo en el paso g.

## d. Actualizar el código y las variables

```bash
docker compose stop web api
git fetch --tags origin
git checkout v0.4.0
```

En el `.env` del servidor agrega o revisa estas variables (la tabla completa está en `OPERACION.md`):

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DOCENTIA_APP_URL` | `https://nova.uniautonoma.edu.co/Evadocente-app`. Con `prod` la API no arranca si está vacía o en localhost |
| `VITE_BASE_PATH` | `/Evadocente-app/` |
| `VITE_API_URL` | `/Evadocente-app/api` |
| `DOCENTIA_GOOGLE_CLIENT_ID` y `VITE_GOOGLE_CLIENT_ID` | El mismo Client ID (**por confirmar**) |
| `DOCENTIA_MAIL_HOST`, `_PORT`, `_USER`, `_PASS`, `_FROM`, `_TLS` | SMTP real (**por confirmar**). Sin él no llega el correo de recuperar contraseña |
| `DOCENTIA_BOOTSTRAP_EMAIL`, `_NOMBRE`, `_CONTRASENA` | Primer administrador (paso f) |

## e. Migraciones

```bash
docker compose up -d --build --wait api
docker compose logs api | grep -E "SQL callback|Successfully applied|Started|failed"
```

Qué esperar:

- `Executing SQL callback: beforeMigrate`
- `Successfully applied 10 migrations` (V34 a V43)
- `Executing SQL callback: afterMigrate`
- `Started DocentiaApplication`

En el ensayo tardó **18 s**. V38 y V39 corrigen nombres que estaban todo en mayúsculas o sin tilde. No cambian notas, promedios, ventanas, respuestas ni actas.

Si aparece `failed` o `Application run failed`, pasa al paso h.

## f. Primer administrador

0.4.0 separa la administración (usuarios, programas, ponderaciones y fechas) de la vicerrectoría. **El administrador se crea en este mismo arranque, antes de que alguien necesite dar de alta usuarios**, porque desde 0.4.0 la vicerrectoría ya no puede hacerlo.

1. Con `DOCENTIA_BOOTSTRAP_*` en el `.env`, la API crea el primer ADMINISTRADOR al arrancar. Lo hace solo si no hay ninguno activo, y una sola vez.
2. Esa persona entra con la contraseña de `DOCENTIA_BOOTSTRAP_CONTRASENA` y DOCENTIA le pide cambiarla.
3. Después, **quita las tres variables `DOCENTIA_BOOTSTRAP_*` del `.env`**. El log no muestra ni el correo ni la clave.

## g. Web y pruebas de humo

```bash
docker compose up -d --build --wait web
sh scripts/humo-postdespliegue.sh https://nova.uniautonoma.edu.co/Evadocente-app
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < docentia-backend/docs/consultas-verificacion.sql > despues.txt
```

- El script de humo solo lee. Debe terminar en `sin fallas` y mostrar `versión publicada: 0.4.0`.
- Un aviso de Google indica que falta el Client ID: no es motivo de vuelta atrás.
- En `despues.txt`, la sección 1 termina en **43**. La sección 7 tiene los mismos conteos que `antes.txt`, salvo `usuarios`, que suma 1 por el administrador.

A mano, con cuentas reales:

- [ ] La vicerrectoría entra con su contraseña y ve Inicio, Estadísticas y Periodos.
- [ ] La administración entra, cambia su clave temporal y ve Usuarios.
- [ ] Un estudiante real de prueba entra con Google y ve Mis evaluaciones. Si la recolección cerró, verá "La evaluación ya cerró".
- [ ] En Periodos, las ventanas del periodo activo coinciden con las de `antes.txt`.
- [ ] Un acta ya firmada sigue mostrando su PDF y su código de verificación.

## h. Vuelta atrás

Aplícala si la API no arranca o no llega a `UP`, si una migración falla, si los conteos no coinciden o si nadie puede entrar, y no se resuelve en la primera hora:

```bash
docker compose down                  # sin -v
git checkout v0.3.3
docker compose up -d --wait db
sh scripts/restaurar.sh respaldo-0.3.3-AAAAMMDD-HHMM.dump
docker compose up -d --build --wait
```

En el ensayo local volvió a V28 con los datos idénticos en 19 s. Se pierde lo escrito después del respaldo.

## i. Qué no hacer

- **No** arrancar con `SPRING_PROFILES_ACTIVE=dev` sobre la base real: carga usuarios de demostración con una contraseña pública.
- **No** desplegar sin un respaldo comprobado y copiado fuera del servidor.
- **No** corregir datos a mano en la base: la aplicación y sus migraciones son las que cambian datos.
- **No** usar `docker compose down -v`: borra el volumen de la base.
- **No** dejar `DOCENTIA_BOOTSTRAP_CONTRASENA` en el `.env` después del primer ingreso.
