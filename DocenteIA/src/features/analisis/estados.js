export const ESTADOS_SOLICITUD = {
  EN_COLA: 'EN_COLA',
  EN_PROCESO: 'EN_PROCESO',
  COMPLETADO: 'COMPLETADO',
  INSUFICIENTE: 'INSUFICIENTE',
  ERROR: 'ERROR',
}

export const ETIQUETA_SOLICITUD = {
  EN_COLA: 'En cola',
  EN_PROCESO: 'Analizando',
  COMPLETADO: 'Borrador listo',
  INSUFICIENTE: 'Evidencia insuficiente',
  ERROR: 'No se pudo analizar',
}

export function estaEnCurso(solicitud) {
  return solicitud?.estado === ESTADOS_SOLICITUD.EN_COLA || solicitud?.estado === ESTADOS_SOLICITUD.EN_PROCESO
}

export const ESTADOS_HALLAZGO = {
  PENDIENTE_VALIDACION: { etiqueta: 'Pendiente de validar', clase: 'info' },
  SIN_CITA_VERIFICADA: { etiqueta: 'Sin cita verificada', clase: 'error' },
  VALIDADO: { etiqueta: 'Validado', clase: 'exito' },
}

export const TIPOS_HALLAZGO = [
  { tipo: 'FORTALEZA', nombre: 'Fortalezas' },
  { tipo: 'OPORTUNIDAD_DE_MEJORA', nombre: 'Oportunidades de mejora' },
]

export const CATEGORIAS_FILTRO = {
  INSULTO: 'Insulto o lenguaje ofensivo',
  APARIENCIA: 'Apariencia física',
  EDAD: 'Edad',
  GENERO: 'Género',
  VIDA_PRIVADA: 'Vida privada',
}

/** Mínimo de encuestas que exige el backend (docentia.ia.minimo-encuestas). */
export const MINIMO_ENCUESTAS = 10
