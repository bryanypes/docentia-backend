import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as analisisService from '../../analisis/services/analisisService.js'
import { ComentariosResumen } from './ComentariosResumen.jsx'

vi.mock('../../analisis/services/analisisService.js')

// Forma del ejemplo docs/ejemplos/ia/resumen.json del contrato.
const RESUMEN = {
  disponible: true,
  motivo: null,
  periodoId: 1,
  docentesConAnalisis: 3,
  minimoDocentes: 3,
  nComentarios: 30,
  nExcluidos: { INSTRUCCION_SOSPECHOSA: 2 },
  sentimientoGlobal: { positivos: 21, negativos: 9, neutros: 0, mixtos: 0 },
  temas: [
    { criterio: 'C2', descriptor: 'D05', nombre: 'Claridad al explicar ideas', polaridad: 'POSITIVO', frecuencia: 10, porcentaje: 33.3, docentes: 3 },
    { criterio: 'C3', descriptor: 'D08', nombre: 'Retroalimentación sobre los resultados', polaridad: 'NEGATIVO', frecuencia: 9, porcentaje: 30.0, docentes: 3 },
  ],
  criterios: [
    { codigo: 'C2', nombre: 'Capacidad para ejecutar', positivos: 10, negativos: 0, neutros: 0 },
    { codigo: 'C3', nombre: 'Capacidad para evaluar', positivos: 0, negativos: 12, neutros: 0 },
  ],
  advertencias: { SENSIBLE_ACOSO: 1 },
}

beforeEach(() => vi.resetAllMocks())

describe('ComentariosResumen', () => {
  it('pide el resumen del periodo y el programa y muestra sentimiento, criterios y temas', async () => {
    analisisService.obtenerResumenComentarios.mockResolvedValue(RESUMEN)
    render(<ComentariosResumen periodoId={1} programaId={7} alcance="Software" esCoordinador={false} />)

    expect(await screen.findByRole('img', { name: 'Índice de sentimiento +40 sobre 100' })).toBeInTheDocument()
    expect(analisisService.obtenerResumenComentarios).toHaveBeenCalledWith({ periodo: 1, programa: 7 })
    expect(screen.getByText('Claridad al explicar ideas').closest('li')).toHaveTextContent('33,3 %')
    expect(screen.getByText('Retroalimentación sobre los resultados').closest('li')).toHaveTextContent('Por mejorar')
    expect(screen.getByText(/2 órdenes al modelo/)).toBeInTheDocument()
    // La vicerrectoría no ve las advertencias sensibles.
    expect(screen.queryByText(/Advertencias para revisar/)).not.toBeInTheDocument()
  })

  it('la coordinación ve las advertencias, sin textos de comentarios', async () => {
    analisisService.obtenerResumenComentarios.mockResolvedValue(RESUMEN)
    render(<ComentariosResumen periodoId={1} alcance="Tu facultad" esCoordinador />)
    expect(await screen.findByText(/Advertencias para revisar/)).toBeInTheDocument()
  })

  it('con pocos docentes analizados explica por qué no se muestra', async () => {
    analisisService.obtenerResumenComentarios.mockResolvedValue({
      ...RESUMEN,
      disponible: false,
      motivo: 'GRUPO_PEQUENO',
      docentesConAnalisis: 1,
      temas: [],
      criterios: [],
    })
    render(<ComentariosResumen periodoId={1} alcance="Software" />)
    expect(await screen.findByText('Todavía no hay suficientes análisis')).toBeInTheDocument()
    expect(screen.getByText(/al menos 3 docentes analizados en esta selección; hay 1/)).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
