import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { listarEvaluaciones } from '../../estudiante/services/estudianteService.js'
import EvaluacionProfesores from './EvaluacionProfesores.jsx'

vi.mock('../../estudiante/services/estudianteService.js')

beforeEach(() => vi.clearAllMocks())

describe('EvaluacionProfesores', () => {
  it('lista los profesores con su estado y sin notas', async () => {
    listarEvaluaciones.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      pendientes: 1,
      evaluaciones: [
        { idAsignacion: 7, docente: 'Laura', curso: 'Programación web', grupo: 'A', estado: 'PENDIENTE' },
        { idAsignacion: 8, docente: 'Camilo', curso: 'Bases de datos', grupo: 'A', estado: 'ENVIADA' },
      ],
    })
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByRole('link', { name: 'Evaluar a Laura' })).toHaveAttribute('href', '/evaluacion-profesores/7')
    expect(screen.getByText('Enviada')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Evaluar a Camilo' })).not.toBeInTheDocument()
  })

  it('con todo enviado agradece y no dice que no tiene evaluaciones', async () => {
    listarEvaluaciones.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      pendientes: 0,
      evaluaciones: [{ idAsignacion: 8, docente: 'Camilo', curso: 'Bases de datos', grupo: 'A', estado: 'ENVIADA' }],
    })
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('Ya completaste todas tus evaluaciones. ¡Gracias por participar!')).toBeInTheDocument()
    expect(screen.queryByText('No tienes docentes por evaluar')).not.toBeInTheDocument()
  })

  it('muestra vacío y error', async () => {
    listarEvaluaciones.mockResolvedValueOnce({ periodo: { nombre: '2026-2' }, pendientes: 0, evaluaciones: [] })
    const { unmount } = renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('No tienes docentes por evaluar')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveAttribute('href', expect.stringContaining('estudiante'))
    expect(screen.getByRole('button', { name: 'Intentar nuevamente' })).toBeInTheDocument()
    unmount()
    listarEvaluaciones.mockRejectedValueOnce(new ApiError(500, 'Falló'))
    renderizarConSesion(<EvaluacionProfesores />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('No fue posible cargar tus evaluaciones')).toBeInTheDocument()
  })
})
