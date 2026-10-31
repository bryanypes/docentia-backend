# DOCENTIA — frontend

Sistema de evaluación docente de la Corporación Universitaria Autónoma del Cauca.

Este repositorio es la aplicación web. La API REST está en
[docentia-backend](https://github.com/bryanypes/docentia-backend).

Stack: React 19, Vite 8, JavaScript y CSS convencional.

## Cómo correrlo en local

Requisitos: Node.js 20 o superior y el backend corriendo en `http://localhost:8080/api`
(ver el README del backend).

```bash
npm install
cp .env.example .env.local
npm run dev        # http://localhost:5173
```

En desarrollo las peticiones a `/api` se reenvían al backend con el proxy de Vite
(`vite.config.js`). Si el backend corre en otro puerto:

```bash
DOCENTIA_API_URL=http://localhost:8081 npm run dev            # Linux, macOS, Git Bash
$env:DOCENTIA_API_URL="http://localhost:8081"; npm run dev    # Windows PowerShell
```

## Pantallas por rol

| Rol           | Pantallas |
| ------------- | --------- |
| Coordinación  | Inicio, Docentes, Expediente, Calificar, Evaluación de estudiantes, Análisis de comentarios, Agenda de citas |
| Vicerrectoría | Inicio, Docentes, Expediente, Calificar, Cargas de estudiantes, Análisis (consulta), Periodos, Ponderaciones, Facultades, Programas, Usuarios |
| Docente       | Mi evaluación, Mi cita |

El recorrido de demo está en [docs/checklist-demo-sprint3.md](docs/checklist-demo-sprint3.md).

Los usuarios de prueba son los del perfil `dev` del backend.

## Comprobaciones

```bash
npm run lint
npm run build
npm test
```

## Estructura

```
design/            maquetado exportado (referencia, no se empaqueta)
docs/              checklist de la demo
src/
  app/             router y rutas protegidas
  features/        una carpeta por área del negocio (auth, docentes, expedientes…)
  services/        cliente HTTP compartido
  shared/          componentes, hooks y utilidades reutilizables
  styles/          variables y estilos globales
```

## Ramas

Se trabaja con GitFlow: `main` y `develop` son permanentes y cada historia de usuario
sale de `develop` en una rama `feature/{id}-{descripcion-corta}`.

Los commits siguen el formato `feat:`, `fix:`, `refactor:` o `style:` con la descripción en español.
