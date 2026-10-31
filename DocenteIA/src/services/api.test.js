import { afterEach, describe, expect, it, vi } from 'vitest'
import { alCerrarSesionPorExpiracion, api } from './api.js'

function responder(status, cuerpo, cabeceras = {}) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(cuerpo === null ? null : JSON.stringify(cuerpo), { status, headers: cabeceras })),
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('api', () => {
  it('muestra el código de soporte en un 5xx', async () => {
    responder(500, { mensaje: 'Error inesperado', correlationId: 'abc-123' })
    await expect(api.get('/x')).rejects.toThrow('Error inesperado Código de soporte: abc-123')
  })

  it('no agrega código en un 4xx', async () => {
    responder(409, { mensaje: 'Etapa cerrada', correlationId: 'abc-123' })
    await expect(api.get('/x')).rejects.toThrow(/^Etapa cerrada$/)
  })

  it('cierra la sesión en un 401', async () => {
    const cerrar = vi.fn()
    alCerrarSesionPorExpiracion(cerrar)
    responder(401, { mensaje: 'Inicia sesión' })
    await expect(api.get('/x')).rejects.toThrow()
    expect(cerrar).toHaveBeenCalled()
  })
})
