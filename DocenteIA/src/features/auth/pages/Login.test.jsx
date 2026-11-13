import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../services/api.js'
import { SesionContext } from '../sesionContext.js'
import Login from './Login.jsx'

function renderLogin(entrar = vi.fn()) {
  const sesion = {
    usuario: null,
    validando: false,
    motivoCierre: null,
    idSesionVencida: null,
    entrar,
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

describe('Login: intentos y bloqueo', () => {
  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
  })

  it('al recargar la página el contador sigue donde iba', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-11-13T15:00:00Z'))
    localStorage.setItem('docentia.bloqueo-ingreso', String(Date.now() + 125_000))
    renderLogin()

    expect(screen.getByRole('timer')).toHaveTextContent('2:05')
    expect(screen.queryByRole('button', { name: /Ingresar/ })).not.toBeInTheDocument()
  })

  it('un bloqueo ya vencido no se muestra y se borra', () => {
    localStorage.setItem('docentia.bloqueo-ingreso', String(Date.now() - 1000))
    renderLogin()

    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(localStorage.getItem('docentia.bloqueo-ingreso')).toBeNull()
  })

  async function ingresar() {
    fireEvent.change(screen.getByLabelText('Correo institucional'), { target: { value: 'ana@uniautonoma.edu.co' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'una-clave-mala' } })
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Ingresar/ }))
    })
  }

  it('dice cuántos intentos lleva sin revelar si falló el correo o la contraseña', async () => {
    const entrar = vi.fn().mockRejectedValue(
      new ApiError(401, 'Correo o contraseña incorrectos', {}, { intentosUsados: 2, intentosMaximos: 5 }),
    )
    renderLogin(entrar)
    await ingresar()

    const aviso = screen.getByRole('alert')
    expect(aviso).toHaveTextContent('Correo o contraseña incorrectos')
    expect(aviso).toHaveTextContent('Llevas 2 de 5 intentos. Te quedan 3.')
  })

  it('al llegar al límite avisa que el próximo fallo bloquea', async () => {
    const entrar = vi.fn().mockRejectedValue(
      new ApiError(401, 'Correo o contraseña incorrectos', {}, { intentosUsados: 5, intentosMaximos: 5 }),
    )
    renderLogin(entrar)
    await ingresar()

    expect(screen.getByRole('alert')).toHaveTextContent('El próximo fallo bloqueará el ingreso unos minutos.')
  })

  it('durante el bloqueo muestra el cronómetro y la ayuda, y al terminar vuelve el formulario', async () => {
    vi.useFakeTimers()
    const entrar = vi.fn().mockRejectedValue(
      new ApiError(429, 'Demasiados intentos. Podrás volver a intentarlo en 2 minutos', {}, { segundosParaReintentar: 65 }),
    )
    renderLogin(entrar)
    await ingresar()

    expect(screen.getByRole('heading', { name: 'Ingreso pausado por seguridad' })).toBeInTheDocument()
    expect(screen.getByRole('timer')).toHaveTextContent('1:05')
    expect(screen.queryByRole('button', { name: /Ingresar/ })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveAttribute('href', expect.stringContaining('tickets'))

    act(() => {
      vi.advanceTimersByTime(6000)
    })
    expect(screen.getByRole('timer')).toHaveTextContent('0:59')

    act(() => {
      vi.advanceTimersByTime(60000)
    })
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Ingresar/ })).toBeEnabled()
  })

  it('al llegar al límite ofrece solicitar ayuda', async () => {
    const entrar = vi.fn().mockRejectedValue(
      new ApiError(401, 'Correo o contraseña incorrectos', {}, { intentosUsados: 5, intentosMaximos: 5 }),
    )
    renderLogin(entrar)
    await ingresar()

    expect(screen.getByText('¿Tienes problemas para ingresar a DOCENTIA?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveClass('boton--primario')
  })
})

describe('Login: correo que no es de la Corporación', () => {
  it('avisa sin enviar el intento al servidor y el aviso se va al corregir el correo', async () => {
    const entrar = vi.fn()
    renderLogin(entrar)
    fireEvent.change(screen.getByLabelText('Correo institucional'), { target: { value: 'ana@gmail.com' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'una-clave' } })
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Ingresar/ }))
    })

    expect(entrar).not.toHaveBeenCalled()
    expect(screen.getByRole('heading', { name: 'Este correo no puede ingresar a DOCENTIA' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('ana@gmail.com no hace parte de la Corporación')
    expect(screen.getByLabelText('Correo institucional')).toHaveAttribute('aria-invalid', 'true')

    fireEvent.change(screen.getByLabelText('Correo institucional'), { target: { value: 'ana@uniautonoma.edu.co' } })
    expect(screen.queryByRole('heading', { name: 'Este correo no puede ingresar a DOCENTIA' })).not.toBeInTheDocument()
  })

  it('siempre deja a mano el enlace de ayuda', () => {
    renderLogin()
    expect(screen.getByText(/¿Problemas para ingresar\?/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Solicitar ayuda/ })).toHaveAttribute('target', '_blank')
  })
})
