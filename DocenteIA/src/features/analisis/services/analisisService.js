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

/**
 * Resumen agregado de los comentarios del periodo en el alcance (GET /analisis-comentarios/resumen).
 * `programa` es opcional. Con pocos docentes analizados llega `disponible: false`.
 */
export function obtenerResumenComentarios({ periodo, programa }) {
  const parametros = new URLSearchParams({ periodo })
  if (programa) parametros.set('programa', programa)
  return api.get(`/analisis-comentarios/resumen?${parametros}`)
}

/** Análisis de comentarios de un docente en un periodo (contrato api-comentarios-ia.md). */
export function obtenerAnalisisDocente(idDocente, periodo) {
  return api.get(`/analisis-comentarios/docentes/${encodeURIComponent(idDocente)}?periodo=${encodeURIComponent(periodo)}`)
}

/** Comentarios anonimizados del análisis vigente, paginados y filtrables por criterio y sentimiento. */
export function listarComentariosDocente(idDocente, { periodo, criterio, sentimiento, pagina = 0, tamano = 10 }) {
  const parametros = new URLSearchParams({ periodo, page: pagina, size: tamano })
  if (criterio) parametros.set('criterio', criterio)
  if (sentimiento) parametros.set('sentimiento', sentimiento)
  return api.get(`/comentarios-estudiantes/docentes/${encodeURIComponent(idDocente)}?${parametros}`)
}
