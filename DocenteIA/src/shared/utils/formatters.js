import { ETIQUETA_FUENTE, NIVELES } from '../constants/dominio.js'

const formatoNota = new Intl.NumberFormat('es-CO', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const formatoFecha = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

/** 4.375 → "4,38". Devuelve "—" si no hay nota. */
export function formatearNota(valor) {
  return valor === null || valor === undefined ? '—' : formatoNota.format(Number(valor))
}

/** 50 → "50 %" */
export function formatearPeso(peso) {
  return `${Number(peso).toLocaleString('es-CO')} %`
}

/** "2026-11-30" → "30 nov 2026", sin desfases de zona horaria. */
export function formatearFecha(fechaIso) {
  if (!fechaIso) return ''
  const [anio, mes, dia] = fechaIso.slice(0, 10).split('-').map(Number)
  return formatoFecha.format(new Date(anio, mes - 1, dia)).replace('.', '')
}

/** "2026-10-20T10:15:42.12" (hora de Colombia) → "20 oct 2026, 10:15". */
export function formatearFechaHora(fechaHoraIso) {
  if (!fechaHoraIso) return ''
  const hora = fechaHoraIso.slice(11, 16)
  return hora ? `${formatearFecha(fechaHoraIso)}, ${hora}` : formatearFecha(fechaHoraIso)
}

/** 1234 → "1.234". Devuelve "—" si no hay valor. */
export function formatearEntero(valor) {
  return valor === null || valor === undefined ? '—' : Number(valor).toLocaleString('es-CO')
}

/** Variación con signo: (0.29, true) → "+0,29"; (-2, false) → "−2". */
export function formatearVariacion(variacion, conDecimales) {
  const numero = Number(variacion)
  const signo = numero > 0 ? '+' : numero < 0 ? '−' : ''
  const absoluto = Math.abs(numero)
  return `${signo}${conDecimales ? formatearNota(absoluto) : formatearEntero(absoluto)}`
}

export function iniciales(nombre = '') {
  return nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase()
}

export function nivelDeNota(valor) {
  if (valor === null || valor === undefined) return null
  return NIVELES.find((nivel) => Number(valor) >= nivel.desde)
}

/** Porcentaje de una nota sobre 5 para el ancho de las barras. */
export function porcentajeNota(valor) {
  if (valor === null || valor === undefined) return '0%'
  return `${Math.max(0, Math.min(100, (Number(valor) / 5) * 100))}%`
}

/** ["COORDINACION", "VICERRECTORIA"] → "Coordinación y Vicerrectoría" */
export function listarFuentes(fuentes = []) {
  const nombres = fuentes.map((fuente) => ETIQUETA_FUENTE[fuente] ?? fuente)
  if (nombres.length <= 1) return nombres.join('')
  return `${nombres.slice(0, -1).join(', ')} y ${nombres.at(-1)}`
}

/** Texto en minúsculas y sin tildes, para buscar sin distinguir "José" de "jose". */
export function normalizarTexto(texto = '') {
  return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
}
