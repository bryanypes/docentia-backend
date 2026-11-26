import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../pruebas/renderizarConSesion.jsx'
import * as servicio from './correccionesService.js'
import { SolicitarCorreccion } from './SolicitarCorreccion.jsx'
import SolicitudesCorreccion from './SolicitudesCorreccion.jsx'

vi.mock('./correccionesService.js')
// El panel para reabrir directo pide la lista de usuarios.
vi.mock('../usuarios/services/usuariosService.js', () => ({ listarUsuarios: () => Promise.resolve([]) }))

const BASE = {
  idSolicitud: 7,
  tipo: 'AUTO',
  periodo: '2026-2',
  idDocente: 4,
  nombreDocente: 'Julián Prueba',
  nombreFacultad: 'Ingeniería',
  nombreSolicitante: 'Julián Prueba',
  motivo: 'Marqué mal varios ítems del segundo criterio',
  rolAprobador: 'COORDINADOR',
  fechaSolicitud: '2026-11-18T10:00:00',
}

describe('Solicitudes de corrección', () => {
  beforeEach(() => vi.resetAllMocks())

  it('el docente pide corregir su autoevaluación con un motivo', async () => {
    servicio.listarMisSolicitudes.mockResolvedValueOnce([]).mockResolvedValue([
      { ...BASE, estado: 'PENDIENTE_APROBACION' },
    ])
    servicio.solicitarCorreccion.mockResolvedValue({ ...BASE, estado: 'PENDIENTE_APROBACION' })
    renderizarConSesion(<SolicitarCorreccion tipo="AUTO" />, { rol: 'DOCENTE' })

    await userEvent.click(await screen.findByRole('button', { name: 'Necesito corregir esta evaluación' }))
    const enviar = screen.getByRole('button', { name: 'Enviar solicitud' })
    expect(enviar).toBeDisabled()
    await userEvent.type(screen.getByLabelText(/Qué necesitas corregir/), 'Marqué mal varios ítems')
    await userEvent.click(enviar)

    expect(servicio.solicitarCorreccion).toHaveBeenCalledWith({
      tipo: 'AUTO',
      idDocente: null,
      motivo: 'Marqué mal varios ítems',
    })
    expect(await screen.findByText(/La está verificando la coordinación/)).toBeInTheDocument()
  })

  it('la coordinación aprueba o rechaza; rechazar exige explicar', async () => {
    servicio.listarPorVerificar.mockResolvedValue([{ ...BASE, estado: 'PENDIENTE_APROBACION' }])
    servicio.listarMisSolicitudes.mockResolvedValue([])
    servicio.verificarSolicitud.mockResolvedValue({ ...BASE, estado: 'APROBADA' })
    renderizarConSesion(<SolicitudesCorreccion />, { rol: 'COORDINADOR' })

    const tarjeta = (await screen.findByText(/Autoevaluación · Julián Prueba/)).closest('li')
    expect(within(tarjeta).getByRole('button', { name: 'Rechazar' })).toBeDisabled()
    await userEvent.click(within(tarjeta).getByRole('button', { name: 'Aprobar corrección' }))

    expect(servicio.verificarSolicitud).toHaveBeenCalledWith(7, true, '')
    expect(await screen.findByText(/La administración la recibió/)).toBeInTheDocument()
  })

  it('la administración reabre el formulario de una solicitud aprobada', async () => {
    servicio.listarTodasLasSolicitudes.mockResolvedValue([
      { ...BASE, estado: 'APROBADA', nombreAprobador: 'Marta Ruiz', fechaAprobacion: '2026-11-18T11:00:00' },
      { ...BASE, idSolicitud: 8, estado: 'PENDIENTE_APROBACION' },
    ])
    servicio.atenderSolicitud.mockResolvedValue({ ...BASE, estado: 'ATENDIDA' })
    renderizarConSesion(<SolicitudesCorreccion />, { rol: 'ADMINISTRADOR' })

    expect(await screen.findByRole('button', { name: /Por atender/ })).toHaveTextContent('1')
    expect(screen.getByText(/Aprobó Marta Ruiz/)).toBeInTheDocument()
    expect(screen.queryByText('Mis solicitudes')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Reabrir formulario' }))

    expect(servicio.atenderSolicitud).toHaveBeenCalledWith(7, true, '')
    expect(await screen.findByText(/Formulario reabierto. Avisamos al solicitante/)).toBeInTheDocument()
  })
})
