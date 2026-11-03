import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LimiteError } from './LimiteError.jsx'

let fallar = true
function Pantalla() {
  if (fallar) throw new Error('fallo de render')
  return <p>Pantalla lista</p>
}

describe('LimiteError', () => {
  beforeEach(() => {
    fallar = true
    sessionStorage.clear()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('muestra a DocenteIA con el error y deja recargar a mano', async () => {
    render(
      <LimiteError enLinea>
        <Pantalla />
      </LimiteError>,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Algo salió mal')
    expect(screen.getByText(/Reintentando en 8 segundos/)).toBeInTheDocument()

    fallar = false
    await userEvent.click(screen.getByRole('button', { name: 'Recargar ahora' }))
    expect(screen.getByText('Pantalla lista')).toBeInTheDocument()
  })

  it('se reintenta sola y, tras dos intentos seguidos, espera al usuario', () => {
    vi.useFakeTimers()
    render(
      <LimiteError enLinea>
        <Pantalla />
      </LimiteError>,
    )
    // Dos reintentos automáticos que vuelven a fallar.
    for (let i = 0; i < 2; i++) {
      act(() => vi.advanceTimersByTime(8000))
    }
    expect(screen.getByRole('alert')).toHaveTextContent('Ya lo intentamos varias veces')
    expect(screen.queryByText(/Reintentando en/)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir al inicio' })).toHaveAttribute('href', '/inicio')
  })
})
