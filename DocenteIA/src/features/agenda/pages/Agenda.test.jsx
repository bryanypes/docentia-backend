import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import * as agendaService from '../services/agendaService.js'
import Agenda from './Agenda.jsx'

vi.mock('../services/agendaService.js')

const FRANJAS = [
  {
    idFranja: 1,
    fecha: '2026-11-09',
    horaInicio: '08:00:00',
    horaFin: '08:30:00',
    estado: 'ABIERTA',
    reservada: true,
    reserva: { idCita: 3, idDocente: 4, nombreDocente: 'Julián Camilo Hoyos' },
  },
  {
    idFranja: 2,
    fecha: '2026-11-09',
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
    agendaService.listarFranjas.mockResolvedValue(FRANJAS)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calcula el fin a +30 minutos y abre la franja con él', async () => {
    agendaService.abrirFranja.mockResolvedValue({ idFranja: 9 })
    renderizarConSesion(<Agenda />)

    fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2099-11-11' } })
    fireEvent.change(screen.getByLabelText('Hora de inicio'), { target: { value: '10:00' } })
    expect(screen.getByText('Termina: 10:30')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir franja' }))

    expect(agendaService.abrirFranja).toHaveBeenCalledWith({
      fecha: '2099-11-11',
      horaInicio: '10:00',
      horaFin: '10:30',
    })
  })

  it('el lote muestra las aplicadas y las omitidas con su motivo', async () => {
    agendaService.abrirFranjasEnLote.mockResolvedValue({
      accion: 'ABRIR',
      aplicadas: [{ indice: 0, idFranja: 5 }],
      omitidas: [
        {
          indice: 1,
          idFranja: null,
          fecha: '2099-11-11',
          horaInicio: '08:30:00',
          horaFin: '09:00:00',
          motivo: 'Se cruza con otra franja abierta',
        },
      ],
    })
    renderizarConSesion(<Agenda />)

    fireEvent.change(screen.getByLabelText('Fecha del bloque'), { target: { value: '2099-11-11' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '09:00' } })
    await userEvent.click(screen.getByRole('button', { name: 'Abrir 2 franjas' }))

    expect(await screen.findByText('Franjas abiertas: 1. Omitidas: 1.')).toBeInTheDocument()
    expect(screen.getByText(/Se cruza con otra franja abierta/)).toBeInTheDocument()
  })

  it('eliminar una franja reservada muestra el 409 del backend', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    agendaService.eliminarFranja.mockRejectedValue(
      new ApiError(409, 'No se puede eliminar una franja con una cita reservada'),
    )
    renderizarConSesion(<Agenda />)

    const fila = (await screen.findByText('Julián Camilo Hoyos')).closest('tr')
    expect(within(fila).queryByRole('checkbox')).not.toBeInTheDocument()
    await userEvent.click(within(fila).getByRole('button', { name: /Eliminar/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('con una cita reservada')
  })

  it('cierra en lote las seleccionadas', async () => {
    agendaService.cerrarFranjasEnLote.mockResolvedValue({
      accion: 'CERRAR',
      aplicadas: [{ indice: 0, idFranja: 2 }],
      omitidas: [],
    })
    renderizarConSesion(<Agenda />)

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Seleccionar 08:30–09:00' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar seleccionadas (1)' }))

    expect(agendaService.cerrarFranjasEnLote).toHaveBeenCalledWith([2])
    expect(await screen.findByText('Franjas cerradas: 1. Omitidas: 0.')).toBeInTheDocument()
  })
})
