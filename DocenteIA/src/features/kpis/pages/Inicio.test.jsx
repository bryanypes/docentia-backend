import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import * as docentesService from '../../docentes/services/docentesService.js'
import * as facultadesService from '../../facultades/services/facultadesService.js'
import * as periodosService from '../../periodos/services/periodosService.js'
import * as configuracionService from '../services/configuracionService.js'
import * as kpisService from '../services/kpisService.js'
import Inicio from './Inicio.jsx'

vi.mock('../../docentes/services/docentesService.js')
vi.mock('../../facultades/services/facultadesService.js')
vi.mock('../../periodos/services/periodosService.js')
vi.mock('../services/configuracionService.js')
vi.mock('../services/kpisService.js')

describe('Inicio', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // Los paneles hijos también consultan estos servicios: por defecto responden vacío.
    for (const modulo of [docentesService, facultadesService, periodosService, configuracionService, kpisService]) {
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

    expect(await screen.findByText('Seguimiento del periodo activo (2026-2)')).toBeInTheDocument()
    expect(screen.getAllByText('Cargando el seguimiento').length).toBeGreaterThan(0)
  })
})
