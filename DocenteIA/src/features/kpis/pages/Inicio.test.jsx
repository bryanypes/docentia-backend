import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import * as docentesService from '../../docentes/services/docentesService.js'
import * as facultadesService from '../../facultades/services/facultadesService.js'
import * as periodosService from '../../periodos/services/periodosService.js'
import * as programasService from '../../programas/services/programasService.js'
import * as configuracionService from '../services/configuracionService.js'
import * as kpisService from '../services/kpisService.js'
import Inicio from './Inicio.jsx'

vi.mock('../../docentes/services/docentesService.js')
vi.mock('../../facultades/services/facultadesService.js')
vi.mock('../../periodos/services/periodosService.js')
vi.mock('../../programas/services/programasService.js')
vi.mock('../services/configuracionService.js')
vi.mock('../services/kpisService.js')

describe('Inicio', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // Los paneles hijos también consultan estos servicios: por defecto responden vacío.
    for (const modulo of [docentesService, facultadesService, periodosService, programasService, configuracionService, kpisService]) {
      for (const funcion of Object.values(modulo)) {
        if (vi.isMockFunction(funcion)) funcion.mockResolvedValue(null)
      }
    }
    periodosService.listarPeriodos.mockResolvedValue([])
    facultadesService.listarFacultades.mockResolvedValue([])
    kpisService.obtenerKpis.mockResolvedValue(null)
    docentesService.listarSeguimientoDocentes.mockResolvedValue({
      periodo: { idPeriodo: 1, nombre: '2026-2' },
      docentes: [],
    })
  })

  it('si el seguimiento llega antes que la configuración, espera en lugar de romperse', async () => {
    // La configuración (el umbral) nunca responde.
    configuracionService.obtenerConfiguracionEvaluacion.mockReturnValue(new Promise(() => {}))
    renderizarConSesion(<Inicio />, { rol: 'VICERRECTOR' })

    expect(await screen.findByRole('heading', { name: /Docentes del periodo 2026-2/ })).toBeInTheDocument()
    expect(screen.getAllByText('Cargando el seguimiento').length).toBeGreaterThan(0)
  })

  it('muestra la mención de honor y filtra todo el tablero por carrera', async () => {
    const docente = (id, nombre, valor) => ({
      docente: { idDocente: id, nombreCompleto: nombre, nombrePrograma: 'Software', nombreFacultad: 'Ingeniería', idPrograma: 7 },
      promedio: { estado: 'COMPLETO', valor, fuentesPendientes: [] },
    })
    docentesService.listarSeguimientoDocentes.mockResolvedValue({
      periodo: { idPeriodo: 1, nombre: '2026-2' },
      docentes: [docente(1, 'Ana Ruiz', 4.8), docente(2, 'Luis Paz', 2.4), docente(3, 'Eva Gil', 4.6)],
    })
    configuracionService.obtenerConfiguracionEvaluacion.mockResolvedValue({ umbralCompromisos: 3.8 })
    programasService.listarProgramas.mockResolvedValue([{ idPrograma: 7, nombre: 'Software' }])
    renderizarConSesion(<Inicio />, { rol: 'VICERRECTOR' })

    const honor = (await screen.findByRole('heading', { name: 'Mención de honor' })).closest('section')
    const filas = within(honor).getAllByRole('link')
    expect(filas.map((f) => f.textContent)).toEqual([expect.stringContaining('Ana Ruiz'), expect.stringContaining('Eva Gil')])
    expect(within(honor).queryByText('Luis Paz')).not.toBeInTheDocument()

    await userEvent.selectOptions(await screen.findByLabelText('Carrera'), '7')
    expect(docentesService.listarSeguimientoDocentes).toHaveBeenLastCalledWith(null, { periodoId: null, idPrograma: '7' })
    expect(kpisService.obtenerKpis).toHaveBeenLastCalledWith({ periodoId: null, facultadId: null, programaId: '7' })
    expect(screen.getByRole('heading', { name: 'Software · Toda la universidad' })).toBeInTheDocument()
  })
})
