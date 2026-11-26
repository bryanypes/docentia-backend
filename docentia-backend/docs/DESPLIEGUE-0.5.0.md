# Despliegue de DOCENTIA 0.5.0

0.5.0 se instala sobre **0.4.1**, la versión que está en producción. Sobre 0.4.2, 0.4.3 o 0.4.4 sirve
igual: Flyway aplica solo las migraciones que falten.

## Qué trae

- **Actas completas y congeladas.** El acta guarda una copia de todo lo que imprime: identificación del
  docente, programa, escuela, periodo, reunión, pesos y aspectos conversados. Desde la primera firma esa
  copia no cambia, y la base de datos rechaza cualquier cambio a un acta firmada, a sus firmas y a sus
  compromisos (solo se registra el seguimiento del compromiso).
- **Anonimato de la nota de estudiantes.** Se publica al llegar al mínimo de respuestas (5) y luego cada
  4 más (9, 13…); al cerrar la recolección, con todas.
- **Histórico propio y reporte PDF colectivo.** El docente ve sus promedios finales de periodos anteriores.
  La coordinación descarga un PDF con los docentes de su facultad (tres notas y promedio) y la
  vicerrectoría, uno de toda la universidad.
- **Desde 0.4.2:** coordinador evaluado por la vicerrectoría, tutorial de primer inicio, solicitudes de
  reapertura de evaluaciones, registro de actas para la administración, análisis por criterio con alerta
  «Para revisar», observaciones de la reunión en el acta, y fallos del proveedor de IA con reintentos y
  respaldo local. El detalle está en el `CHANGELOG.md`.

## Antes de empezar: el `.env` del servidor

**Hasta 0.4.4 el `docker-compose.yml` solo pasaba al contenedor 24 de las variables.** Desde 0.5.0 pasa
todas. Las siguientes no surtían efecto aunque estuvieran en el `.env`; desde ahora sí. Revisa su valor
antes de desplegar:

`DOCENTIA_ACTAS_EXIGIR_FIRMA_DIBUJADA`, `DOCENTIA_ACTAS_LOGO`, `DOCENTIA_CORS_ORIGENES`,
`DOCENTIA_DB_POOL_MAXIMO`, `DOCENTIA_HETEROEVALUACION_MINIMO`, `DOCENTIA_IA_MAX_CARACTERES_COMENTARIO`,
`DOCENTIA_IA_MAX_COMENTARIOS_LLM`, `DOCENTIA_IA_MAX_TOKENS_ENTRADA`, `DOCENTIA_IA_MAX_TOKENS_SALIDA`,
`DOCENTIA_IA_PRECIO_MILLON_ENTRADA`, `DOCENTIA_IA_PRECIO_MILLON_SALIDA`,
`DOCENTIA_IA_PRESUPUESTO_TOKENS_DIA`, `DOCENTIA_IA_PRESUPUESTO_TOKENS_PERIODO`,
`DOCENTIA_IA_TIMEOUT_SEGUNDOS`, `DOCENTIA_IA_WORKER`, `DOCENTIA_NOTIFICACIONES_RETENCION_DIAS`,
`DOCENTIA_PERIODOS_MES_FIN_PRIMER_SEMESTRE` y `DOCENTIA_PROCESO_EXIGIR_ASISTENCIA`.

**Variables nuevas desde 0.4.1** (todas opcionales; el valor por defecto sirve):

| Variable | Por defecto | Para qué |
|---|---|---|
| `DOCENTIA_ACTAS_URL_VERIFICACION` | `https://nova.uniautonoma.edu.co/Evadocente-app` | Dirección impresa en el PDF para verificar el acta |
| `DOCENTIA_ACTAS_ROTULO_DOCUMENTO` | `Código` | Rótulo del código del docente en el acta; vacío, no se imprime |
| `DOCENTIA_ACTAS_FORMATO_CODIGO` | vacío | Código del formato institucional del acta, en el membrete |
| `DOCENTIA_ACTAS_FORMATO_VERSION` | vacío | Versión de ese formato |
| `DOCENTIA_HETEROEVALUACION_BLOQUE` | `4` | Cada cuántas respuestas, después del mínimo, se vuelve a publicar la nota de estudiantes |
| `DOCENTIA_IA_ENFRIAMIENTO_MINUTOS` | `5` | Espera mínima para regenerar un análisis |
| `DOCENTIA_IA_MINIMO_COMENTARIOS` | `5` | Comentarios escritos necesarios para analizar a un docente |
| `DOCENTIA_UMBRAL_REVISION` | `4.0` | Nota final bajo la cual el docente queda «Para revisar» |

**Cambia un valor por defecto:** `DOCENTIA_IA_ENVIAR_TEMPERATURA` pasa de `true` a `false`.

## Migraciones

La base pasa de **V43 a V52** (9 migraciones). En el ensayo con una base vacía se aplican sin errores;
**falta el ensayo con la copia de producción**.

| Migración | Qué hace | ¿Cambia datos? |
|---|---|---|
| V44 | La vicerrectoría puede firmar actas (coordinador evaluado) | No |
| V45 | Un promedio PARCIAL ya no guarda valor ni categoría | **Sí**: los borra en los PARCIAL; se recalculan |
| V46 | `solicitudes_analisis.comentarios_leidos` | No |
| V47 | `usuarios.tutorial_visto` (todos lo verán una vez) | No |
| V48 | Tabla `solicitudes_reapertura` y `evaluaciones_formulario.reabierto` | No |
| V49 | `actas.observaciones` y protección de las observaciones firmadas | No |
| V50 | `hallazgos_analisis.criterio` | No |
| V51 | Copia de los datos del acta (identificación, reunión, pesos, facultad); las actas existentes toman los datos de hoy | **Sí**: llena columnas nuevas; no toca notas, hashes ni PDF |
| V52 | Inmutabilidad completa del acta, sus firmas y sus compromisos | No |

## Pasos

```bash
# 1. Respaldo (obligatorio: V45 y V51 cambian datos)
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > respaldo-antes-de-0.5.0.dump

# 2. Versión
git fetch --tags origin
git checkout v0.5.0

# 3. Revisar el .env (sección anterior) y desplegar backend y frontend juntos
docker compose up -d --build --wait api web
sh scripts/humo-postdespliegue.sh https://nova.uniautonoma.edu.co/Evadocente-app
```

- El log de `api` debe decir `Successfully applied 9 migrations to schema "public", now at version v52`.
- El script de humo debe terminar sin fallas y mostrar `versión publicada: 0.5.0`.
- `docker compose exec api env | grep DOCENTIA_ | sort` debe listar las variables del `.env`.

## Vuelta atrás

V45 y V51 cambian datos, así que **la vuelta atrás es restaurar el respaldo** y volver a la etiqueta
anterior. Se pierde lo escrito después del respaldo.

```bash
docker compose stop api web
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' < respaldo-antes-de-0.5.0.dump
git checkout v0.4.1
docker compose up -d --build --wait api web
```

No se usa `docker compose down -v`: borra el volumen de la base.
