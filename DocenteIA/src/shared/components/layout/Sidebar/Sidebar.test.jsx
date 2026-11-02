import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../../pruebas/renderizarConSesion.jsx'
import { Sidebar } from './Sidebar.jsx'

vi.mock('../../../../features/formularios/services/formulariosService.js', () => ({
  obtenerPendientesCoevaluacion: () => Promise.resolve(null),
}))
vi.mock('react-router-dom', () => ({
  Link: ({ to, children, ...rest }) => (
    <a href={to} {...rest}>
      {children}
    </a>
  ),
  NavLink: ({ to, children }) => <a href={to}>{children}</a>,
  useLocation: () => ({ pathname: '/' }),
  MemoryRouter: ({ children }) => children,
}))

describe('Sidebar', () => {
  it('el coordinador no ve la carga de CSV de estudiantes', () => {
    renderizarConSesion(<Sidebar />, { rol: 'COORDINADOR' })
    expect(screen.getByRole('link', { name: 'Coevaluaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Evaluación de estudiantes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /CSV|Excel|carga/i })).not.toBeInTheDocument()
  })

  it('el vicerrector no ve el menú de cargas de estudiantes', () => {
    renderizarConSesion(<Sidebar />, { rol: 'VICERRECTOR' })
    expect(screen.getByRole('link', { name: 'Docentes' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Cargas de estudiantes' })).not.toBeInTheDocument()
  })
})
