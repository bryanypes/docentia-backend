import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authService from '../services/authService.js'
import RecuperarContrasena from './RecuperarContrasena.jsx'

vi.mock('../services/authService.js')

function abrir(ruta) {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <RecuperarContrasena />
    </MemoryRouter>,
  )
}

describe('RecuperarContrasena', () => {
  beforeEach(() => vi.resetAllMocks())

  it('con correo configurado pide el correo y muestra el mensaje genérico', async () => {
    authService.obtenerDisponibilidadRecuperacion.mockResolvedValue({ porCorreo: true })
    authService.solicitarRecuperacion.mockResolvedValue({ mensaje: 'Si el correo está registrado, te enviamos un enlace.' })
    abrir('/recuperar-contrasena')
    await userEvent.type(screen.getByLabelText('Correo institucional'), 'alguien@uniautonoma.edu.co')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar enlace' }))
    expect(authService.solicitarRecuperacion).toHaveBeenCalledWith('alguien@uniautonoma.edu.co')
    expect(await screen.findByText('Si el correo está registrado, te enviamos un enlace.')).toBeInTheDocument()
  })

  it('sin correo configurado no promete un enlace y dice a quién pedir la contraseña', async () => {
    authService.obtenerDisponibilidadRecuperacion.mockResolvedValue({ porCorreo: false })
    abrir('/recuperar-contrasena')
    expect(await screen.findByText('Pide una nueva contraseña a la Vicerrectoría')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar enlace' })).not.toBeInTheDocument()
    expect(authService.solicitarRecuperacion).not.toHaveBeenCalled()
  })

  it('con token fija la contraseña nueva si coinciden', async () => {
    authService.restablecerContrasena.mockResolvedValue(null)
    abrir('/restablecer-contrasena?token=abc123')
    const boton = screen.getByRole('button', { name: 'Guardar contraseña' })
    await userEvent.type(screen.getByLabelText('Contraseña nueva'), 'Nueva-Clave-2026')
    await userEvent.type(screen.getByLabelText('Repite la contraseña'), 'Otra')
    expect(boton).toBeDisabled()
    await userEvent.clear(screen.getByLabelText('Repite la contraseña'))
    await userEvent.type(screen.getByLabelText('Repite la contraseña'), 'Nueva-Clave-2026')
    await userEvent.click(boton)
    expect(authService.restablecerContrasena).toHaveBeenCalledWith('abc123', 'Nueva-Clave-2026')
    expect(await screen.findByText('Contraseña actualizada')).toBeInTheDocument()
  })

  it('en todos los casos ofrece ir al inicio, ir al login y solicitar ayuda', async () => {
    for (const porCorreo of [true, false]) {
      authService.obtenerDisponibilidadRecuperacion.mockResolvedValue({ porCorreo })
      const { unmount } = render(
        <MemoryRouter initialEntries={['/recuperar-contrasena']}>
          <RecuperarContrasena />
        </MemoryRouter>,
      )
      expect(await screen.findByRole('link', { name: /Ir al inicio/ })).toHaveAttribute('href', '/')
      expect(screen.getByRole('link', { name: /Ir al login/ })).toHaveAttribute('href', '/login')
      expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveAttribute('target', '_blank')
      unmount()
    }
  })
})
