import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { contarNoLeidas, listarNotificaciones, marcarLeida } from '../services/notificacionesService.js'
import { Campana } from './Campana.jsx'

vi.mock('../services/notificacionesService.js')

const AVISO = { id: 1, titulo: 'Acta pendiente', mensaje: 'Ingresa', enlace: '/mi-acta', leida: false, creadaEn: '2026-10-31T10:00:00' }

beforeEach(() => {
  vi.clearAllMocks()
  contarNoLeidas.mockResolvedValue({ noLeidas: 3 })
})

describe('Campana', () => {
  it('muestra el conteo con texto accesible', async () => {
    renderizarConSesion(<Campana />)
    expect(await screen.findByRole('button', { name: 'Notificaciones: 3 sin leer' })).toBeInTheDocument()
    expect(screen.getByText('3 notificaciones sin leer')).toBeInTheDocument()
  })

  it('lista las últimas y marca como leída al abrir', async () => {
    listarNotificaciones.mockResolvedValue({ contenido: [AVISO] })
    marcarLeida.mockResolvedValue(AVISO)
    renderizarConSesion(<Campana />)
    await userEvent.click(await screen.findByRole('button', { name: /Notificaciones/ }))
    expect(listarNotificaciones).toHaveBeenCalledWith({ tamano: 8 })
    await userEvent.click(await screen.findByRole('button', { name: /Acta pendiente/ }))
    expect(marcarLeida).toHaveBeenCalledWith(1)
  })

  it('muestra vacío y error', async () => {
    listarNotificaciones.mockResolvedValueOnce({ contenido: [] }).mockRejectedValueOnce(new Error('x'))
    renderizarConSesion(<Campana />)
    const boton = await screen.findByRole('button', { name: /Notificaciones/ })
    await userEvent.click(boton)
    expect(await screen.findByText('No tienes notificaciones.')).toBeInTheDocument()
    await userEvent.click(boton)
    await userEvent.click(boton)
    expect(await screen.findByText('No fue posible cargarlas.')).toBeInTheDocument()
  })

  it('no consulta con la pestaña oculta', async () => {
    vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
    renderizarConSesion(<Campana />)
    expect(contarNoLeidas).not.toHaveBeenCalled()
    vi.restoreAllMocks()
  })
})
