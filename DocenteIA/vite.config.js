import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Destino del proxy de desarrollo; se cambia si el 8080 está ocupado:
// DOCENTIA_API_URL=http://localhost:8081 npm run dev
const destinoApi = process.env.DOCENTIA_API_URL ?? 'http://localhost:8080'

// Subruta del despliegue (por ejemplo /Evadocente-app/). Se fija al compilar con VITE_BASE_PATH;
// el router la toma de import.meta.env.BASE_URL.
const base = process.env.VITE_BASE_PATH?.trim() || '/'

// https://vite.dev/config/
export default defineConfig({
  base: base.endsWith('/') ? base : `${base}/`,
  plugins: [react()],
  server: {
    // El backend no tiene CORS: en desarrollo las peticiones a /api se reenvían a Spring Boot.
    proxy: {
      '/api': destinoApi,
    },
  },
  test: {
    include: ['src/**/*.test.{js,jsx}'],
    environment: 'jsdom',
    setupFiles: './src/configuracionPruebas.js',
    // Las pantallas con varios pasos y modales superan los 5 s por defecto en máquinas cargadas.
    testTimeout: 15000,
  },
})
