import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { obtenerPanelProgramas } from '../services/kpisService.js'
import PanelProgramas from './PanelProgramas.jsx'

vi.mock('../services/kpisService.js')
vi.mock('../../periodos/services/periodosService.js')
vi.mock('../../facultades/services/facultadesService.js')
vi.mock('../../formularios/services/formulariosService.js')
vi.mock('../../heteroevaluacion/services/matriculaService.js')

const FILA = {
  idPrograma: 1,
  programa: 'Software',
  facultad: 'Ingeniería',
  docentes: 4,
  evaluados: 2,
  cobertura: 50,
  promedio: 3.8,
  distribucion: [{ codigo: 'BAJO', etiqueta: 'Bajo', cantidad: 2, porcentaje: 100 }],
}

beforeEach(() => {
  vi.clearAllMocks()
  listarPeriodos.mockResolvedValue([])
  listarFacultades.mockResolvedValue([])
})

describe('PanelProgramas', () => {
  it('muestra cobertura y distribución sin enlaces individuales', async () => {
    obtenerPanelProgramas.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      programas: [FILA],
      total: { ...FILA, idPrograma: null, programa: 'Total' },
    })
    const { container } = renderizarConSesion(<PanelProgramas />)
    expect(await screen.findByText('Software')).toBeInTheDocument()
    expect(within(container).getAllByText('50 %').length).toBeGreaterThan(0)
    expect(container.querySelector('.pagina a')).toBeNull()
  })

  it('muestra el error del servidor', async () => {
    obtenerPanelProgramas.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<PanelProgramas />)
    expect(await screen.findByText('No fue posible cargar el panel')).toBeInTheDocument()
  })
})
