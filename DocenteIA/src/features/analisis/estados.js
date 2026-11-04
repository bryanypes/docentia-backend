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

/** Mínimo de encuestas que exige el backend (docentia.ia.minimo-encuestas). */
export const MINIMO_ENCUESTAS = 10

/** Cómo presentar cada estado del análisis de comentarios (contrato api-comentarios-ia.md). */
export const ESTADO_ANALISIS = {
  EN_COLA: { robot: 'esperando', tono: 'violeta', titulo: 'El análisis está en cola' },
  EN_PROCESO: { robot: 'pensando', tono: 'violeta', titulo: 'Estoy analizando los comentarios' },
  INSUFICIENTE: { robot: 'error', tono: 'alerta', titulo: 'Evidencia insuficiente' },
  ERROR: { robot: 'error', tono: 'coral', titulo: 'No pude generar el análisis' },
  SIN_COMENTARIOS: { robot: 'esperando', tono: 'azul', titulo: 'No hubo comentarios útiles' },
  NO_DISPONIBLE_AUN: { robot: 'esperando', tono: 'violeta', titulo: 'Todavía no hay análisis' },
}

/** Texto inicial de un compromiso a partir de una oportunidad de mejora del análisis. */
export function compromisoDesdeTema(tema) {
  const nombre = tema.nombre.charAt(0).toLowerCase() + tema.nombre.slice(1)
  return `Fortalecer ${nombre}, señalado en ${tema.frecuencia} ${tema.frecuencia === 1 ? 'comentario' : 'comentarios'} de estudiantes`
}
