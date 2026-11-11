# Pruebas de humo

Recorren la demo con un navegador real contra el sistema levantado con el seed recién creado. No
forman parte del build de la aplicación.

```bash
docker compose down -v && docker compose up --build -d   # en la raíz del monorepo
cd DocenteIA/e2e
npm install
npm run instalar
BASE_URL=http://localhost:5173 npm test
```

Once pruebas, unos 40 segundos:

1. Coordinación: expediente con categoría, acta de compromiso con un compromiso y un horario para hoy.
2. Docente: reserva la cita y no puede firmar antes de la reunión.
3. Coordinación: registra la asistencia («Asistió · habilitar resultado») y firma el acta.
4. Docente: firma su acta.
5. Coordinación: descarga el PDF; el acta firmada no se edita (409).
6. Vicerrectoría: corrige con una versión nueva y la ve en Auditoría.
7. Coordinación: analiza, edita, valida, descarta y publica hallazgos; el docente no los ve sin reunión.
8. Panel por programa sin docentes identificables.
9. Campana de notificaciones.
10. Autoevaluación y coevaluación cambian la nota; el docente no ve la coevaluación.
11. Estudiante importado por matrícula evalúa de forma anónima; al quinto envío la nota cambia.

**Fechas.** La semilla tiene fechas fijas. Antes de empezar, la prueba abre las cuatro etapas del
periodo activo desde una semana antes de hoy hasta el fin del periodo (`PUT /periodos-academicos/{id}`
como vicerrectoría), y abre el horario de hoy a las 23:00, porque la asistencia solo se registra el día
de la cita. Después del fin del periodo de la semilla (5 de diciembre de 2026) hay que actualizar la
semilla.

**Datos.** Las pruebas modifican la base: cada corrida necesita un seed nuevo (`docker compose down -v`).
