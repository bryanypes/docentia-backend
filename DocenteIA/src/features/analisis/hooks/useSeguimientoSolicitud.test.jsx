import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as analisisService from '../services/analisisService.js'
import { INTERVALO_SONDEO_MS, MAXIMO_CONSULTAS, useSeguimientoSolicitud } from './useSeguimientoSolicitud.js'

vi.mock('../services/analisisService.js')

function solicitud(estado) {
  return { idSolicitud: 1, estado }
}

async function avanzar(ms) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

describe('useSeguimientoSolicitud', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('consulta mientras está en curso y se detiene al terminar', async () => {
    analisisService.obtenerSolicitud
      .mockResolvedValueOnce(solicitud('EN_COLA'))
      .mockResolvedValueOnce(solicitud('EN_PROCESO'))
      .mockResolvedValueOnce(solicitud('COMPLETADO'))
    const { result } = renderHook(() => useSeguimientoSolicitud(1))

    await avanzar(0)
    expect(result.current.solicitud.estado).toBe('EN_COLA')
    await avanzar(INTERVALO_SONDEO_MS)
    expect(result.current.solicitud.estado).toBe('EN_PROCESO')
    await avanzar(INTERVALO_SONDEO_MS)
    expect(result.current.solicitud.estado).toBe('COMPLETADO')
    await avanzar(INTERVALO_SONDEO_MS * 5)

    expect(analisisService.obtenerSolicitud).toHaveBeenCalledTimes(3)
  })

  it('se detiene en el tope y avisa que se agotó la espera', async () => {
    analisisService.obtenerSolicitud.mockResolvedValue(solicitud('EN_COLA'))
    const { result } = renderHook(() => useSeguimientoSolicitud(1))

    await avanzar(INTERVALO_SONDEO_MS * (MAXIMO_CONSULTAS + 5))

    expect(analisisService.obtenerSolicitud).toHaveBeenCalledTimes(MAXIMO_CONSULTAS)
    expect(result.current.agotado).toBe(true)
  })

  it('al salir de la pantalla cancela la petición y no vuelve a consultar', async () => {
    analisisService.obtenerSolicitud.mockResolvedValue(solicitud('EN_PROCESO'))
    const { unmount } = renderHook(() => useSeguimientoSolicitud(1))
    await avanzar(0)
    const senal = analisisService.obtenerSolicitud.mock.calls[0][1]

    unmount()
    await avanzar(INTERVALO_SONDEO_MS * 4)

    expect(senal.aborted).toBe(true)
    expect(analisisService.obtenerSolicitud).toHaveBeenCalledTimes(1)
  })
})
