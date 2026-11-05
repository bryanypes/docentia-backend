import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import * as procesoService from '../../proceso/services/procesoService.js'
import * as agendaService from '../services/agendaService.js'
import MiCita from './MiCita.jsx'

vi.mock('../services/agendaService.js')
vi.mock('../../proceso/services/procesoService.js')

const DISPONIBLES = [
  { idFranja: 1, fecha: '2026-11-09', horaInicio: '08:00:00', horaFin: '08:30:00', nombreCoordinador: 'Andrés Ordóñez' },
  { idFranja: 2, fecha: '2026-11-09', horaInicio: '08:30:00', horaFin: '09:00:00', nombreCoordinador: 'Andrés Ordóñez' },
  { idFranja: 3, fecha: '2026-11-12', horaInicio: '10:00:00', horaFin: '10:30:00', nombreCoordinador: 'Andrés Ordóñez' },
]

const CITA = { ...DISPONIBLES[0], idCita: 7, idCitaLiberada: null, confirmacion: null, asistencia: null }

function renderizar() {
  renderizarConSesion(<MiCita />, { rol: 'DOCENTE' })
}

describe('MiCita', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-02T17:00:00Z'))
    agendaService.listarFranjasDisponibles.mockResolvedValue(DISPONIBLES)
    procesoService.obtenerProcesoPropio.mockResolvedValue({ estado: 'EVALUACIONES_EN_CURSO', paso: 1, hitos: [] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('el calendario resalta los días con horarios y la reserva se confirma en un modal', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(null)
    agendaService.reservarFranja.mockResolvedValue({ ...CITA })
    renderizar()

    expect(await screen.findByText('Todavía no tienes cita')).toBeInTheDocument()
    // Arranca en el primer día con horarios libres; el 12 también está habilitado y el 10 no.
    expect(screen.getByRole('button', { name: /^lunes, 9 de noviembre: 2 horarios libres/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: /^martes, 10 de noviembre/ })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: /^Reservar 9 de nov de 2026, 08:00–08:30/ }))

    const modal = screen.getByRole('dialog', { name: 'Reservar cita' })
    expect(within(modal).getByText('Andrés Ordóñez')).toBeInTheDocument()
    await userEvent.click(within(modal).getByRole('button', { name: 'Reservar' }))
    expect(agendaService.reservarFranja).toHaveBeenCalledWith(1)
    expect(await screen.findByText(/Cita reservada/)).toBeInTheDocument()
  })

  it('con una cita activa el modal avisa que la anterior se libera y cancelar no reserva', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(CITA)
    renderizar()

    expect(await screen.findByText('Tu cita')).toBeInTheDocument()
    await userEvent.click(await screen.findByRole('button', { name: /^jueves, 12 de noviembre/ }))
    await userEvent.click(screen.getByRole('button', { name: /^Reservar 12 de nov de 2026/ }))

    const modal = screen.getByRole('dialog', { name: 'Cambiar mi cita' })
    expect(modal).toHaveTextContent('Tu cita anterior')
    expect(modal).toHaveTextContent('se liberará')
    await userEvent.click(within(modal).getByRole('button', { name: 'Cancelar' }))
    expect(agendaService.reservarFranja).not.toHaveBeenCalled()
  })

  it('si otra persona tomó la franja muestra el 409 y recarga la lista', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(null)
    agendaService.reservarFranja.mockRejectedValue(new ApiError(409, 'La franja ya fue reservada por otro docente'))
    renderizar()

    await userEvent.click(await screen.findByRole('button', { name: /^Reservar 9 de nov de 2026, 08:00–08:30/ }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reservar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La franja ya fue reservada por otro docente')
    expect(agendaService.listarFranjasDisponibles).toHaveBeenCalledTimes(2)
  })

  it('confirma la asistencia y declinar pide confirmación en un modal', async () => {
    agendaService.obtenerMiCita.mockResolvedValue(CITA)
    procesoService.responderCita.mockResolvedValue({ ...CITA, confirmacion: 'CONFIRMADA' })
    renderizar()

    expect(await screen.findByText('Falta confirmar tu asistencia')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar asistencia' }))
    expect(procesoService.responderCita).toHaveBeenCalledWith(true)
    expect(await screen.findByText(/Confirmaste tu asistencia/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'No podré asistir' }))
    const confirmacion = await screen.findByRole('dialog', { name: '¿Declinar la cita?' })
    expect(confirmacion).toHaveTextContent('franja queda para otro docente')
    await userEvent.click(within(confirmacion).getByRole('button', { name: 'Cancelar' }))
    expect(procesoService.responderCita).toHaveBeenCalledTimes(1)
  })
})
