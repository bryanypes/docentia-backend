import { ROLES } from '../shared/constants/dominio.js'

/** Todos los roles entran por la misma ruta; lo que muestra depende del rol (InicioPorRol). */
export const RUTA_INICIO = '/inicio'

export function rutaInicioDelRol() {
  return RUTA_INICIO
}

/** Panel principal que ve cada rol en /inicio. */
export function panelInicioDelRol(rol) {
  if (rol === ROLES.DOCENTE) return 'MI_EVALUACION'
  if (rol === ROLES.ESTUDIANTE) return 'EVALUACION_PROFESORES'
  return 'GESTION'
}

/** Pantalla para cambiar la contraseña propia; obligatoria mientras la actual sea temporal. */
export const RUTA_CAMBIO_CONTRASENA = '/cambiar-contrasena'
