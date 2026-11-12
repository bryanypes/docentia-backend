import { api } from '../../../services/api.js'

/** Docentes dentro del alcance del usuario. El coordinador siempre recibe los de su facultad. */
export function listarDocentes(idFacultad) {
  const consulta = idFacultad ? `?idFacultad=${encodeURIComponent(idFacultad)}` : ''
  return api.get(`/docentes${consulta}`)
}

/**
 * Docentes del alcance con su promedio: { periodo, docentes: [{ docente, promedio }] }. Sin `periodoId`
 * usa el activo; `idPrograma` filtra por carrera. El promedio incompleto trae las fuentes que faltan;
 * sin periodo, periodo y promedios van en null.
 */
export function listarSeguimientoDocentes(idFacultad, { periodoId, idPrograma } = {}) {
  const parametros = new URLSearchParams()
  if (idFacultad) parametros.set('idFacultad', idFacultad)
  if (periodoId) parametros.set('periodoId', periodoId)
  if (idPrograma) parametros.set('idPrograma', idPrograma)
  const consulta = parametros.toString()
  return api.get(`/docentes/seguimiento${consulta ? `?${consulta}` : ''}`)
}
