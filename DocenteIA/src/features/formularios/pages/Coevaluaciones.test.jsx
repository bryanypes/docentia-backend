import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
    expect(await screen.findByRole('link', { name: 'Continuar · Nora' })).toHaveAttribute('href', '/coevaluaciones/1')
    expect(screen.getByRole('link', { name: 'Ver expediente · Nora' })).toHaveAttribute('href', '/docentes/1')
    const tarjeta = screen.getByText('Nora').closest('li')
    expect(within(tarjeta).getByText('En borrador')).toBeInTheDocument()
    expect(within(tarjeta).getByText('2 de 6')).toBeInTheDocument()
  })

  it('los chips cuentan cada estado y, con la búsqueda, filtran las tarjetas', async () => {
    obtenerPendientesCoevaluacion.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      itemsCoevaluacion: 6,
      pendientes: 2,
      docentes: [
        { idDocente: 1, nombreDocente: 'Nora Muñoz', programa: 'Software', estado: 'BORRADOR', respondidos: 2 },
        { idDocente: 2, nombreDocente: 'Luis Paz', programa: 'Software', estado: 'SIN_INICIAR', respondidos: 0 },
        { idDocente: 3, nombreDocente: 'Ana Ruiz', programa: 'Derecho', estado: 'ENVIADA', respondidos: 6 },
      ],
    })
    renderizarConSesion(<Coevaluaciones />)
    const enviadas = await screen.findByRole('button', { name: 'Enviada 1' })
    expect(screen.getByRole('button', { name: 'Todos 3' })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(enviadas)
    expect(screen.getByText('Ana Ruiz')).toBeInTheDocument()
    expect(screen.queryByText('Luis Paz')).not.toBeInTheDocument()
    expect(screen.getByText('1 de 3')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Todos 3' }))
    await userEvent.type(screen.getByPlaceholderText('Buscar docente'), 'munoz')
    expect(screen.getByText('Nora Muñoz')).toBeInTheDocument()
    expect(screen.queryByText('Ana Ruiz')).not.toBeInTheDocument()
  })

  it('muestra el error', async () => {
    obtenerPendientesCoevaluacion.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByText('No fue posible cargar las coevaluaciones')).toBeInTheDocument()
  })
})
