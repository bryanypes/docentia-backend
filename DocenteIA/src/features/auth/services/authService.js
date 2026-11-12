import { api } from '../../../services/api.js'

export function iniciarSesion(correo, contrasena) {
  return api.post('/sesiones', { correo, contrasena }, { publica: true })
}

export function iniciarSesionConGoogle(idToken) {
  return api.post('/sesiones/google', { idToken }, { publica: true })
}

export function obtenerSesionActual() {
  return api.get('/sesiones/actual')
}

/** Cambia la contraseña propia. Devuelve una sesión nueva { token, usuario } sin la marca de temporal. */
export function cambiarContrasena(contrasenaActual, contrasenaNueva) {
  return api.put('/sesiones/actual/contrasena', { contrasenaActual, contrasenaNueva })
}

/** Perfil propio: nombre, correo, rol y facultad y programa por nombre. */
export function obtenerPerfil() {
  return api.get('/sesiones/actual/perfil')
}

/** Siempre responde lo mismo, exista o no el correo. */
export function solicitarRecuperacion(correo) {
  return api.post('/sesiones/recuperacion', { correo }, { publica: true })
}

export function restablecerContrasena(token, contrasena) {
  return api.post('/sesiones/recuperacion/restablecimiento', { token, contrasena }, { publica: true })
}
