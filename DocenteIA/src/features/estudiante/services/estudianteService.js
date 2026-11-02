import { api } from '../../../services/api.js'

/** Asignaciones del estudiante; null si no hay periodo activo. */
export async function listarEvaluaciones() {
  try {
    return await api.get('/estudiante/evaluaciones')
  } catch (error) {
    if (error.status === 409) return null
    throw error
  }
}

export const obtenerEvaluacion = (idAsignacion) =>
  api.get(`/estudiante/evaluaciones/${encodeURIComponent(idAsignacion)}`)

/** Envío definitivo. La respuesta solo confirma el estado. */
export const enviarEvaluacion = (idAsignacion, envio) =>
  api.post(`/estudiante/evaluaciones/${encodeURIComponent(idAsignacion)}/enviar`, envio)
