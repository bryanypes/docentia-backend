import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { obtenerConfiguracionEvaluacion } from '../../kpis/services/configuracionService.js'
import { obtenerExpedientePropio } from '../services/expedientesService.js'
import MiEvaluacion from './MiEvaluacion.jsx'

vi.mock('../services/expedientesService.js')
vi.mock('../../kpis/services/configuracionService.js')

const SIN_NOTA = { estado: 'SIN_NOTA', valor: null, fuentesPendientes: ['ESTUDIANTES', 'COORDINACION', 'AUTOEVALUACION'] }

beforeEach(() => {
  vi.clearAllMocks()
  obtenerConfiguracionEvaluacion.mockResolvedValue({})
})

describe('MiEvaluacion sin materias', () => {
  it('sin clases ni notas en el periodo explica que no hay materias y ofrece ayuda', async () => {
    obtenerExpedientePropio.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      promedio: SIN_NOTA,
      dimensiones: [],
      compromisosVigentes: [],
      resultadoHabilitado: true,
      clasesEnPeriodo: 0,
    })
    renderizarConSesion(<MiEvaluacion />, { rol: 'DOCENTE' })

    expect(await screen.findByText('No tienes materias asignadas en este periodo')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Intentar nuevamente' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveAttribute('href', expect.stringContaining('docente'))
  })

  it('con notas aunque no tenga clases registradas no muestra el aviso', async () => {
    obtenerExpedientePropio.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      promedio: { estado: 'PARCIAL', valor: null, fuentesPendientes: ['AUTOEVALUACION'] },
      dimensiones: [],
      compromisosVigentes: [],
      resultadoHabilitado: true,
      clasesEnPeriodo: 0,
    })
    renderizarConSesion(<MiEvaluacion />, { rol: 'DOCENTE' })

    expect(await screen.findByRole('heading', { name: /Mi evaluación 2026-2/ })).toBeInTheDocument()
    expect(screen.queryByText('No tienes materias asignadas en este periodo')).not.toBeInTheDocument()
  })
})
