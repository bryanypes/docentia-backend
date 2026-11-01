import { api } from '../../../services/api.js'

/** Correcciones de notas del docente, de la más reciente a la más antigua. Sin periodo trae todas. */
export function listarCorrecciones(idDocente, idPeriodo) {
  const consulta = idPeriodo ? `?idPeriodo=${encodeURIComponent(idPeriodo)}` : ''
  return api.get(`/docentes/${encodeURIComponent(idDocente)}/calificaciones/correcciones${consulta}`)
}
