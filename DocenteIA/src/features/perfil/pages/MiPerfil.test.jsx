import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SesionContext } from '../../auth/sesionContext.js'
import * as authService from '../../auth/services/authService.js'
import CambiarContrasena from '../../auth/pages/CambiarContrasena.jsx'
import { AvisosProvider } from '../../../shared/components/ui/Avisos/AvisosProvider.jsx'
import MiPerfil from './MiPerfil.jsx'

vi.mock('../../auth/services/authService.js')

const USUARIO = {
  idUsuario: 4,
  nombreCompleto: 'Julián Camilo Hoyos',
  correo: 'julian.hoyos@uniautonoma.edu.co',
  rol: 'DOCENTE',
  idFacultad: 1,
  idPrograma: 2,
  debeCambiarContrasena: false,
}

function renderizar(ruta = '/perfil', reemplazarSesion = vi.fn()) {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <SesionContext.Provider value={{ usuario: USUARIO, validando: false, reemplazarSesion }}>
        <AvisosProvider>
          <Routes>
            <Route path="/perfil" element={<MiPerfil />} />
            <Route path="/cambiar-contrasena" element={<CambiarContrasena />} />
          </Routes>
        </AvisosProvider>
      </SesionContext.Provider>
    </MemoryRouter>,
  )
  return reemplazarSesion
}

describe('MiPerfil', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    authService.obtenerPerfil.mockResolvedValue({
      ...USUARIO,
      codigo: 'D-104',
      nombreFacultad: 'Facultad de Ingeniería',
      nombrePrograma: 'Ingeniería de Software',
      fechaCreacion: '2026-08-01T10:00:00',
    })
  })

  it('muestra los datos propios con su programa, sin hablar de facultad', async () => {
    renderizar()
    expect(screen.getByRole('heading', { name: 'Julián Camilo Hoyos' })).toBeInTheDocument()
    expect(await screen.findByText('Ingeniería de Software')).toBeInTheDocument()
    expect(screen.queryByText('Facultad de Ingeniería')).not.toBeInTheDocument()
    expect(screen.getByText('julian.hoyos@uniautonoma.edu.co')).toBeInTheDocument()
  })

  it('cambia la contraseña en un modal sobre el perfil', async () => {
    const sesionNueva = { token: 't', usuario: USUARIO }
    authService.cambiarContrasena.mockResolvedValue(sesionNueva)
    const reemplazarSesion = renderizar()

    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    const modal = await screen.findByRole('dialog', { name: 'Cambiar contraseña' })
    await userEvent.type(within(modal).getByLabelText('Contraseña actual'), 'Docentia2026*')
    await userEvent.type(within(modal).getByLabelText('Nueva contraseña'), 'OtraClave2026*')
    await userEvent.type(within(modal).getByLabelText('Repite la nueva contraseña'), 'OtraClave2026*')
    expect(within(modal).getByText('¡Todo listo para guardar!')).toBeInTheDocument()
    await userEvent.click(within(modal).getByRole('button', { name: 'Guardar contraseña' }))

    expect(authService.cambiarContrasena).toHaveBeenCalledWith('Docentia2026*', 'OtraClave2026*')
    expect(reemplazarSesion).toHaveBeenCalledWith(sesionNueva)
    expect(screen.queryByRole('dialog', { name: 'Cambiar contraseña' })).not.toBeInTheDocument()
  })

  it('la ruta de cambio voluntario abre el modal dentro de Mi perfil', async () => {
    renderizar('/cambiar-contrasena')
    expect(await screen.findByRole('dialog', { name: 'Cambiar contraseña' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Mi perfil', level: 1 })).toBeInTheDocument()
  })
})
