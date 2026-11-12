export const DURACION_FRANJA_MIN = 30

function aMinutos(hora) {
  const [horas, minutos] = hora.split(':').map(Number)
  return horas * 60 + minutos
}

function aHora(minutos) {
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
