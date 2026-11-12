import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import * as analisisService from '../services/analisisService.js'
import { PanoramaAnalisis } from './PanoramaAnalisis.jsx'

vi.mock('../services/analisisService.js')

const TOTALES = {
  docentes: 3,
  conComentarios: 3,
  comentarios: 30,
  analizados: 1,
  publicados: 0,
  pendientesRevision: 1,
  enCurso: 0,
  conError: 0,
  insuficientes: 0,
  comentariosEnviados: 12,
  comentariosFiltrados: 1,
  fortalezas: 2,
  oportunidades: 1,
  descartados: 0,
  porDecidir: 3,
  citas: 3,
  citasVerificadas: 3,
}

function fila(idDocente, nombreCompleto, nombrePrograma, nombreFacultad, extra = {}) {
  return {
    docente: { idDocente, nombreCompleto, nombrePrograma, nombreFacultad },
    comentarios: 10,
    estado: null,
    publicado: false,
    fortalezas: 0,
    oportunidades: 0,
    ...extra,
  }
}

const PANORAMA = {
  periodo: { idPeriodo: 2, nombre: '2026-2' },
  totales: TOTALES,
  docentes: [
    fila(1, 'Ana Ruiz', 'Software', 'Ingeniería', { estado: 'COMPLETADO', fortalezas: 2, oportunidades: 1, comentarios: 14 }),
    fila(2, 'Bruno Paz', 'Electrónica', 'Ingeniería'),
    fila(3, 'Carla Díaz', 'Derecho', 'Derecho'),
  ],
}

const nombres = () =>
  within(screen.getByRole('heading', { name: 'Docentes' }).closest('section'))
    .getAllByRole('listitem')
    .map((li) => li.querySelector('strong')?.textContent)
    .filter(Boolean)

beforeEach(() => {
  vi.resetAllMocks()
  analisisService.obtenerPanorama.mockResolvedValue(PANORAMA)
})

describe('Panorama del análisis', () => {
  it('filtra por facultad, programa y estado, y ordena', async () => {
    renderizarConSesion(<PanoramaAnalisis periodoId="2" onElegirDocente={() => {}} />, { rol: 'VICERRECTOR' })
    await screen.findByText('Ana Ruiz')
    await userEvent.selectOptions(screen.getByLabelText('Facultad'), 'Ingeniería')
    expect(nombres()).toEqual(['Ana Ruiz', 'Bruno Paz'])
    await userEvent.selectOptions(screen.getByLabelText('Programa'), 'Electrónica')
    expect(nombres()).toEqual(['Bruno Paz'])
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar' }))
    const chips = within(screen.getByRole('group', { name: 'Filtrar por estado del análisis' }))
    await userEvent.click(chips.getByRole('button', { name: /^Por revisar/ }))
    expect(nombres()).toEqual(['Ana Ruiz'])
    await userEvent.click(chips.getByRole('button', { name: /^Todos/ }))
    await userEvent.selectOptions(screen.getByLabelText('Ordenar por'), 'comentarios')
    expect(nombres()[0]).toBe('Ana Ruiz')
  })

  it('busca una palabra en los comentarios y deja solo los docentes que la tienen', async () => {
    analisisService.obtenerCoincidencias.mockResolvedValue([{ idDocente: 3, coincidencias: 2 }])
    renderizarConSesion(<PanoramaAnalisis periodoId="2" onElegirDocente={() => {}} />, { rol: 'VICERRECTOR' })
    await screen.findByText('Ana Ruiz')
    await userEvent.type(screen.getByPlaceholderText(/Buscar en comentarios/), 'puntual')
    expect(await screen.findByText(/2 con «puntual»/, {}, { timeout: 2000 })).toBeInTheDocument()
    expect(analisisService.obtenerCoincidencias).toHaveBeenLastCalledWith('2', 'puntual')
    expect(nombres()).toEqual(['Carla Díaz'])
  })

  it('la coordinación no ve el filtro de facultad', async () => {
    renderizarConSesion(<PanoramaAnalisis periodoId="2" onElegirDocente={() => {}} />, { rol: 'COORDINADOR' })
    await screen.findByText('Ana Ruiz')
    expect(screen.queryByLabelText('Facultad')).not.toBeInTheDocument()
  })
})
