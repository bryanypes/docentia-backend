import { api } from '../../../services/api.js'

/** Expediente del docente autenticado, sin notas individuales de coordinación ni vicerrectoría. */
export function obtenerExpedientePropio() {
  return api.get('/expedientes/propio')
}

/** Expediente completo de un docente, para coordinación y vicerrectoría. */
export function obtenerExpedienteDocente(idDocente) {
  return api.get(`/expedientes/${encodeURIComponent(idDocente)}`)
}
