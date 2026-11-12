import { ETAPAS } from '../../shared/constants/dominio.js'
import { formatearFecha } from '../../shared/utils/formatters.js'

/** Texto con las fechas de una etapa. Sirve con una ventana o con el cuerpo del 409 de etapa cerrada. */
export function describirFechasEtapa({ fechaApertura, fechaCierre }) {
  if (!fechaApertura || !fechaCierre) return 'Aún no está programada; la vicerrectoría debe definir sus fechas.'
  return `Sus fechas son del ${formatearFecha(fechaApertura)} al ${formatearFecha(fechaCierre)}.`
}

/** Las cuatro etapas en orden, con lo que cada una habilita. */
export const ETAPAS_PERIODO = [
  {
    etapa: ETAPAS.RECOLECCION_CALIFICACIONES,
    nombre: 'Recolección de calificaciones',
    habilita: 'Calificar y cargar el CSV de estudiantes.',
  },
  {
    etapa: ETAPAS.REVISION_HALLAZGOS,
    nombre: 'Revisión de hallazgos',
    habilita: 'Solicitar el análisis y validar hallazgos.',
  },
  { etapa: ETAPAS.AGENDA_CITAS, nombre: 'Agenda de citas', habilita: 'Abrir franjas y reservar citas.' },
  { etapa: ETAPAS.FIRMA_ACTAS, nombre: 'Firma de actas', habilita: 'Firmar las actas de retroalimentación.' },
]

/**
 * Revisa las fechas con las mismas reglas del backend, que vuelve a validar todo. Las fechas son
 * textos AAAA-MM-DD, así que se comparan directamente. Devuelve una lista de mensajes.
 */
export function validarFechasPeriodo({ fechaInicio, fechaFin, etapas }) {
  const errores = []
  if (fechaInicio && fechaFin && fechaFin < fechaInicio) {
    errores.push('La fecha de fin del periodo no puede ser anterior a la de inicio.')
  }
  for (const { tipoEtapa, fechaApertura, fechaCierre } of etapas) {
    const { nombre } = ETAPAS_PERIODO.find(({ etapa }) => etapa === tipoEtapa)
    if (!fechaApertura || !fechaCierre) errores.push(`${nombre}: necesita fecha de apertura y de cierre.`)
    else if (fechaCierre < fechaApertura) errores.push(`${nombre}: el cierre no puede ser antes de la apertura.`)
    else if ((fechaInicio && fechaApertura < fechaInicio) || (fechaFin && fechaCierre > fechaFin)) {
      errores.push(`${nombre}: debe quedar dentro de las fechas del periodo.`)
    }
  }
  return errores
}
