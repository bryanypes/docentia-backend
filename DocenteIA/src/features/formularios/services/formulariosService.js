import { api } from '../../../services/api.js'

const AUTO = '/formularios/autoevaluacion'
const coevaluacion = (idDocente) => `/formularios/coevaluacion/${encodeURIComponent(idDocente)}`

export const obtenerAutoevaluacion = () => api.get(AUTO)
export const guardarAutoevaluacion = (borrador) => api.put(AUTO, borrador)
export const enviarAutoevaluacion = () => api.post(`${AUTO}/enviar`)

/** Sin periodo, la del periodo activo; con periodo, la de ese periodo (solo lectura si ya pasó). */
export const obtenerCoevaluacion = (idDocente, periodo) =>
  api.get(periodo ? `${coevaluacion(idDocente)}?periodo=${encodeURIComponent(periodo)}` : coevaluacion(idDocente))
export const guardarCoevaluacion = (idDocente, borrador) => api.put(coevaluacion(idDocente), borrador)
export const enviarCoevaluacion = (idDocente) => api.post(`${coevaluacion(idDocente)}/enviar`)

/** Avance sin puntajes. Si no hay periodo activo devuelve null. */
export async function obtenerPendientesCoevaluacion() {
  try {
    return await api.get('/formularios/coevaluacion/pendientes')
  } catch (error) {
    if (error.status === 409) return null
    throw error
  }
}
