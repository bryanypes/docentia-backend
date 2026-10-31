import { describe, expect, it } from 'vitest'
import { agruparPorFecha, finDeFranja, franjasEntre } from './franjas.js'

describe('franjas', () => {
  it('el fin es 30 minutos después y la última franja empieza a las 23:00', () => {
    expect(finDeFranja('08:00')).toBe('08:30')
    expect(finDeFranja('09:45')).toBe('10:15')
    expect(finDeFranja('23:00')).toBe('23:30')
    expect(finDeFranja('23:30')).toBeNull()
    expect(finDeFranja('')).toBeNull()
  })

  it('arma franjas seguidas sin pasarse de la hora final', () => {
    expect(franjasEntre('2026-11-11', '08:00', '09:15')).toEqual([
      { fecha: '2026-11-11', horaInicio: '08:00', horaFin: '08:30' },
      { fecha: '2026-11-11', horaInicio: '08:30', horaFin: '09:00' },
    ])
    expect(franjasEntre('2026-11-11', '10:00', '10:00')).toEqual([])
  })

  it('agrupa por fecha conservando el orden', () => {
    const franjas = [
      { idFranja: 1, fecha: '2026-11-09' },
      { idFranja: 2, fecha: '2026-11-09' },
      { idFranja: 3, fecha: '2026-11-10' },
    ]
    expect(agruparPorFecha(franjas).map(([fecha, lista]) => [fecha, lista.length])).toEqual([
      ['2026-11-09', 2],
      ['2026-11-10', 1],
    ])
  })
})
