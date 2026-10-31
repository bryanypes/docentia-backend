import { api } from '../../../services/api.js'

/** Umbral de compromisos, rol corrector de actas y pesos predeterminados de la Resolución 074. */
export function obtenerConfiguracionEvaluacion() {
  return api.get('/configuracion/evaluacion')
}
