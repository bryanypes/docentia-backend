import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../services/api.js'
import { verificarActa } from '../services/actasService.js'
import VerificarActa from './VerificarActa.jsx'

vi.mock('../services/actasService.js')

function abrir() {
  render(
    <MemoryRouter initialEntries={['/verificar-acta/ABC']}>
      <Routes>
        <Route path="/verificar-acta/:codigo" element={<VerificarActa />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('VerificarActa', () => {
  it('confirma un acta firmada', async () => {
    verificarActa.mockResolvedValue({ estado: 'FIRMADA', nombreDocente: 'Nora', periodo: '2026-2', version: 1 })
    abrir()
    expect(await screen.findByText('Acta válida')).toBeInTheDocument()
  })

  it('avisa si el código no existe', async () => {
    verificarActa.mockRejectedValue(new ApiError(404, 'No encontrada'))
    abrir()
    expect(await screen.findByText('El código no corresponde a ningún acta')).toBeInTheDocument()
  })
})
