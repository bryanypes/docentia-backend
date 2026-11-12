import { ROLES } from '../shared/constants/dominio.js'

export function rutaInicioDelRol(rol) {
  if (rol === ROLES.DOCENTE) return '/mi-evaluacion'
  if (rol === ROLES.ESTUDIANTE) return '/evaluacion-profesores'
  return '/inicio'
}

/** Pantalla para cambiar la contraseña propia; obligatoria mientras la actual sea temporal. */
export const RUTA_CAMBIO_CONTRASENA = '/cambiar-contrasena'
