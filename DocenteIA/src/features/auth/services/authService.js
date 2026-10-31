import { api } from '../../../services/api.js'

export function iniciarSesion(correo, contrasena) {
  return api.post('/sesiones', { correo, contrasena }, { publica: true })
}

export function obtenerSesionActual() {
  return api.get('/sesiones/actual')
}

/** Cambia la contraseña propia. Devuelve una sesión nueva { token, usuario } sin la marca de temporal. */
export function cambiarContrasena(contrasenaActual, contrasenaNueva) {
  return api.put('/sesiones/actual/contrasena', { contrasenaActual, contrasenaNueva })
}
