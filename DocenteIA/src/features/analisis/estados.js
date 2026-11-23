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

/** Motivos con los que el backend marca un análisis que hizo el modelo local en lugar del externo. */
export const MOTIVOS_RESPALDO_LOCAL = [
  'PROVEEDOR_CONFIGURACION',
  'PROVEEDOR_NO_DISPONIBLE',
  'RESPUESTA_INVALIDA',
  'RESPUESTA_CORTADA',
  'LLM_NO_DISPONIBLE',
]

/** Pidieron el modelo externo, falló, y el borrador lo hizo el modelo local por reglas. */
export function usoRespaldoLocal(solicitud) {
  return solicitud?.estado === ESTADOS_SOLICITUD.COMPLETADO && MOTIVOS_RESPALDO_LOCAL.includes(solicitud?.motivo)
}

export const ESTADOS_HALLAZGO = {
  PENDIENTE_VALIDACION: { etiqueta: 'Pendiente de validar', clase: 'info' },
  SIN_CITA_VERIFICADA: { etiqueta: 'Sin cita verificada', clase: 'error' },
  VALIDADO: { etiqueta: 'Validado', clase: 'exito' },
  DESCARTADO: { etiqueta: 'Descartado', clase: 'neutro' },
}

export const ETIQUETA_VERSION = {
  GENERADO: 'Generado por el modelo',
  INCLUIDO: 'Incluido',
  DESCARTADO: 'Descartado',
  EDITADO: 'Texto editado',
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

/** Mínimo de comentarios de estudiantes que exige el backend (docentia.ia.minimo-comentarios). */
export const MINIMO_COMENTARIOS = 5
