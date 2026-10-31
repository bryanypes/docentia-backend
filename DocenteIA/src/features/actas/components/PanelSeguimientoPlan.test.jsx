import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { marcarCumplido, obtenerPlanDocente, obtenerPlanPropio } from '../services/actasService.js'
import { PanelSeguimientoPlan } from './PanelSeguimientoPlan.jsx'

vi.mock('../services/actasService.js')

const PLAN = {
  periodoEvaluado: { idPeriodo: 1, nombre: '2026-1' },
  compromisos: [
    { idCompromiso: 5, descripcion: 'Entregar plan', fechaLimite: '2026-09-30', estado: 'VENCIDO', historial: [] },
  ],
}

beforeEach(() => vi.clearAllMocks())

describe('PanelSeguimientoPlan', () => {
  it('la coordinación marca cumplido con observación', async () => {
    obtenerPlanDocente.mockResolvedValue(PLAN)
    marcarCumplido.mockResolvedValue(PLAN)
    renderizarConSesion(<PanelSeguimientoPlan idDocente={3} puedeMarcar />)
    expect(await screen.findByText('Vencido')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Marcar cumplido' }))
    await userEvent.type(screen.getByLabelText('Observación'), 'Verificado')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar cumplimiento' }))
    expect(marcarCumplido).toHaveBeenCalledWith(5, 'Verificado')
  })

  it('el docente solo consulta los suyos', async () => {
    obtenerPlanPropio.mockResolvedValue(PLAN)
    renderizarConSesion(<PanelSeguimientoPlan />, { rol: 'DOCENTE' })
    expect(await screen.findByText('Entregar plan')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar cumplido' })).not.toBeInTheDocument()
  })
})
