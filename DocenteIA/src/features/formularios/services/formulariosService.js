import { api } from '../../../services/api.js'

const AUTO = '/formularios/autoevaluacion'
const coevaluacion = (idDocente) => `/formularios/coevaluacion/${encodeURIComponent(idDocente)}`
const hetero = (idClase, idDocente) =>
  `/formularios/heteroevaluacion/${encodeURIComponent(idClase)}/${encodeURIComponent(idDocente)}`

export const obtenerAutoevaluacion = () => api.get(AUTO)
export const guardarAutoevaluacion = (borrador) => api.put(AUTO, borrador)
export const enviarAutoevaluacion = () => api.post(`${AUTO}/enviar`)

export const obtenerCoevaluacion = (idDocente) => api.get(coevaluacion(idDocente))
export const guardarCoevaluacion = (idDocente, borrador) => api.put(coevaluacion(idDocente), borrador)
export const enviarCoevaluacion = (idDocente) => api.post(`${coevaluacion(idDocente)}/enviar`)

export const obtenerHeteroevaluacion = (idClase, idDocente) => api.get(hetero(idClase, idDocente))
export const guardarHeteroevaluacion = (idClase, idDocente, borrador) => api.put(hetero(idClase, idDocente), borrador)
export const enviarHeteroevaluacion = (idClase, idDocente) => api.post(`${hetero(idClase, idDocente)}/enviar`)

/** Avance sin puntajes. Si no hay periodo activo devuelve null. */
export async function obtenerPendientesCoevaluacion() {
  try {
    return await api.get('/formularios/coevaluacion/pendientes')
  } catch (error) {
    if (error.status === 409) return null
    throw error
  }
}

/** Clases del estudiante con estado, sin puntajes. Si no hay periodo activo devuelve null. */
export async function obtenerListaHeteroevaluacion() {
  try {
    return await api.get('/formularios/heteroevaluacion')
  } catch (error) {
    if (error.status === 409) return null
    throw error
  }
}
