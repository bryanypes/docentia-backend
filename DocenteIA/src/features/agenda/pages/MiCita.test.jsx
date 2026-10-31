import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import * as agendaService from '../services/agendaService.js'
import MiCita from './MiCita.jsx'

vi.mock('../services/agendaService.js')

const DISPONIBLES = [
  { idFranja: 1, fecha: '2026-11-09', horaInicio: '08:00:00', horaFin: '08:30:00', nombreCoordinador: 'Andrés Ordóñez' },
  { idFranja: 2, fecha: '2026-11-09', horaInicio: '08:30:00', horaFin: '09:00:00', nombreCoordinador: 'Andrés Ordóñez' },
]

const CITA = { ...DISPONIBLES[0], idCita: 7, idCitaLiberada: null }

function renderizar() {
  renderizarConSesion(<MiCita />, { rol: 'DOCENTE' })
}

describe('MiCita', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    agendaService.listarFranjasDisponibles.mockResolvedValue(DISPONIBLES)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('sin cita reserva directamente', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(null)
    agendaService.reservarFranja.mockResolvedValue(CITA)
    const confirmar = vi.spyOn(window, 'confirm')
    renderizar()

    expect(await screen.findByText('Todavía no tienes cita')).toBeInTheDocument()
    await userEvent.click((await screen.findAllByRole('button', { name: /^Reservar/ }))[0])

    expect(confirmar).not.toHaveBeenCalled()
    expect(agendaService.reservarFranja).toHaveBeenCalledWith(1)
    expect(await screen.findByText(/Cita reservada/)).toBeInTheDocument()
  })

  it('con una cita activa avisa que la anterior se liberará y respeta el cancelar', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(CITA)
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderizar()

    expect(await screen.findByText('Tu cita')).toBeInTheDocument()
    await userEvent.click((await screen.findAllByRole('button', { name: /^Reservar/ }))[1])

    expect(confirmar.mock.calls[0][0]).toContain('Tu cita anterior')
    expect(confirmar.mock.calls[0][0]).toContain('se liberará')
    expect(agendaService.reservarFranja).not.toHaveBeenCalled()
  })

  it('si otra persona tomó la franja muestra el 409 y recarga la lista', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(null)
    agendaService.reservarFranja.mockRejectedValue(new ApiError(409, 'La franja ya fue reservada por otro docente'))
    renderizar()

    await userEvent.click((await screen.findAllByRole('button', { name: /^Reservar/ }))[0])

    expect(await screen.findByRole('alert')).toHaveTextContent('La franja ya fue reservada por otro docente')
    expect(screen.getByText(/La lista de franjas se actualizó/)).toBeInTheDocument()
    expect(agendaService.listarFranjasDisponibles).toHaveBeenCalledTimes(2)
  })
})
