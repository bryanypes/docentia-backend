/**
 * Copia local de lo que el estudiante respondió. El servidor guarda las respuestas sin saber quién
 * las dio (anonimato del formulario), así que la única forma de volver a verlas es esta copia en el
 * navegador del propio estudiante. La clave lleva su usuario: otra cuenta en el mismo equipo no la ve.
 */
const PREFIJO = 'docentia.resumen-evaluacion'

const clave = (idUsuario, idAsignacion) => `${PREFIJO}.${idUsuario}.${idAsignacion}`

/** @param envio { respuestas: {codigo: valor}, comentario } */
export function guardarResumen(idUsuario, idAsignacion, envio, ahora = new Date()) {
  try {
    localStorage.setItem(
      clave(idUsuario, idAsignacion),
      JSON.stringify({ respuestas: envio.respuestas, comentario: envio.comentario ?? null, enviadaEn: ahora.toISOString() }),
    )
  } catch {
    // Sin almacenamiento (modo privado, bloqueado): solo habrá comprobante.
  }
}

/** @returns { respuestas, comentario, enviadaEn } o null si no hay copia en este navegador */
export function leerResumen(idUsuario, idAsignacion) {
  try {
    const guardado = JSON.parse(localStorage.getItem(clave(idUsuario, idAsignacion)) ?? 'null')
    return guardado && typeof guardado.respuestas === 'object' ? guardado : null
  } catch {
    return null
  }
}
