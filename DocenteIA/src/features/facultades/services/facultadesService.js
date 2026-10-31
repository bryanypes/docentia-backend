import { api } from '../../../services/api.js'

/** Facultades dentro del alcance del usuario (el coordinador solo recibe la suya). */
export function listarFacultades() {
  return api.get('/facultades')
}

export function crearFacultad(facultad) {
  return api.post('/facultades', facultad)
}

export function actualizarFacultad(idFacultad, facultad) {
  return api.put(`/facultades/${encodeURIComponent(idFacultad)}`, facultad)
}

export function eliminarFacultad(idFacultad) {
  return api.delete(`/facultades/${encodeURIComponent(idFacultad)}`)
}
