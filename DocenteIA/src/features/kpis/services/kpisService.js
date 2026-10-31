import { api } from '../../../services/api.js'

/**
 * Indicadores generales del periodo comparados con el anterior. Sin periodo usa el activo (409 si no
 * hay). El coordinador siempre recibe los de su facultad: el backend ignora `facultadId`.
 */
export function obtenerKpis({ periodoId, facultadId } = {}) {
  const parametros = new URLSearchParams()
  if (periodoId) parametros.set('periodoId', periodoId)
  if (facultadId) parametros.set('facultadId', facultadId)
  const consulta = parametros.toString()
  return api.get(`/kpis${consulta ? `?${consulta}` : ''}`)
}

/** Panel agregado por programa (I11). Sin datos individuales. */
export function obtenerPanelProgramas({ periodoId, facultadId }) {
  const consulta = new URLSearchParams()
  if (periodoId) consulta.set('periodoId', periodoId)
  if (facultadId) consulta.set('facultadId', facultadId)
  const texto = consulta.toString()
  return api.get(`/kpis/panel${texto ? `?${texto}` : ''}`)
}
