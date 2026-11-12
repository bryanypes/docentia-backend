import { api } from '../../../services/api.js'

/** Cobertura de la evaluación de estudiantes: solo cuenta respuestas, nunca dice quién respondió. */
export function obtenerCobertura(pagina = 0) {
  return api.get(`/cobertura-heteroevaluacion?page=${pagina}&size=20`)
}
