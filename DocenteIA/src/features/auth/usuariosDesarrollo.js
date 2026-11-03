/**
 * Usuarios que el seed del perfil dev del backend crea (ver README del backend), elegidos porque
 * tienen datos para recorrer cada rol de punta a punta. Solo se muestran con `import.meta.env.DEV`,
 * así que no llegan al build de producción.
 */
export const CONTRASENA_DESARROLLO = 'Docentia2026*'

export const USUARIOS_DESARROLLO = [
  { rol: 'Vicerrectoría', nombre: 'Toda la universidad', correo: 'vicerrectoria@uniautonoma.edu.co' },
  { rol: 'Coordinación', nombre: 'Ingeniería · 7 docentes con notas', correo: 'coordinacion.ingenieria@uniautonoma.edu.co' },
  { rol: 'Docente', nombre: 'Julián Hoyos · 4,34, comentarios e histórico', correo: 'julian.hoyos@uniautonoma.edu.co' },
  { rol: 'Docente', nombre: 'Laura Demo · formularios de estudiantes', correo: 'laura.demo@uniautonoma.edu.co' },
  { rol: 'Docente', nombre: 'Nora Muñoz · 2,85, acta de compromiso', correo: 'nora.munoz@uniautonoma.edu.co' },
  { rol: 'Estudiante', nombre: 'Estudiante Demo 5 · 2 evaluaciones', correo: 'estudiante.demo5@uniautonoma.edu.co' },
  { rol: 'Estudiante', nombre: 'Karen Mancilla · 3 clases', correo: 'karen.mancilla@uniautonoma.edu.co' },
]
