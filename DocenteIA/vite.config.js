import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Destino del proxy de desarrollo; se cambia si el 8080 está ocupado:
// DOCENTIA_API_URL=http://localhost:8081 npm run dev
const destinoApi = process.env.DOCENTIA_API_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // El backend no tiene CORS: en desarrollo las peticiones a /api se reenvían a Spring Boot.
    proxy: {
      '/api': destinoApi,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/configuracionPruebas.js',
  },
})
