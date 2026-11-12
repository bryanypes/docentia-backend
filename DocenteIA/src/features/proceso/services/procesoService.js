import { api } from '../../../services/api.js'

/** Proceso de los docentes del alcance en el periodo activo. */
export function listarProcesos(idFacultad) {
  return api.get(`/procesos-evaluacion${idFacultad ? `?idFacultad=${encodeURIComponent(idFacultad)}` : ''}`)
}

export function obtenerProceso(idDocente) {
  return api.get(`/procesos-evaluacion/${encodeURIComponent(idDocente)}`)
}

/** Proceso del docente autenticado. */
export function obtenerProcesoPropio() {
  return api.get('/procesos-evaluacion/propio')
}

/** El docente confirma (true) o declina (false) su cita. */
export function responderCita(asiste) {
  return api.post('/citas/mia/confirmacion', { asiste })
}

/** La coordinación registra si el docente asistió a la reunión. */
export function registrarAsistencia(idCita, asistio) {
  return api.post(`/citas/${encodeURIComponent(idCita)}/asistencia`, { asistio })
}
