import { ROLES } from '../shared/constants/dominio.js'

export function rutaInicioDelRol(rol) {
  return rol === ROLES.DOCENTE ? '/mi-evaluacion' : '/inicio'
}

/** Pantalla para cambiar la contraseña propia; obligatoria mientras la actual sea temporal. */
export const RUTA_CAMBIO_CONTRASENA = '/cambiar-contrasena'
