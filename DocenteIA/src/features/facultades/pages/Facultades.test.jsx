import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import * as programasService from '../../programas/services/programasService.js'
import * as facultadesService from '../services/facultadesService.js'
import Facultades from './Facultades.jsx'

vi.mock('../services/facultadesService.js')
vi.mock('../../programas/services/programasService.js')

beforeEach(() => {
  vi.resetAllMocks()
  facultadesService.listarFacultades.mockResolvedValue([
    { idFacultad: 1, nombre: 'Facultad de Ingeniería' },
    { idFacultad: 2, nombre: 'Facultad de Derecho' },
  ])
  programasService.listarProgramas.mockResolvedValue([
    { idPrograma: 10, nombre: 'Ingeniería de Software', idFacultad: 1, nombreFacultad: 'Facultad de Ingeniería' },
    { idPrograma: 11, nombre: 'Ingeniería Electrónica', idFacultad: 1, nombreFacultad: 'Facultad de Ingeniería' },
    { idPrograma: 20, nombre: 'Derecho', idFacultad: 2, nombreFacultad: 'Facultad de Derecho' },
  ])
})

const tarjeta = (nombre) => screen.getByRole('heading', { name: nombre }).closest('section')

describe('Facultades y programas', () => {
  it('muestra cada facultad con sus programas y busca por programa', async () => {
    renderizarConSesion(<Facultades />, { rol: 'VICERRECTOR' })
    await screen.findByRole('heading', { name: 'Facultad de Ingeniería' })
    expect(within(tarjeta('Facultad de Ingeniería')).getByText('2 programas')).toBeInTheDocument()
    expect(within(tarjeta('Facultad de Derecho')).getByText('Derecho')).toBeInTheDocument()

    await userEvent.type(screen.getByPlaceholderText('Buscar facultad o programa'), 'electronica')
    expect(screen.queryByRole('heading', { name: 'Facultad de Derecho' })).not.toBeInTheDocument()
    expect(within(tarjeta('Facultad de Ingeniería')).queryByText('Ingeniería de Software')).not.toBeInTheDocument()
    expect(within(tarjeta('Facultad de Ingeniería')).getByText('Ingeniería Electrónica')).toBeInTheDocument()
  })

  it('agrega un programa desde la tarjeta con la facultad ya elegida', async () => {
    programasService.crearPrograma.mockResolvedValue({})
    renderizarConSesion(<Facultades />, { rol: 'VICERRECTOR' })
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar programa a Facultad de Derecho' }))
    const modal = await screen.findByRole('dialog', { name: 'Nuevo programa' })
    expect(within(modal).getByLabelText('Facultad')).toHaveValue('2')
    await userEvent.type(within(modal).getByLabelText('Nombre'), 'Ciencia Política')
    await userEvent.click(within(modal).getByRole('button', { name: 'Crear programa' }))
    expect(programasService.crearPrograma).toHaveBeenCalledWith({ nombre: 'Ciencia Política', idFacultad: 2 })
  })
})
