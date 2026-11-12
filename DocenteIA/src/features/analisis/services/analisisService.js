import { api } from '../../../services/api.js'

/** Con un análisis ya generado, pedir otro es regenerar: el backend exige motivo y una hora de espera. */
export function solicitarAnalisis(docenteId, periodoId, motivo) {
  const cuerpo = { docenteId: Number(docenteId), periodoId: Number(periodoId) }
  if (motivo) cuerpo.motivo = motivo
  return api.post('/analisis-ia/solicitudes', cuerpo)
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

export function descartarHallazgo(idHallazgo, motivo) {
  return api.post(`/analisis-ia/hallazgos/${encodeURIComponent(idHallazgo)}/descarte`, { motivo: motivo || null })
}

export function editarHallazgo(idHallazgo, texto) {
  return api.put(`/analisis-ia/hallazgos/${encodeURIComponent(idHallazgo)}`, { texto })
}

export function listarVersionesHallazgo(idHallazgo) {
  return api.get(`/analisis-ia/hallazgos/${encodeURIComponent(idHallazgo)}/versiones`)
}

/** Cierra la revisión: desde aquí el docente ve los hallazgos incluidos. */
export function publicarRevision(idSolicitud) {
  return api.post(`/analisis-ia/solicitudes/${encodeURIComponent(idSolicitud)}/publicacion`)
}

/** Hallazgos publicados del docente autenticado. */
export function obtenerResultadoPropio() {
  return api.get('/analisis-ia/resultados/propio')
}

/** Panorama del análisis del alcance (solo conteos). Sin periodo, el activo. */
export function obtenerPanorama(periodoId) {
  return api.get(`/analisis-ia/panorama${periodoId ? `?periodoId=${encodeURIComponent(periodoId)}` : ''}`)
}
