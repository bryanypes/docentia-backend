import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
        nombreDocente: 'Ramiro Muestra',
        programa: 'Electrónica',
        estadoPromedio: 'COMPLETO',
        categoria: { codigo: 'MUY_BAJO', etiqueta: 'Muy bajo' },
        accion: 'Alerta informativa para Vicerrectoría',
      },
    ])
    renderizarConSesion(<AccionesSugeridas />)
    expect(await screen.findByText('Ramiro Muestra')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Muy bajo')).toBeInTheDocument()
    expect(screen.getByText('Alerta informativa para Vicerrectoría')).toBeInTheDocument()
  })

  it('avisa de los promedios por debajo de 4,0 y filtra con un clic', async () => {
    listarAccionesSugeridas.mockResolvedValue([
      { idDocente: 1, nombreDocente: 'Ana Ruiz', programa: 'Derecho', promedio: 4.6, estadoPromedio: 'COMPLETO', categoria: { codigo: 'ALTO', etiqueta: 'Alto' }, accion: 'Felicitación' },
      { idDocente: 2, nombreDocente: 'Luis Muestra', programa: 'Software', promedio: 3.9, estadoPromedio: 'COMPLETO', categoria: { codigo: 'ACEPTABLE', etiqueta: 'Aceptable' }, accion: 'Seguimiento' },
    ])
    renderizarConSesion(<AccionesSugeridas />)
    const aviso = await screen.findByRole('button', { name: /1 docente tiene un promedio por debajo de 4,0/ })
    await userEvent.click(aviso)
    expect(screen.getByText('Luis Muestra')).toBeInTheDocument()
    expect(screen.queryByText('Ana Ruiz')).not.toBeInTheDocument()
    expect(screen.getByText('1 de 2 docentes')).toBeInTheDocument()
  })

  it('explica el 409 sin periodo activo', async () => {
    listarAccionesSugeridas.mockRejectedValue(new ApiError(409, 'No hay un periodo académico activo'))
    renderizarConSesion(<AccionesSugeridas />)
    expect(await screen.findByText('No hay un periodo activo')).toBeInTheDocument()
  })
})
