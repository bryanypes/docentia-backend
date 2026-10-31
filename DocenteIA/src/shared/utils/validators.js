/** Valida una nota de aspecto: 1.0 a 5.0 con máximo un decimal (regla @NotaAspecto del backend). */
export function validarNotaAspecto(texto) {
  if (texto === '' || texto === null || texto === undefined) return 'La nota es obligatoria'
  const normalizado = String(texto).replace(',', '.')
  if (!/^\d(\.\d)?$/.test(normalizado)) return 'Usa un número con máximo un decimal'
  const valor = Number(normalizado)
  if (valor < 1 || valor > 5) return 'La nota debe estar entre 1,0 y 5,0'
  return null
}

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Valida el inicio de sesión con las reglas de InicioSesionRequest. Devuelve { campo: mensaje }. */
export function validarInicioSesion({ correo, contrasena }) {
  const errores = {}
  if (!correo.trim()) errores.correo = 'Escribe tu correo institucional'
  else if (!PATRON_CORREO.test(correo.trim())) errores.correo = 'Revisa el correo: debe ser como nombre@uniautonoma.edu.co'
  if (!contrasena) errores.contrasena = 'Escribe tu contraseña'
  return errores
}

/** Valida el cambio de contraseña propia con las reglas de CambioContrasenaRequest. */
export function validarCambioContrasena({ contrasenaActual, contrasenaNueva, confirmacion }) {
  const errores = {}
  if (!contrasenaActual) errores.contrasenaActual = 'Escribe tu contraseña actual'
  if (!contrasenaNueva) errores.contrasenaNueva = 'Escribe la nueva contraseña'
  else if (contrasenaNueva.length < 8 || contrasenaNueva.length > 72) {
    errores.contrasenaNueva = 'La contraseña debe tener entre 8 y 72 caracteres'
  } else if (contrasenaNueva === contrasenaActual) {
    errores.contrasenaNueva = 'La nueva contraseña debe ser distinta de la actual'
  }
  if (contrasenaNueva && confirmacion !== contrasenaNueva) errores.confirmacion = 'Las contraseñas no coinciden'
  return errores
}

/**
 * Valida el formulario de usuario con las reglas de UsuarioRequest. Devuelve { campo: mensaje }
 * solo con los campos inválidos. El backend vuelve a validar todo.
 */
export function validarUsuario(formulario, { contrasenaObligatoria }) {
  const errores = {}
  if (!formulario.nombreCompleto.trim()) errores.nombreCompleto = 'El nombre es obligatorio'
  if (!formulario.correo.trim()) errores.correo = 'El correo es obligatorio'
  else if (!PATRON_CORREO.test(formulario.correo.trim())) errores.correo = 'El correo no tiene un formato válido'
  if (!formulario.rol) errores.rol = 'Elige un rol'
  if (formulario.rol === 'COORDINADOR' && !formulario.idFacultad) errores.idFacultad = 'Elige la facultad que coordina'
  if (formulario.rol === 'DOCENTE' && !formulario.idPrograma) errores.idPrograma = 'Elige el programa del docente'
  const { contrasena } = formulario
  if (!contrasena && contrasenaObligatoria) errores.contrasena = 'La contraseña es obligatoria'
  else if (contrasena && (contrasena.length < 8 || contrasena.length > 72)) {
    errores.contrasena = 'La contraseña debe tener entre 8 y 72 caracteres'
  }
  return errores
}

export function aNumero(texto) {
  return Number(String(texto).replace(',', '.'))
}
