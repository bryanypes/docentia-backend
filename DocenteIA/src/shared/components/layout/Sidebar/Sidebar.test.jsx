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
  it('el coordinador tiene la carga de estudiantes y la matrícula', () => {
    renderizarConSesion(<Sidebar />, { rol: 'COORDINADOR' })
    expect(screen.getByRole('link', { name: 'Coevaluaciones' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Evaluación de estudiantes' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Matrícula y cobertura' })).toBeInTheDocument()
  })

  it('el estudiante solo ve sus evaluaciones', () => {
    renderizarConSesion(<Sidebar />, { rol: 'ESTUDIANTE' })
    expect(screen.getByRole('link', { name: 'Mis evaluaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Docentes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mi evaluación' })).not.toBeInTheDocument()
  })
})
