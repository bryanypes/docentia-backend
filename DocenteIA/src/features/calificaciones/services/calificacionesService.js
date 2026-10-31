import { api } from '../../../services/api.js'

const RUTA_FUENTE = {
  COORDINACION: 'coordinacion',
  VICERRECTORIA: 'vicerrectoria',
}

function ruta(idDocente, fuente) {
  return `/docentes/${encodeURIComponent(idDocente)}/calificaciones/${RUTA_FUENTE[fuente]}`
}

/** Calificación ya registrada por la fuente en el periodo activo, o null si aún no existe (204). */
export function obtenerCalificacion(idDocente, fuente) {
  return api.get(ruta(idDocente, fuente))
}

/**
 * Registra o reemplaza la calificación de la fuente. Si ya existía y sus notas cambian, el backend
 * la guarda como corrección con `motivoCorreccion`.
 */
export function guardarCalificacion(idDocente, fuente, calificacion) {
  return api.put(ruta(idDocente, fuente), calificacion)
}

/** Correcciones de notas del docente, de la más reciente a la más antigua. Sin periodo trae todas. */
export function listarCorrecciones(idDocente, idPeriodo) {
  const consulta = idPeriodo ? `?idPeriodo=${encodeURIComponent(idPeriodo)}` : ''
  return api.get(`/docentes/${encodeURIComponent(idDocente)}/calificaciones/correcciones${consulta}`)
}
