import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
    // Nunca enlaza un expediente individual; solo las estadísticas del programa.
    expect(container.querySelector('.pagina a[href*="/docentes"]')).toBeNull()
    expect(screen.getByRole('link', { name: /Ver estadísticas del programa/ })).toHaveAttribute(
      'href',
      '/estadisticas?programa=Software',
    )
  })

  it('filtra por categoría tocando la leyenda del total y busca por nombre', async () => {
    const otro = {
      ...FILA,
      idPrograma: 2,
      programa: 'Derecho',
      promedio: 4.7,
      distribucion: [{ codigo: 'MUY_ALTO', etiqueta: 'Muy alto', cantidad: 2, porcentaje: 100 }],
    }
    obtenerPanelProgramas.mockResolvedValue({
      periodo: { nombre: '2026-2' },
      programas: [FILA, otro],
      total: {
        ...FILA,
        idPrograma: null,
        programa: 'Total',
        distribucion: [...FILA.distribucion, ...otro.distribucion],
      },
    })
    renderizarConSesion(<PanelProgramas />)

    expect(await screen.findByRole('heading', { name: 'Derecho' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /^Bajo/ }))
    expect(screen.queryByRole('heading', { name: 'Derecho' })).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('1 de 2 programas con docentes en «Bajo»')

    await userEvent.click(screen.getByRole('button', { name: 'Quitar filtros' }))
    await userEvent.type(screen.getByLabelText('Buscar programa'), 'der')
    expect(screen.getByRole('heading', { name: 'Derecho' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Software' })).not.toBeInTheDocument()
  })

  it('muestra el error del servidor', async () => {
    obtenerPanelProgramas.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<PanelProgramas />)
    expect(await screen.findByText('No fue posible cargar el panel')).toBeInTheDocument()
  })
})
