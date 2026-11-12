import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import Presentacion from './Presentacion.jsx'

describe('Presentacion', () => {
  it('explica qué es, los tres pasos y lleva al inicio de sesión', () => {
    render(
      <MemoryRouter>
        <Presentacion />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { level: 1, name: 'Evaluación docente de punta a punta' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cómo funciona' })).toBeInTheDocument()
    const enlaces = screen.getAllByRole('link', { name: 'Iniciar sesión' })
    expect(enlaces.length).toBeGreaterThan(0)
    for (const enlace of enlaces) expect(enlace).toHaveAttribute('href', '/login')
  })
})
