import { api } from '../../../services/api.js'

export function listarNotificaciones({ soloNoLeidas = false, pagina = 0, tamano = 20 } = {}) {
  const consulta = new URLSearchParams({ soloNoLeidas, page: pagina, size: tamano })
  return api.get(`/notificaciones?${consulta}`)
}

export function contarNoLeidas(senal) {
  return api.get('/notificaciones/no-leidas/conteo', { senal })
}

export function marcarLeida(id) {
  return api.patch(`/notificaciones/${encodeURIComponent(id)}/leida`)
}

export function marcarTodasLeidas() {
  return api.patch('/notificaciones/leer-todas')
}

export function obtenerPreferencias() {
  return api.get('/notificaciones/preferencias')
}

export function guardarPreferencias(correoHabilitado) {
  return api.put('/notificaciones/preferencias', { correoHabilitado })
}
