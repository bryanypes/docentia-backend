import { api } from '../../../services/api.js'

const RUTA = '/periodos-academicos/activo/ponderaciones'

export function obtenerPonderacionActiva() {
  return api.get(RUTA)
}

export function guardarPonderacionActiva(ponderacion) {
  return api.put(RUTA, ponderacion)
}
