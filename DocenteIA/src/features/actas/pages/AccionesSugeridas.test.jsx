import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarAccionesSugeridas } from '../services/actasService.js'
import AccionesSugeridas from './AccionesSugeridas.jsx'

vi.mock('../services/actasService.js')
vi.mock('../../periodos/services/periodosService.js')

beforeEach(() => {
  vi.clearAllMocks()
  listarPeriodos.mockResolvedValue([])
})

describe('AccionesSugeridas', () => {
  it('lista docente, categoría y acción', async () => {
    listarAccionesSugeridas.mockResolvedValue([
      {
        idDocente: 1,
        nombreDocente: 'Ramiro Paz',
        programa: 'Electrónica',
        estadoPromedio: 'COMPLETO',
        categoria: { codigo: 'MUY_BAJO', etiqueta: 'Muy bajo' },
        accion: 'Alerta informativa para Vicerrectoría',
      },
    ])
    renderizarConSesion(<AccionesSugeridas />)
    expect(await screen.findByText('Ramiro Paz')).toBeInTheDocument()
    expect(screen.getByText('Muy bajo')).toBeInTheDocument()
    expect(screen.getByText('Alerta informativa para Vicerrectoría')).toBeInTheDocument()
  })

  it('explica el 409 sin periodo activo', async () => {
    listarAccionesSugeridas.mockRejectedValue(new ApiError(409, 'No hay un periodo académico activo'))
    renderizarConSesion(<AccionesSugeridas />)
    expect(await screen.findByText('No hay un periodo activo')).toBeInTheDocument()
  })
})
