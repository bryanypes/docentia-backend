import { api } from '../../services/api.js'

/** Si el usuario de la sesión ya vio el tutorial: { visto }. */
export function obtenerTutorial() {
  return api.get('/sesiones/actual/tutorial')
}

/** Lo terminó o lo saltó: no se le vuelve a mostrar. */
export function marcarTutorialVisto() {
  return api.put('/sesiones/actual/tutorial')
}

/** Quiere verlo otra vez. */
export function reiniciarTutorial() {
  return api.delete('/sesiones/actual/tutorial')
}

/** Evento con el que otra pantalla (Mi perfil) pide abrir el tutorial ahora mismo. */
export const EVENTO_VER_TUTORIAL = 'docentia:ver-tutorial'

export function pedirTutorial() {
  window.dispatchEvent(new Event(EVENTO_VER_TUTORIAL))
}
