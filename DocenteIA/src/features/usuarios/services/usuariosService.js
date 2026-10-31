import { api } from '../../../services/api.js'

/** Usuarios ordenados por nombre; con `rol` solo los de ese rol. Solo vicerrectoría. */
export function listarUsuarios(rol) {
  const consulta = rol ? `?rol=${encodeURIComponent(rol)}` : ''
  return api.get(`/usuarios${consulta}`)
}

export function obtenerUsuario(idUsuario) {
  return api.get(`/usuarios/${encodeURIComponent(idUsuario)}`)
}

export function crearUsuario(usuario) {
  return api.post('/usuarios', usuario)
}

/** Reemplaza los datos del usuario. Sin contraseña se conserva la actual. */
export function actualizarUsuario(idUsuario, usuario) {
  return api.put(`/usuarios/${encodeURIComponent(idUsuario)}`, usuario)
}

/** Activa o desactiva la cuenta. Desactivarla corta su sesión de inmediato. Nadie puede desactivarse a sí mismo. */
export function cambiarEstadoUsuario(idUsuario, esActivo) {
  return api.patch(`/usuarios/${encodeURIComponent(idUsuario)}/estado`, { esActivo })
}
