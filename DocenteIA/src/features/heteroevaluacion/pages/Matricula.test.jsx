import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { importarMatricula, obtenerCobertura } from '../services/matriculaService.js'
import Matricula from './Matricula.jsx'

vi.mock('../services/matriculaService.js')

const COBERTURA = {
  periodo: { nombre: '2026-2' },
  total: { nombre: 'Total', matriculados: 6, respondieron: 4, porcentaje: 66.7 },
  programas: [{ nombre: 'Software', matriculados: 6, respondieron: 4, porcentaje: 66.7 }],
  docentes: { contenido: [{ idDocente: 1, nombre: 'Laura', matriculados: 6, respondieron: 4, porcentaje: 66.7 }], totalPaginas: 1 },
}

beforeEach(() => {
  vi.clearAllMocks()
  obtenerCobertura.mockResolvedValue(COBERTURA)
})

describe('Matricula', () => {
  it('muestra la cobertura por docente y programa sin estudiantes', async () => {
    renderizarConSesion(<Matricula />)
    expect(await screen.findByText('Laura')).toBeInTheDocument()
    expect(screen.getByText('4 de 6 (66,7 %)')).toBeInTheDocument()
  })

  it('reporta los errores por fila de un 422', async () => {
    importarMatricula.mockRejectedValue(
      new ApiError(422, 'x', {}, { valido: false, errores: [{ fila: 3, motivo: 'El docente no existe o no está activo' }] }),
    )
    renderizarConSesion(<Matricula />)
    await userEvent.upload(await screen.findByLabelText('Archivo CSV'), new File(['a'], 'm.csv', { type: 'text/csv' }))
    await userEvent.click(screen.getByRole('button', { name: 'Importar' }))
    expect(await screen.findByText('Fila 3: El docente no existe o no está activo')).toBeInTheDocument()
  })

  it('confirma una importación válida', async () => {
    importarMatricula.mockResolvedValue({ valido: true, filasProcesadas: 2, estudiantesCreados: 1, matriculasNuevas: 2, matriculasRetiradas: 0 })
    renderizarConSesion(<Matricula />)
    await userEvent.upload(await screen.findByLabelText('Archivo CSV'), new File(['a'], 'm.csv', { type: 'text/csv' }))
    await userEvent.click(screen.getByRole('button', { name: 'Importar' }))
    expect(await screen.findByText('Matrícula importada')).toBeInTheDocument()
  })
})
