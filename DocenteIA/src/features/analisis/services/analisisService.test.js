import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../../../services/api.js'
import { obtenerModelosAnalisis, solicitarAnalisis } from './analisisService.js'

vi.mock('../../../services/api.js', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('analisisService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('envía el modelo elegido junto con el motivo opcional', async () => {
    api.post.mockResolvedValue({ idSolicitud: 8 })

    await solicitarAnalisis('4', '2', 'Revisar el nuevo periodo', 'LLM')

    expect(api.post).toHaveBeenCalledWith('/analisis-ia/solicitudes', {
      docenteId: 4,
      periodoId: 2,
      motivo: 'Revisar el nuevo periodo',
      modelo: 'LLM',
    })
  })

  it('consulta las opciones expuestas por el backend', async () => {
    api.get.mockResolvedValue({ porDefecto: 'LOCAL', opciones: [] })

    await obtenerModelosAnalisis()

    expect(api.get).toHaveBeenCalledWith('/analisis-comentarios/modelos')
  })
})
