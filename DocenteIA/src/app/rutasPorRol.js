import { ROLES } from '../shared/constants/dominio.js'

/** Casi todos los roles entran por /inicio; lo que muestra depende del rol (InicioPorRol). */
export const RUTA_INICIO = '/inicio'

/** Administración y visualización no tienen panel en /inicio: entran a su primera pantalla. */
const INICIO_PROPIO = { ADMINISTRADOR: '/usuarios', VISUALIZADOR: '/panel-programas' }

export function rutaInicioDelRol(rol) {
  return INICIO_PROPIO[rol] ?? RUTA_INICIO
}

/** Panel principal que ve cada rol en /inicio. */
export function panelInicioDelRol(rol) {
  if (rol === ROLES.DOCENTE) return 'MI_EVALUACION'
  if (rol === ROLES.ESTUDIANTE) return 'EVALUACION_PROFESORES'
  if (rol === ROLES.ADMINISTRADOR || rol === ROLES.VISUALIZADOR) return 'REDIRIGIR'
  return 'GESTION'
}

/** Pantalla para cambiar la contraseña propia; obligatoria mientras la actual sea temporal. */
export const RUTA_CAMBIO_CONTRASENA = '/cambiar-contrasena'
