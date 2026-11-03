import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import * as procesoService from '../../proceso/services/procesoService.js'
import * as agendaService from '../services/agendaService.js'
import Agenda from './Agenda.jsx'

vi.mock('../services/agendaService.js')
vi.mock('../../proceso/services/procesoService.js')

// Lunes 9 de noviembre de 2026, mediodía en Colombia.
const AHORA = new Date('2026-11-09T17:00:00Z')

const FRANJAS = [
  {
    idFranja: 1,
    fecha: '2026-11-09',
    horaInicio: '08:00:00',
    horaFin: '08:30:00',
    estado: 'ABIERTA',
    reservada: true,
    reserva: { idCita: 3, idDocente: 4, nombreDocente: 'Julián Camilo Hoyos', confirmacion: 'CONFIRMADA', asistencia: null },
  },
  {
    idFranja: 2,
    fecha: '2026-11-10',
    horaInicio: '08:30:00',
    horaFin: '09:00:00',
    estado: 'ABIERTA',
    reservada: false,
    reserva: null,
  },
]

describe('Agenda', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(AHORA)
    agendaService.listarFranjas.mockResolvedValue(FRANJAS)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('muestra la semana con quién reservó y si confirmó', async () => {
    renderizarConSesion(<Agenda />)

    const reservada = await screen.findByRole('button', { name: /08:00–08:30: Julián Camilo Hoyos, Confirmó/ })
    expect(reservada).toHaveTextContent('Julián Camilo Hoyos')
    expect(screen.getByRole('button', { name: /martes, 10 de noviembre, 08:30–09:00: Libre/ })).toBeInTheDocument()
    const estados = screen.getByRole('list', { name: 'Estado de las franjas visibles' })
    expect(estados).toHaveTextContent('1 libres')
    expect(estados).toHaveTextContent('1 confirmadas')
  })

  it('arrastrar sobre horas vacías abre las franjas del bloque', async () => {
    agendaService.abrirFranjasEnLote.mockResolvedValue({
      accion: 'ABRIR',
      aplicadas: [
        { indice: 0, idFranja: 8 },
        { indice: 1, idFranja: 9 },
      ],
      omitidas: [],
    })
    renderizarConSesion(<Agenda />)

    const inicio = await screen.findByRole('button', { name: 'miércoles, 11 de noviembre, 10:00–10:30: vacía' })
    const fin = screen.getByRole('button', { name: 'miércoles, 11 de noviembre, 10:30–11:00: vacía' })
    // Con teclado: Enter marca el inicio y Mayús+Enter el final.
    fireEvent.click(inicio)
    fireEvent.click(fin, { shiftKey: true })
    await userEvent.click(screen.getByRole('button', { name: 'Abrir 2 franjas' }))

    expect(agendaService.abrirFranjasEnLote).toHaveBeenCalledWith([
      { fecha: '2026-11-11', horaInicio: '10:00', horaFin: '10:30' },
      { fecha: '2026-11-11', horaInicio: '10:30', horaFin: '11:00' },
    ])
    expect(await screen.findByText('Franjas abiertas: 2. Omitidas: 0.')).toBeInTheDocument()
  })

  it('los días pasados no se pueden abrir', async () => {
    renderizarConSesion(<Agenda />)
    await userEvent.click(await screen.findByRole('button', { name: 'Semana anterior' }))
    expect(screen.getByRole('button', { name: 'lunes, 2 de noviembre, 10:00–10:30: vacía' })).toBeDisabled()
  })

  it('el día de la cita la coordinación registra la asistencia desde el calendario', async () => {
    procesoService.registrarAsistencia.mockResolvedValue({ idCita: 3, asistencia: 'ASISTIO' })
    renderizarConSesion(<Agenda />)

    await userEvent.click(await screen.findByRole('button', { name: /Julián Camilo Hoyos, Confirmó/ }))
    expect(screen.getByRole('link', { name: 'Ver expediente y proceso' })).toHaveAttribute('href', '/docentes/4')
    await userEvent.click(screen.getByRole('button', { name: 'Asistió · habilitar resultado' }))

    expect(procesoService.registrarAsistencia).toHaveBeenCalledWith(3, true)
    expect(await screen.findByText('Asistencia registrada.')).toBeInTheDocument()
  })

  it('la coordinación mueve una cita a otra franja libre o la cancela', async () => {
    agendaService.reprogramarCita.mockResolvedValue({ idCita: 5, idFranja: 2 })
    agendaService.cancelarCita.mockResolvedValue({ idCita: 3, estado: 'LIBERADA' })
    renderizarConSesion(<Agenda />)

    await userEvent.click(await screen.findByRole('button', { name: /Julián Camilo Hoyos, Confirmó/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar horario' }))
    await userEvent.selectOptions(screen.getByLabelText('Nuevo horario'), '2')
    await userEvent.click(screen.getByRole('button', { name: 'Mover la cita' }))
    expect(agendaService.reprogramarCita).toHaveBeenCalledWith(3, 2)
    expect(await screen.findByText('Cita movida. Se avisó al docente.')).toBeInTheDocument()

    await userEvent.click(await screen.findByRole('button', { name: /Julián Camilo Hoyos, Confirmó/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar cita' }))
    const confirmacion = await screen.findByRole('dialog', { name: '¿Cancelar la cita?' })
    await userEvent.click(within(confirmacion).getByRole('button', { name: 'Cancelar cita' }))
    expect(agendaService.cancelarCita).toHaveBeenCalledWith(3)
  })

  it('eliminar una franja muestra el error del backend', async () => {
    agendaService.eliminarFranja.mockRejectedValue(new ApiError(409, 'La franja ya tuvo citas'))
    renderizarConSesion(<Agenda />)

    await userEvent.click(await screen.findByRole('button', { name: /08:30–09:00: Libre/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
    // Se confirma en un modal propio, no en el diálogo del navegador.
    const confirmacion = await screen.findByRole('dialog', { name: '¿Eliminar la franja?' })
    await userEvent.click(within(confirmacion).getByRole('button', { name: 'Eliminar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La franja ya tuvo citas')
  })
})
