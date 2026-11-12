import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { SesionContext } from '../sesionContext.js'
import Login from './Login.jsx'

function renderLogin() {
  const sesion = {
    usuario: null,
    validando: false,
    motivoCierre: null,
    idSesionVencida: null,
    entrar: vi.fn(),
    entrarConGoogle: vi.fn(),
  }
  return render(
    <MemoryRouter>
      <SesionContext.Provider value={sesion}>
        <Login />
      </SesionContext.Provider>
    </MemoryRouter>,
  )
}

describe('Login', () => {
  it('muestra una sola opción de recuperación y la contraseña revela su estado', () => {
    renderLogin()

    const recuperacion = screen.getAllByRole('link', { name: '¿Olvidaste tu contraseña?' })
    expect(recuperacion).toHaveLength(1)
    expect(recuperacion[0]).toHaveAttribute('href', '/recuperar-contrasena')

    const contrasena = screen.getByLabelText('Contraseña')
    expect(contrasena).toHaveAttribute('type', 'password')
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(screen.getByRole('button', { name: 'Ocultar contraseña' })).toHaveAttribute('aria-pressed', 'true')
    expect(contrasena).toHaveAttribute('type', 'text')
  })
})
