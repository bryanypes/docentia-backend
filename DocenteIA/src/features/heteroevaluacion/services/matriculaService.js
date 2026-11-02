import { api } from '../../../services/api.js'

/** Importa la matrícula. Un 422 trae el reporte por fila en `error.datos`. */
export function importarMatricula(archivo) {
  const datos = new FormData()
  datos.append('archivo', archivo)
  return api.post('/matriculas/csv', datos)
}

export const descargarPlantilla = () => api.get('/matriculas/plantilla', { binario: true })

export function obtenerCobertura(pagina = 0) {
  return api.get(`/cobertura-heteroevaluacion?page=${pagina}&size=20`)
}
