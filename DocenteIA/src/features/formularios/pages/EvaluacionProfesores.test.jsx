import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { obtenerListaHeteroevaluacion } from '../services/formulariosService.js'
import EvaluacionProfesores from './EvaluacionProfesores.jsx'

vi.mock('../services/formulariosService.js')
vi.mock('react-router-dom', () => ({
  Link: ({ to, children }) => <a href={to}>{children}</a>,
  MemoryRouter: ({ children }) => children,
}))

beforeEach(() => vi.clearAllMocks())

describe('EvaluacionProfesores', () => {
  it('lista profesores por clase con estado y avance, sin puntajes', async () => {
    obtenerListaHeteroevaluacion.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      itemsHeteroevaluacion: 11,
      pendientes: 2,
      clases: [
        {
          idClase: 10,
          idDocente: 3,
          nombreDocente: 'Martín Valencia',
          nombreCurso: 'CREATIVIDAD E INNOVACIÓN EMPRENDIMIENTO',
          grupo: 'E',
          jornada: 'S001',
          nombrePrograma: 'ADMINISTRACION DE EMPRESAS PROFESIONAL',
          estado: 'SIN_INICIAR',
          respondidos: 0,
        },
        {
          idClase: 11,
          idDocente: 4,
          nombreDocente: 'Zulema León',
          nombreCurso: 'DESARROLLO DE APLICACIONES MÓVILES',
          grupo: 'A',
          jornada: 'S001',
          nombrePrograma: 'ADMINISTRACION DE EMPRESAS PROFESIONAL',
          estado: 'BORRADOR',
          respondidos: 5,
        },
      ],
    })
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByRole('link', { name: 'Martín Valencia' })).toHaveAttribute(
      'href',
      '/evaluacion-profesores/10/3',
    )
    expect(screen.getByRole('link', { name: 'Zulema León' })).toHaveAttribute(
      'href',
      '/evaluacion-profesores/11/4',
    )
    expect(screen.getByText('Grupo')).toBeInTheDocument()
    expect(screen.getByText('E')).toBeInTheDocument()
    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.getByText('Sin iniciar')).toBeInTheDocument()
    expect(screen.getByText('En borrador')).toBeInTheDocument()
    expect(screen.getByText('0 de 11')).toBeInTheDocument()
    expect(screen.getByText('5 de 11')).toBeInTheDocument()
    expect(screen.queryByText(/puntaje|nota/i)).not.toBeInTheDocument()
  })

  it('muestra el error de carga', async () => {
    obtenerListaHeteroevaluacion.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('No fue posible cargar tus profesores')).toBeInTheDocument()
    expect(screen.getByText('Falló')).toBeInTheDocument()
  })

  it('avisa si no hay periodo activo', async () => {
    obtenerListaHeteroevaluacion.mockResolvedValue(null)
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('No hay un periodo activo')).toBeInTheDocument()
  })
})
