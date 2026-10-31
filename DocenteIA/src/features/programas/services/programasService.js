import { api } from '../../../services/api.js'

/** Programas académicos; con `idFacultad` solo los de esa facultad. */
export function listarProgramas(idFacultad) {
  const consulta = idFacultad ? `?idFacultad=${encodeURIComponent(idFacultad)}` : ''
  return api.get(`/programas${consulta}`)
}

export function crearPrograma(programa) {
  return api.post('/programas', programa)
}

export function actualizarPrograma(idPrograma, programa) {
  return api.put(`/programas/${encodeURIComponent(idPrograma)}`, programa)
}

export function eliminarPrograma(idPrograma) {
  return api.delete(`/programas/${encodeURIComponent(idPrograma)}`)
}
