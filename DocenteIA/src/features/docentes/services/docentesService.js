import { api } from '../../../services/api.js'

/** Docentes dentro del alcance del usuario. El coordinador siempre recibe los de su facultad. */
export function listarDocentes(idFacultad) {
  const consulta = idFacultad ? `?idFacultad=${encodeURIComponent(idFacultad)}` : ''
  return api.get(`/docentes${consulta}`)
}

/**
 * Docentes del alcance con el promedio del periodo activo: { periodo, docentes: [{ docente, promedio }] }.
 * El promedio incompleto trae las fuentes que faltan; sin periodo activo, periodo y promedios van en null.
 */
export function listarSeguimientoDocentes(idFacultad) {
  const consulta = idFacultad ? `?idFacultad=${encodeURIComponent(idFacultad)}` : ''
  return api.get(`/docentes/seguimiento${consulta}`)
}
