import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
  NavLink: ({ to, children, onClick }) => (
    <a
      href={to}
      onClick={(evento) => {
        evento.preventDefault()
        onClick?.(evento)
      }}
    >
      {children}
    </a>
  ),
  useLocation: () => ({ pathname: '/' }),
  MemoryRouter: ({ children }) => children,
}))

describe('Sidebar', () => {
  it('el coordinador no ve la carga CSV de estudiantes ni la matrícula, que están ocultas', () => {
    renderizarConSesion(<Sidebar />, { rol: 'COORDINADOR' })
    expect(screen.getByRole('link', { name: 'Coevaluaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Evaluación de estudiantes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Matrícula y cobertura' })).not.toBeInTheDocument()
  })

  it('la vicerrectoría ve su menú en Seguimiento y Administración, sin Auditoría ni Programas', () => {
    renderizarConSesion(<Sidebar />, { rol: 'VICERRECTOR' })
    expect(screen.getByText('Seguimiento')).toBeInTheDocument()
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Facultades y programas' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Auditoría' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Programas' })).not.toBeInTheDocument()
  })

  it('el estudiante solo ve sus evaluaciones', () => {
    renderizarConSesion(<Sidebar />, { rol: 'ESTUDIANTE' })
    expect(screen.getByRole('link', { name: 'Mis evaluaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Docentes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mi evaluación' })).not.toBeInTheDocument()
  })

  it('en el celular, elegir un destino cierra el menú y la X también lo cierra', async () => {
    const onNavegar = vi.fn()
    const onCerrar = vi.fn()
    renderizarConSesion(<Sidebar id="menu-principal" movil onNavegar={onNavegar} onCerrar={onCerrar} />, {
      rol: 'VICERRECTOR',
    })
    await userEvent.click(screen.getByRole('link', { name: 'Docentes' }))
    expect(onNavegar).toHaveBeenCalledTimes(1)
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar menú' }))
    expect(onCerrar).toHaveBeenCalledTimes(1)
  })
})
