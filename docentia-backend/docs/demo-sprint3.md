# Demo Sprint 3 — recorrido de punta a punta

Recorrido completo con `curl` desde Git Bash, sobre una base recién creada con el seed de
desarrollo. Los valores de ejemplo son los que dio la última corrida (27 de octubre de 2026); si se
corre otro día, cambian las fechas de reserva pero no los números.

Orden: login coordinador → importar CSV (C4/C5) → promedio recalculado y KPIs (I5) → solicitar
análisis IA (E1) → estado y borrador con citas (E2/E3) → abrir franjas (F1) → login docente →
reservar (F2).

## 0. Preparar

Requisitos: JDK 21 en `JAVA_HOME`, PostgreSQL con la base `docentia` vacía (ver "Recrear la base de
desarrollo" en el README) y Python para leer el JSON en la consola (`jq` también sirve).

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # en otra terminal; queda en :8080/api
```

En otra terminal, desde la raíz del repositorio:

```bash
B=http://localhost:8080/api
login(){ curl -s -X POST $B/sesiones -H 'Content-Type: application/json' \
  -d "{\"correo\":\"$1\",\"contrasena\":\"Docentia2026*\"}" \
  | python -c "import sys,json; print(json.load(sys.stdin)['token'])"; }
json(){ python -m json.tool; }
```

## 1. Login coordinador

```bash
C=$(login coordinacion.ingenieria@uniautonoma.edu.co)
V=$(login vicerrectoria@uniautonoma.edu.co)
curl -s -H "Authorization: Bearer $C" $B/expedientes/4 | json   # Julián: promedio COMPLETO 4.34
```

## 2. Importar el CSV de estudiantes (C4)

Primero el archivo con errores, para mostrar que no se guarda nada:

```bash
curl -s -H "Authorization: Bearer $C" \
  -F "archivo=@docs/ejemplos/evaluacion-estudiantes-con-errores.csv" \
  $B/importaciones/csv/validar | json
```

→ `"valido": false`, 4 filas con error y 5 problemas (la fila 4 tiene dos): nota 7, "cuatro", nota vacía, docente de Derecho y correo
inexistente (los dos últimos como `Docente no encontrado`). Importarlo da **422** y la carga queda
como `RECHAZADA` en `GET $B/importaciones`.

Ahora el archivo válido:

```bash
curl -s -H "Authorization: Bearer $C" \
  -F "archivo=@docs/ejemplos/evaluacion-estudiantes-ejemplo.csv" \
  $B/importaciones/csv | json
```

→ `200`, `"resultado": "EXITOSA"`, `notasNuevas: 1` (Óscar), `notasCorregidas: 1` (Julián),
`comentariosNuevos: 4`. Volver a enviarlo da `notasSinCambios: 2`, `comentariosRepetidos: 4` y una
advertencia de archivo repetido: nada se duplica.

### Mapeo de columnas (C5)

```bash
# Sin mapeo: las columnas "Dom.", "Plan."… no se reconocen
curl -s -H "Authorization: Bearer $C" \
  -F "archivo=@docs/ejemplos/evaluacion-power-bi-columnas-propias.csv" \
  $B/importaciones/csv/validar | json      # valido=false, camposObligatoriosSinMapear: 5

# Con el mapeo: valida y se puede importar
curl -s -H "Authorization: Bearer $C" \
  -F "archivo=@docs/ejemplos/evaluacion-power-bi-columnas-propias.csv" \
  -F "mapeo=$(cat docs/ejemplos/mapeo-power-bi.json)" \
  $B/importaciones/csv/validar | json      # valido=true, origenMapeo=MANUAL
```

Para la demo **no** se importa este segundo archivo: volvería a corregir la nota de Julián.

## 3. Promedio recalculado y KPIs (D1/D2/I5)

```bash
curl -s -H "Authorization: Bearer $C" $B/expedientes/4 | json                       # 4.34 → 4.27
curl -s -H "Authorization: Bearer $C" $B/docentes/4/calificaciones/correcciones | json
curl -s -H "Authorization: Bearer $V" $B/kpis | json
```

- El promedio de Julián pasó de **4.34 a 4.27**: su nota de estudiantes bajó de 4.50 a 4.36.
- El historial (D2) muestra esa corrección con motivo `Carga de CSV #… (evaluacion-estudiantes-ejemplo.csv)`.
- KPIs institucionales: docentes evaluados **2** (antes 1), promedio general **4.29** (antes 4.04,
  +0.25), evaluaciones **7** (antes 3), destacados **0** (nadie llega a 4.5 con el seed).

## 4. Solicitar el análisis con IA (E1)

```bash
curl -s -X POST -H "Authorization: Bearer $C" -H 'Content-Type: application/json' \
  -d '{"docenteId":4,"periodoId":2}' $B/analisis-ia/solicitudes | json
```

→ **202** inmediato, `"estado": "EN_COLA"`. Anotar `idSolicitud` (en la base recién creada es `1`).

Para mostrar el caso sin evidencia, Diana (6 comentarios):

```bash
curl -s -X POST -H "Authorization: Bearer $C" -H 'Content-Type: application/json' \
  -d '{"docenteId":5,"periodoId":2}' $B/analisis-ia/solicitudes | json
```

## 5. Estado y borrador con citas (E1/E2/E3)

El procesador revisa la cola cada 3 segundos en `dev`:

```bash
curl -s -H "Authorization: Bearer $C" $B/analisis-ia/solicitudes/1 | json
curl -s -H "Authorization: Bearer $C" $B/analisis-ia/solicitudes/1/comentarios-filtrados | json
curl -s -H "Authorization: Bearer $C" $B/analisis-ia/solicitudes/2 | json
```

- Julián: `COMPLETADO`, 15 encuestas cerradas, **13 comentarios enviados y 2 filtrados** (un
  insulto y uno sobre su apariencia). El borrador dice modelo `simulado-determinista`, instrucción
  `analisis-v1`, estado `PENDIENTE_VALIDACION`; 6 hallazgos, cada uno con su cita y
  `"verificada": true`. La crítica dura "La metodología es pésima…" sí llega y se cita.
- Diana: `INSUFICIENTE`, `"evidencia insuficiente (6 de 10 requeridas)"`, sin borrador.

Validar un hallazgo (E3) y comprobar que el docente no ve el borrador:

```bash
curl -s -X POST -H "Authorization: Bearer $C" $B/analisis-ia/hallazgos/1/validacion | json   # VALIDADO
J=$(login julian.hoyos@uniautonoma.edu.co)
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $J" $B/analisis-ia/solicitudes/1   # 403
```

El 422 por cita no verificada no se ve con el modelo simulado (siempre cita bien); está cubierto por
`CitasVerificablesTest`.

## 6. Abrir franjas (F1)

El seed ya trae franjas del 9 y 10 de noviembre. Abrir más:

```bash
curl -s -X POST -H "Authorization: Bearer $C" -H 'Content-Type: application/json' \
  -d '{"fecha":"2026-11-11","horaInicio":"10:00","horaFin":"10:30"}' $B/franjas | json    # 201

curl -s -X POST -H "Authorization: Bearer $C" -H 'Content-Type: application/json' \
  -d '{"accion":"ABRIR","franjas":[
        {"fecha":"2026-11-11","horaInicio":"10:30","horaFin":"11:00"},
        {"fecha":"2026-11-11","horaInicio":"10:15","horaFin":"10:45"}]}' \
  $B/franjas/lote | json
```

→ El lote aplica la de 10:30 y omite la de 10:15 con `"Se cruza con otra franja del mismo lote
(10:30)"`. Una franja de 45 minutos responde 400; una que se cruza con otra abierta, 409.

## 7. Login docente y reserva (F2)

```bash
J=$(login julian.hoyos@uniautonoma.edu.co)
curl -s -H "Authorization: Bearer $J" $B/franjas/disponibles | json
curl -s -X POST -H "Authorization: Bearer $J" $B/franjas/1/reserva | json     # 201, idCitaLiberada null
curl -s -X POST -H "Authorization: Bearer $J" $B/franjas/2/reserva | json     # 201, idCitaLiberada = la anterior
curl -s -H "Authorization: Bearer $J" $B/citas/mia | json
curl -s -H "Authorization: Bearer $C" "$B/franjas?fecha=2026-11-09" | json    # el coordinador ve quién reservó
```

Para mostrar el conflicto: con Diana (`diana.astaiza@uniautonoma.edu.co`) intentar reservar la
franja que ya tiene Julián → `409 "La franja ya fue reservada por otro docente"`.

## Si algo no sale

| Síntoma | Causa |
| ------- | ----- |
| 409 con `"etapa": …` | La fecha de hoy está fuera de la etapa en el periodo; revisar `GET $B/periodos-academicos/activo/ventanas` |
| Los datos no coinciden con esta guía | La base no se recreó: el seed solo corre con la tabla `usuarios` vacía |
| 503 en el análisis | No se arrancó con el perfil `dev` (el análisis está apagado por defecto) |
| La solicitud se queda `EN_COLA` | El procesador está apagado: `docentia.ia.worker.habilitado=false` |
