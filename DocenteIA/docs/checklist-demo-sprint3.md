# Checklist de demo — Sprint 3

Recorrido por la aplicación sobre una base **recién creada** con el perfil `dev` (ver "Recrear la
base de desarrollo" en el README del backend). Contraseña de los usuarios de demo: `Docentia2026*`.

## Antes de empezar

- [ ] Backend corriendo con el perfil `dev` y JDK 21 (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`).
- [ ] Frontend con `npm run dev` (o con `DOCENTIA_API_URL` si el backend no está en el 8080).
- [ ] `http://localhost:5173` abre el login.

## 1. Coordinación — `coordinacion.ingenieria@uniautonoma.edu.co`

- [ ] **Inicio:** indicadores de la Facultad de Ingeniería.
- [ ] **Evaluación de estudiantes → `evaluacion-estudiantes-con-errores.csv`** (en `docs/ejemplos/`
      del backend): errores por fila (nota 7, "cuatro", nota vacía, dos "Docente no encontrado") y sin
      opción de importar.
- [ ] **`evaluacion-estudiantes-ejemplo.csv`:** válido; **Importar** muestra la carga con 1 nota nueva,
      1 corregida y 4 comentarios nuevos.
- [ ] **Docentes → Julián:** promedio **4,27** (antes 4,34) y la corrección "Carga de CSV #…" en el
      historial.
- [ ] **Análisis de comentarios → Julián, 2026-2 → Solicitar análisis:** en segundos, borrador con 6
      hallazgos, cada cita con su comentario de origen, y "Comentarios excluidos por el filtro (2)".
- [ ] **Marcar como válido** el primero → "1 de 6 hallazgos validados."
- [ ] **Análisis → Diana:** "6 de 10 encuestas con comentario".
- [ ] **Agenda de citas:** abrir el 11 nov a las 10:00 (termina 10:30). Bloque de 09:30 a 11:00 →
      "Franjas abiertas: 2. Omitidas: 1." con el motivo.
- [ ] **Alcance:** `/docentes/7` (docente de Derecho) → no encontrado.

## 2. Docente — `julian.hoyos@uniautonoma.edu.co`

- [ ] **Mi cita:** reservar la primera franja; reservar otra → confirmación de que la anterior se
      liberará → "Tu cita anterior quedó liberada."
- [ ] **Mi evaluación:** promedio 4,27, sin notas por fuente (regla A4) y sin borrador del análisis.
- [ ] `/analisis` → "Sin acceso".

## 3. Vicerrectoría — `vicerrectoria@uniautonoma.edu.co`

- [ ] **Inicio:** promedio general 4,29 (+0,25 frente a 2026-1); los filtros recalculan.
- [ ] **Periodos:** crear 2027-1 con las cuatro etapas y editarlo; en 2026-2 cada etapa dice si está
      abierta hoy.
- [ ] **Ponderaciones:** el botón solo se habilita si los pesos suman 100.
- [ ] **Facultades y Programas:** crear uno de cada uno.
- [ ] **Usuarios:** crear un docente. Entrar con esa cuenta obliga a cambiar la contraseña temporal
      antes de ver cualquier pantalla.

## Datos del seed sin importar el CSV

| Dato | Esperado |
| ---- | -------- |
| Julián, 2026-2 | COMPLETO 4,34 |
| Diana, 2026-2 | COMPLETO 4,31 |
| Julián, 2026-1 (histórico) | 4,04 |

## Si algo no sale

| Síntoma | Causa |
| ------- | ----- |
| Aviso de etapa cerrada | La fecha de hoy está fuera de esa etapa: revisar Periodos |
| Los números no coinciden | La base no se recreó antes de la demo |
| "Esta función no está disponible" en Análisis | El backend no se arrancó con el perfil `dev` |
| Error 502 o "servidor no disponible" | El backend no corre o `DOCENTIA_API_URL` apunta a otro puerto |
