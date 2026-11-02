import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { listarImportaciones } from '../services/importacionesService.js'
import Importaciones from './Importaciones.jsx'

vi.mock('../services/importacionesService.js')
vi.mock('react-router-dom', () => ({
  MemoryRouter: ({ children }) => children,
}))

beforeEach(() => {
  vi.clearAllMocks()
  listarImportaciones.mockResolvedValue([])
})

describe('Importaciones', () => {
  it('el coordinador no ve el selector de archivo y sí el aviso de corte', async () => {
    renderizarConSesion(<Importaciones />, { rol: 'COORDINADOR' })
    expect(await screen.findByText('La carga de CSV ya no está disponible')).toBeInTheDocument()
    expect(screen.queryByLabelText(/archivo|csv/i)).not.toBeInTheDocument()
    expect(document.querySelector('input[type="file"]')).not.toBeInTheDocument()
  })
})
