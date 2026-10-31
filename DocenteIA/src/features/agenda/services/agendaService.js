import { api } from '../../../services/api.js'

/** Franjas propias de la coordinación en el periodo activo. */
export function listarFranjas({ fecha, estado } = {}) {
  const parametros = new URLSearchParams()
  if (fecha) parametros.set('fecha', fecha)
  if (estado) parametros.set('estado', estado)
  const consulta = parametros.toString()
  return api.get(`/franjas${consulta ? `?${consulta}` : ''}`)
}

export function abrirFranja(franja) {
  return api.post('/franjas', franja)
}

export function abrirFranjasEnLote(franjas) {
  return api.post('/franjas/lote', { accion: 'ABRIR', franjas })
}

export function cerrarFranjasEnLote(ids) {
  return api.post('/franjas/lote', { accion: 'CERRAR', ids })
}

export function cerrarFranja(idFranja) {
  return api.patch(`/franjas/${encodeURIComponent(idFranja)}/cierre`)
}

export function eliminarFranja(idFranja) {
  return api.delete(`/franjas/${encodeURIComponent(idFranja)}`)
}

/** Franjas que el docente puede reservar. */
export function listarFranjasDisponibles() {
  return api.get('/franjas/disponibles')
}

export function reservarFranja(idFranja) {
  return api.post(`/franjas/${encodeURIComponent(idFranja)}/reserva`)
}

/** Cita activa del docente, o null si no tiene (204). */
export function obtenerMiCita() {
  return api.get('/citas/mia')
}
