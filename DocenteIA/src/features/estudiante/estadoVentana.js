/**
 * Mensaje para el estudiante según la ventana de recolección: «aún no» si hoy es antes de la
 * apertura, «ya cerró» en otro caso. Sin ventana conocida, null.
 */
export function mensajeVentanaCerrada(ventana, hoy = new Date().toLocaleDateString('en-CA', { timeZone: 'America/Bogota' })) {
  if (!ventana || ventana.abierta) return null
  if (ventana.fechaApertura && hoy < ventana.fechaApertura) {
    return { titulo: 'La evaluación aún no está habilitada', texto: `Podrás responder desde el ${ventana.fechaApertura}.` }
  }
  return { titulo: 'La evaluación ya cerró', texto: 'La ventana de recolección de este periodo terminó.' }
}
