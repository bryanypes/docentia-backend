import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import Tutoriales from './Tutoriales.jsx'

describe('Tutoriales', () => {
  it('muestra un video por rol y avisa que los nuevos están pendientes', () => {
    render(
      <MemoryRouter>
        <Tutoriales />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { level: 1, name: 'Aprende a usar Evaluación Docente' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Estamos preparando los nuevos videos de Evaluación Docente')
    expect(screen.getByRole('heading', { name: 'Cómo evaluar a tus profesores' })).toBeInTheDocument()
    expect(screen.getAllByText('Pendiente · muy pronto').length).toBeGreaterThanOrEqual(5)
    expect(screen.getByRole('link', { name: /Inicio/ })).toHaveAttribute('href', '/')
  })
})
