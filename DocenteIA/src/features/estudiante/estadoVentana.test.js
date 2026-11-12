import { describe, expect, it } from 'vitest'
import { mensajeVentanaCerrada } from './estadoVentana.js'

describe('mensajeVentanaCerrada', () => {
  const ventana = { abierta: false, fechaApertura: '2026-09-28', fechaCierre: '2026-11-13' }

  it('abierta o desconocida: sin mensaje', () => {
    expect(mensajeVentanaCerrada({ ...ventana, abierta: true }, '2026-11-12')).toBeNull()
    expect(mensajeVentanaCerrada(null, '2026-11-12')).toBeNull()
  })

  it('antes de abrir dice que aún no está habilitada', () => {
    expect(mensajeVentanaCerrada(ventana, '2026-09-01').titulo).toBe('La evaluación aún no está habilitada')
  })

  it('después del cierre dice que ya cerró', () => {
    expect(mensajeVentanaCerrada(ventana, '2026-11-14').titulo).toBe('La evaluación ya cerró')
  })
})
