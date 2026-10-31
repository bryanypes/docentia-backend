import { api } from '../../../services/api.js'

export function solicitarAnalisis(docenteId, periodoId) {
  return api.post('/analisis-ia/solicitudes', { docenteId: Number(docenteId), periodoId: Number(periodoId) })
}

/** Solicitud con su borrador. `senal` cancela la consulta al salir de la pantalla. */
export function obtenerSolicitud(idSolicitud, senal) {
  return api.get(`/analisis-ia/solicitudes/${encodeURIComponent(idSolicitud)}`, { senal })
}

/** Solicitudes del docente en el periodo, la más reciente primero. */
export function listarSolicitudes(docenteId, periodoId) {
  const consulta = new URLSearchParams({ docenteId, periodoId })
  return api.get(`/analisis-ia/solicitudes?${consulta}`)
}

export function listarComentariosFiltrados(idSolicitud) {
  return api.get(`/analisis-ia/solicitudes/${encodeURIComponent(idSolicitud)}/comentarios-filtrados`)
}

export function validarHallazgo(idHallazgo) {
  return api.post(`/analisis-ia/hallazgos/${encodeURIComponent(idHallazgo)}/validacion`)
}
