import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { api } from '../../../services/api.js'
import * as usuariosService from '../services/usuariosService.js'
import Usuarios from './Usuarios.jsx'

vi.mock('../services/usuariosService.js')
vi.spyOn(api, 'get')

beforeEach(() => {
  vi.clearAllMocks()
  usuariosService.listarUsuarios.mockResolvedValue([
    { idUsuario: 9, nombreCompleto: 'Ana Ruiz', correo: 'ana@u.co', rol: 'DOCENTE', esActivo: true, nombrePrograma: 'Software' },
  ])
  api.get.mockResolvedValue({ contenido: [], totalElementos: 0, totalPaginas: 0 })
})

describe('Usuarios', () => {
  it('la actividad (auditoría) es una pestaña de Usuarios', async () => {
    renderizarConSesion(<Usuarios />, { rol: 'VICERRECTOR', ruta: '/usuarios' })
    expect(await screen.findByText('Ana Ruiz')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Usuarios' })).toHaveAttribute('aria-selected', 'true')

    await userEvent.click(screen.getByRole('tab', { name: 'Actividad' }))
    expect(screen.getByRole('tab', { name: 'Actividad' })).toHaveAttribute('aria-selected', 'true')
    expect(await screen.findByText('Sin registros')).toBeInTheDocument()
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('/auditoria?'))
    expect(screen.queryByText('Ana Ruiz')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nuevo usuario' })).not.toBeInTheDocument()
  })
})
