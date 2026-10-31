import { api } from '../../../services/api.js'

export function listarNotificaciones({ soloNoLeidas = false, pagina = 0, tamano = 20 } = {}) {
  const consulta = new URLSearchParams({ soloNoLeidas, page: pagina, size: tamano })
  return api.get(`/notificaciones?${consulta}`)
}

export function contarNoLeidas(senal) {
  return api.get('/notificaciones/no-leidas/conteo', { senal })
}

/** La campana lo escucha para actualizar el contador sin esperar a la siguiente consulta. */
export const EVENTO_CAMBIO = 'docentia:notificaciones'

function avisarCambio(resultado) {
  window.dispatchEvent(new Event(EVENTO_CAMBIO))
  return resultado
}

export function marcarLeida(id) {
  return api.patch(`/notificaciones/${encodeURIComponent(id)}/leida`).then(avisarCambio)
}

export function marcarTodasLeidas() {
  return api.patch('/notificaciones/leer-todas').then(avisarCambio)
}

export function obtenerPreferencias() {
  return api.get('/notificaciones/preferencias')
}

export function guardarPreferencias(correoHabilitado) {
  return api.put('/notificaciones/preferencias', { correoHabilitado })
}
