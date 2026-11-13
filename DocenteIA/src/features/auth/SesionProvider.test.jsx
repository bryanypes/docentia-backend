import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, guardarToken, leerToken } from '../../services/api.js'
import { SesionProvider } from './SesionProvider.jsx'
import * as authService from './services/authService.js'
import { useSesion } from './sesionContext.js'

vi.mock('./services/authService.js')

function Estado() {
  const { usuario, validando } = useSesion()
  return <p>{validando ? 'validando' : usuario ? `con sesión ${usuario.nombreCompleto}` : 'sin sesión'}</p>
}

function montar() {
  render(
    <SesionProvider>
      <Estado />
    </SesionProvider>,
  )
}

describe('SesionProvider', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    guardarToken('token-de-prueba')
  })
  afterEach(() => guardarToken(null))

  it('con un 401 la sesión ya no vale y se cierra', async () => {
    authService.obtenerSesionActual.mockRejectedValue(new ApiError(401, 'Sesión vencida'))
    montar()
    expect(await screen.findByText('sin sesión')).toBeInTheDocument()
    expect(leerToken()).toBeNull()
  })

  it('un error de red o una validación abortada no cierra la sesión', async () => {
    authService.obtenerSesionActual.mockRejectedValue(new TypeError('Failed to fetch'))
    montar()
    await waitFor(() => expect(screen.getByText('sin sesión')).toBeInTheDocument(), { timeout: 5000 })
    expect(authService.obtenerSesionActual).toHaveBeenCalledTimes(3)
    expect(leerToken()).toBe('token-de-prueba')
  })

  it('si el reintento responde, la sesión sigue', async () => {
    authService.obtenerSesionActual
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce({ idUsuario: 1, nombreCompleto: 'Persona de prueba', rol: 'DOCENTE' })
    montar()
    expect(await screen.findByText('con sesión Persona de prueba', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(leerToken()).toBe('token-de-prueba')
  })
})
