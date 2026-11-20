import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../services/api.js'
import { verificarActa } from '../services/actasService.js'
import VerificarActa from './VerificarActa.jsx'

vi.mock('../services/actasService.js')

function abrir(ruta = '/verificar-acta/ABC') {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route path="/verificar-acta" element={<VerificarActa />} />
        <Route path="/verificar-acta/:codigo" element={<VerificarActa />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('VerificarActa', () => {
  it('confirma un acta firmada sin mostrar la versión', async () => {
    verificarActa.mockResolvedValue({ estado: 'FIRMADA', nombreDocente: 'Nora', periodo: '2026-2', version: 3 })
    abrir()
    expect(await screen.findByText('Acta válida')).toBeInTheDocument()
    expect(screen.getByText('Nora')).toBeInTheDocument()
    expect(screen.queryByText(/versión/i)).not.toBeInTheDocument()
  })

  it('avisa si el código no existe', async () => {
    verificarActa.mockRejectedValue(new ApiError(404, 'No encontrada'))
    abrir()
    expect(await screen.findByText('Código no encontrado')).toBeInTheDocument()
  })

  it('sin código pide escribirlo y lo verifica', async () => {
    verificarActa.mockResolvedValue({ estado: 'FIRMADA', nombreDocente: 'Nora', periodo: '2026-2' })
    abrir('/verificar-acta')
    expect(verificarActa).not.toHaveBeenCalled()
    await userEvent.type(screen.getByLabelText('Código del acta'), 'doc-1a2b')
    await userEvent.click(screen.getByRole('button', { name: 'Verificar' }))
    expect(await screen.findByText('Acta válida')).toBeInTheDocument()
    expect(verificarActa).toHaveBeenCalledWith('DOC-1A2B')
  })
})
