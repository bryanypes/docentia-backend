/**
 * Estados del proceso de evaluación (EstadoProcesoEvaluacion del backend), en orden. Cada uno tiene
 * su tono de color y un texto que dice qué falta; el color nunca va solo.
 */
export const ESTADOS_PROCESO = {
  EVALUACIONES_EN_CURSO: {
    etiqueta: 'Evaluaciones en curso',
    tono: 'neutro',
    ayuda: 'Faltan las evaluaciones de estudiantes o de coordinación.',
  },
  PENDIENTE_AUTOEVALUACION: {
    etiqueta: 'Falta la autoevaluación',
    tono: 'alerta',
    ayuda: 'Estudiantes y coordinación ya calificaron; solo falta la autoevaluación del docente.',
  },
  EVALUACION_COMPLETA: {
    etiqueta: 'Evaluación completa',
    tono: 'cielo',
    ayuda: 'Las tres evaluaciones están listas. Falta que el docente escoja un horario.',
  },
  NO_ASISTIO: {
    etiqueta: 'No asistió',
    tono: 'coral',
    ayuda: 'No llegó a su cita. Debe reservar una nueva franja.',
  },
  CITA_AGENDADA: {
    etiqueta: 'Cita agendada',
    tono: 'violeta',
    ayuda: 'Escogió horario; falta que confirme su asistencia.',
  },
  ASISTENCIA_CONFIRMADA: {
    etiqueta: 'Asistencia confirmada',
    tono: 'turquesa',
    ayuda: 'Confirmó que irá. En la reunión se registra su asistencia.',
  },
  EN_REUNION: {
    etiqueta: 'En reunión',
    tono: 'info',
    ayuda: 'Asistió: ya ve su resultado. Falta firmar el acta.',
  },
  ACTA_EN_FIRMA: {
    etiqueta: 'Acta en firma',
    tono: 'rosa',
    ayuda: 'Una de las partes ya firmó; falta la otra.',
  },
  FINALIZADO: {
    etiqueta: 'Proceso finalizado',
    tono: 'exito',
    ayuda: 'Acta firmada por el docente y la coordinación.',
  },
}

export const ORDEN_ESTADOS = Object.keys(ESTADOS_PROCESO)

/** Los pasos que dibuja la línea del proceso. `paso` coincide con el del backend. */
export const PASOS_PROCESO = [
  { paso: 1, titulo: 'Estudiantes y coordinación' },
  { paso: 2, titulo: 'Autoevaluación' },
  { paso: 3, titulo: 'Escoge horario' },
  { paso: 4, titulo: 'Cita agendada' },
  { paso: 5, titulo: 'Confirma asistencia' },
  { paso: 6, titulo: 'Reunión' },
  { paso: 7, titulo: 'Firma del acta' },
  { paso: 8, titulo: 'Finalizado' },
]

export const ETIQUETA_TIPO_ACTA = {
  COMPROMISO: 'Acta de compromiso',
  RETROALIMENTACION: 'Acta de retroalimentación',
}

export const ETIQUETA_CONFIRMACION = {
  CONFIRMADA: 'Confirmó asistencia',
  DECLINADA: 'Declinó la cita',
}

export const ETIQUETA_ASISTENCIA = {
  ASISTIO: 'Asistió',
  NO_ASISTIO: 'No asistió',
}

/** ¿La coordinación ya puede registrar la asistencia? Desde el día de la cita y una sola vez. */
export function puedeRegistrarAsistencia(cita, hoy) {
  return Boolean(cita?.activa && !cita.asistencia && cita.fecha <= hoy)
}

const ORIGEN = { CSV: 'cargada por CSV', MANUAL: 'registrada sin formularios' }

/**
 * Explica la nota de estudiantes: cuántos respondieron el formulario en la app y, si la nota no sale
 * de ahí, de dónde salió. Evita que "0 de 0" contradiga un paso ya cumplido.
 * @returns {{ titulo: string, detalle: string|null }}
 */
export function describirCobertura(cobertura) {
  if (!cobertura) return { titulo: '—', detalle: null }
  const { respondidas, esperadas, minimoRespuestas: minimo, origenNota } = cobertura
  if (origenNota === 'FORMULARIO') {
    return { titulo: `${respondidas} de ${esperadas} respondieron`, detalle: 'La nota sale de sus formularios.' }
  }
  if (esperadas === 0) {
    return origenNota
      ? { titulo: 'Sin matrícula en la app', detalle: `La nota de estudiantes está ${ORIGEN[origenNota]}.` }
      : { titulo: 'Sin matrícula en la app', detalle: 'Aún no hay estudiantes matriculados en sus cursos.' }
  }
  const faltan = Math.max(0, minimo - respondidas)
  const umbral =
    faltan > 0
      ? `${faltan === 1 ? 'Falta 1' : `Faltan ${faltan}`} para usar el formulario (mínimo ${minimo}, por anonimato).`
      : null
  const origen = origenNota ? `La nota actual está ${ORIGEN[origenNota]}.` : null
  return { titulo: `${respondidas} de ${esperadas} respondieron`, detalle: [umbral, origen].filter(Boolean).join(' ') || null }
}
