/** Estado de una solicitud de corrección, con texto y tono de píldora. */
export const ESTADO_SOLICITUD = {
  PENDIENTE_APROBACION: { etiqueta: 'Esperando verificación', clase: 'alerta' },
  APROBADA: { etiqueta: 'Aprobada · la atiende administración', clase: 'info' },
  RECHAZADA: { etiqueta: 'Rechazada', clase: 'neutro' },
  ATENDIDA: { etiqueta: 'Formulario reabierto', clase: 'exito' },
}

export const ETIQUETA_TIPO = {
  AUTO: 'Autoevaluación',
  COEVAL: 'Coevaluación',
}

export const ETIQUETA_APROBADOR = {
  COORDINADOR: 'la coordinación',
  VICERRECTOR: 'la vicerrectoría',
}

export const MOTIVO_MINIMO = 15
export const RESPUESTA_MINIMA = 10

/** Quién la tiene ahora, en palabras. */
export function quienLaTiene(solicitud) {
  if (solicitud.estado === 'PENDIENTE_APROBACION') return `La está verificando ${ETIQUETA_APROBADOR[solicitud.rolAprobador]}.`
  if (solicitud.estado === 'APROBADA') return 'La administración reabrirá el formulario.'
  if (solicitud.estado === 'ATENDIDA') return 'Ya puedes corregir el formulario y enviarlo de nuevo.'
  return solicitud.respuestaAdministrador || solicitud.respuestaAprobador || 'La solicitud fue rechazada.'
}
