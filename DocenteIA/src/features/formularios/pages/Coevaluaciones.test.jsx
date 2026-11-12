import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import Coevaluaciones from './Coevaluaciones.jsx'

vi.mock('../services/formulariosService.js')

beforeEach(() => vi.clearAllMocks())

const PENDIENTES = {
  periodo: { nombre: '2026-2' },
  itemsCoevaluacion: 6,
  pendientes: 1,
  docentes: [
    { idDocente: 1, nombreDocente: 'Nora', programa: 'Software', estado: 'BORRADOR', respondidos: 2 },
    { idDocente: 2, nombreDocente: 'Tomás', programa: 'Derecho', estado: 'ENVIADA', respondidos: 6 },
  ],
}

describe('Coevaluaciones', () => {
  it('lista estado y avance sin puntajes', async () => {
    obtenerPendientesCoevaluacion.mockResolvedValue({
      ...PENDIENTES,
      docentes: [PENDIENTES.docentes[0]],
    })
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByRole('link', { name: 'Continuar · Nora' })).toHaveAttribute('href', '/coevaluaciones/1')
    expect(screen.getByRole('link', { name: 'Ver expediente · Nora' })).toHaveAttribute('href', '/docentes/1')
    expect(screen.getByText('En borrador', { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByText('2 de 6')).toBeInTheDocument()
  })

  it('marca como realizada la coevaluación enviada y filtra por estado', async () => {
    obtenerPendientesCoevaluacion.mockResolvedValue(PENDIENTES)
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByRole('link', { name: 'Ver coevaluación · Tomás' })).toHaveAttribute('href', '/coevaluaciones/2')
    expect(screen.getByText('Realizada', { selector: 'span' })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText(/Estado/), 'ENVIADA')
    expect(screen.queryByRole('link', { name: 'Continuar · Nora' })).not.toBeInTheDocument()
    expect(screen.getByText('1', { selector: 'strong' })).toBeInTheDocument()
  })

  it('filtra por nombre o programa y limpia los filtros', async () => {
    obtenerPendientesCoevaluacion.mockResolvedValue(PENDIENTES)
    renderizarConSesion(<Coevaluaciones />)
    await screen.findByRole('link', { name: 'Continuar · Nora' })

    await userEvent.type(screen.getByRole('searchbox'), 'derecho')
    expect(screen.queryByRole('link', { name: 'Continuar · Nora' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver coevaluación · Tomás' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }))
    expect(screen.getByRole('link', { name: 'Continuar · Nora' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver coevaluación · Tomás' })).toBeInTheDocument()
  })

  it('muestra el error', async () => {
    obtenerPendientesCoevaluacion.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<Coevaluaciones />)
    expect(await screen.findByText('No fue posible cargar las coevaluaciones')).toBeInTheDocument()
  })
})
