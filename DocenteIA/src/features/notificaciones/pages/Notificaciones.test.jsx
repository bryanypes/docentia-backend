import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import {
  guardarPreferencias,
  listarNotificaciones,
  marcarTodasLeidas,
  obtenerPreferencias,
} from '../services/notificacionesService.js'
import Notificaciones from './Notificaciones.jsx'

vi.mock('../services/notificacionesService.js')

const PAGINA = {
  contenido: [{ id: 1, titulo: 'Cita agendada', mensaje: 'Tu cita', leida: true, creadaEn: '2026-10-31T10:00:00' }],
  totalPaginas: 2,
}

beforeEach(() => {
  vi.clearAllMocks()
  obtenerPreferencias.mockResolvedValue({ correoHabilitado: true })
  listarNotificaciones.mockResolvedValue(PAGINA)
})

describe('Notificaciones', () => {
  it('muestra el estado con texto y pagina', async () => {
    renderizarConSesion(<Notificaciones />)
    expect(await screen.findByText('Leída')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(listarNotificaciones).toHaveBeenLastCalledWith({ soloNoLeidas: false, pagina: 1 })
  })

  it('filtra y marca todas', async () => {
    marcarTodasLeidas.mockResolvedValue(null)
    renderizarConSesion(<Notificaciones />)
    await userEvent.click(await screen.findByLabelText('Solo no leídas'))
    expect(listarNotificaciones).toHaveBeenLastCalledWith({ soloNoLeidas: true, pagina: 0 })
    await userEvent.click(screen.getByRole('button', { name: 'Marcar todas como leídas' }))
    expect(marcarTodasLeidas).toHaveBeenCalled()
  })

  it('cambia el interruptor de correo', async () => {
    guardarPreferencias.mockResolvedValue({ correoHabilitado: false })
    renderizarConSesion(<Notificaciones />)
    const interruptor = await screen.findByLabelText('Recibir avisos por correo')
    await vi.waitFor(() => expect(interruptor).toBeEnabled())
    await userEvent.click(interruptor)
    expect(guardarPreferencias).toHaveBeenCalledWith(false)
  })

  it('muestra vacío y error', async () => {
    listarNotificaciones.mockResolvedValueOnce({ contenido: [], totalPaginas: 0 })
    const { unmount } = renderizarConSesion(<Notificaciones />)
    expect(await screen.findByText('No hay notificaciones')).toBeInTheDocument()
    unmount()
    listarNotificaciones.mockRejectedValueOnce(new ApiError(500, 'Falló'))
    renderizarConSesion(<Notificaciones />)
    expect(await screen.findByText('No fue posible cargar las notificaciones')).toBeInTheDocument()
  })
})
