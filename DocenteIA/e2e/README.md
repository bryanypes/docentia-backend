# Pruebas de humo

Recorren la demo con un navegador real contra el sistema levantado con el seed recién creado. No
forman parte del build de la aplicación.

```bash
docker compose down -v && docker compose up --build -d   # en docentia-backend
cd e2e
npm install
npm run instalar
BASE_URL=http://localhost:5173 npm test
```

Cubren login por rol, expediente con categoría, cita, firma con PDF, rechazo de editar un acta
firmada, corrección, revisión de hallazgos, panel por programa y notificaciones. Modifican datos: cada
corrida necesita un seed nuevo.
