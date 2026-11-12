import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import Coevaluaciones from './Coevaluaciones.jsx'

vi.mock('../services/formulariosService.js')

beforeEach(() => vi.clearAllMocks())

describe('Coevaluaciones', () => {
  it('lista estado y avance sin puntajes', async () => {
    obtenerPendientesCoevaluacion.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      itemsCoevaluacion: 6,
      pendientes: 1,
      docentes: [{ idDocente: 1, nombreDocente: 'Nora', programa: 'Software', estado: 'BORRADOR', respondidos: 2 }],
    })
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByRole('link', { name: 'Nora' })).toHaveAttribute('href', '/coevaluaciones/1')
    expect(screen.getByText('En borrador')).toBeInTheDocument()
    expect(screen.getByText('2 de 6')).toBeInTheDocument()
  })

  it('muestra el error', async () => {
    obtenerPendientesCoevaluacion.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByText('No fue posible cargar las coevaluaciones')).toBeInTheDocument()
  })
})
