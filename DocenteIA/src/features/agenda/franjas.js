export const DURACION_FRANJA_MIN = 30

export function aMinutos(hora) {
  const [horas, minutos] = hora.split(':').map(Number)
  return horas * 60 + minutos
}

export function aHora(minutos) {
  return `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`
}

/** "08:00" → "08:30". 23:30–00:00 lo rechaza la base, así que la última franja empieza a las 23:00. */
export function finDeFranja(horaInicio) {
  if (!horaInicio) return null
  const fin = aMinutos(horaInicio) + DURACION_FRANJA_MIN
  return fin >= 24 * 60 ? null : aHora(fin)
}

/** Franjas consecutivas de 30 minutos entre dos horas, sin pasarse de la final. */
export function franjasEntre(fecha, desde, hasta) {
  const franjas = []
  for (let inicio = aMinutos(desde); inicio + DURACION_FRANJA_MIN <= aMinutos(hasta); inicio += DURACION_FRANJA_MIN) {
    franjas.push({ fecha, horaInicio: aHora(inicio), horaFin: aHora(inicio + DURACION_FRANJA_MIN) })
  }
  return franjas
}

/** Agrupa una lista ordenada por fecha: [[fecha, [franjas…]], …]. */
export function agruparPorFecha(franjas) {
  const grupos = new Map()
  for (const franja of franjas) {
    if (!grupos.has(franja.fecha)) grupos.set(franja.fecha, [])
    grupos.get(franja.fecha).push(franja)
  }
  return [...grupos.entries()]
}

/** "08:00:00" → "08:00". */
export function formatearHora(hora) {
  return hora ? hora.slice(0, 5) : ''
}

/** Hoy en Colombia como AAAA-MM-DD, para no ofrecer fechas pasadas. */
export function hoyEnColombia() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date())
}

function aFecha(fechaIso) {
  return new Date(`${fechaIso}T00:00:00Z`)
}

/** Suma días a una fecha AAAA-MM-DD sin depender de la zona horaria del navegador. */
export function sumarDias(fechaIso, dias) {
  const fecha = aFecha(fechaIso)
  fecha.setUTCDate(fecha.getUTCDate() + dias)
  return fecha.toISOString().slice(0, 10)
}

/** Lunes de la semana de una fecha. */
export function lunesDe(fechaIso) {
  const diaSemana = aFecha(fechaIso).getUTCDay()
  return sumarDias(fechaIso, diaSemana === 0 ? -6 : 1 - diaSemana)
}

/** Fechas de lunes a sábado desde un lunes. */
export function diasDeSemana(lunes, cantidad = 6) {
  return Array.from({ length: cantidad }, (_, i) => sumarDias(lunes, i))
}

const FORMATO_DIA = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', timeZone: 'UTC' })
const FORMATO_DIA_LARGO = new Intl.DateTimeFormat('es-CO', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  timeZone: 'UTC',
})
const FORMATO_MES = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', timeZone: 'UTC' })

/** "lun 9" para la cabecera de la columna. */
export function etiquetaDia(fechaIso) {
  return FORMATO_DIA.format(aFecha(fechaIso))
}

/** "lunes, 9 de noviembre" para los lectores de pantalla y el detalle. */
export function etiquetaDiaLarga(fechaIso) {
  return FORMATO_DIA_LARGO.format(aFecha(fechaIso))
}

/** "9 nov – 14 nov" para el título de la semana. */
export function etiquetaSemana(dias) {
  return `${FORMATO_MES.format(aFecha(dias[0]))} – ${FORMATO_MES.format(aFecha(dias[dias.length - 1]))}`
}

/** Primer día del mes de una fecha: "2026-11-17" → "2026-11-01". */
export function inicioDeMes(fechaIso) {
  return `${fechaIso.slice(0, 7)}-01`
}

/** Suma meses a un primer día de mes. */
export function sumarMeses(inicioMes, meses) {
  const fecha = aFecha(inicioMes)
  fecha.setUTCMonth(fecha.getUTCMonth() + meses)
  return fecha.toISOString().slice(0, 10)
}

/** Las 6 semanas (de lunes a domingo) que muestran un mes completo. */
export function semanasDelMes(inicioMes) {
  const primero = lunesDe(inicioMes)
  return Array.from({ length: 6 }, (_, s) => diasDeSemana(sumarDias(primero, s * 7), 7))
}

const FORMATO_MES_LARGO = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' })

/** "noviembre de 2026" */
export function etiquetaMes(inicioMes) {
  return FORMATO_MES_LARGO.format(aFecha(inicioMes))
}
