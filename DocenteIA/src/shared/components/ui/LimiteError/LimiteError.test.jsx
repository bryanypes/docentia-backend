import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { LimiteError } from './LimiteError.jsx'

function Rota() {
  throw new Error('fallo de render')
}

describe('LimiteError', () => {
  it('muestra un mensaje en vez de dejar la pantalla en blanco', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    render(
      <LimiteError>
        <Rota />
      </LimiteError>,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Algo salió mal')
    vi.restoreAllMocks()
  })
})
