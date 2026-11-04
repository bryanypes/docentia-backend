import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as analisisService from '../services/analisisService.js'
import { ComentariosDocente } from './ComentariosDocente.jsx'
import { ResultadoComentarios } from './ResultadoComentarios.jsx'

vi.mock('../services/analisisService.js')

// Forma de docs/ejemplos/ia/analisis-completado.json (contrato api-comentarios-ia.md).
const TEMA_FORTALEZA = {
  criterio: 'C2',
  descriptor: 'D05',
  nombre: 'Claridad al explicar ideas e instrucciones',
  polaridad: 'POSITIVO',
  frecuencia: 4,
  porcentaje: 36.36,
  recurrente: true,
  citas: [{ comentarioId: 5, fragmento: 'Sus explicaciones son excelentes y muy claras' }],
}
const TEMA_OPORTUNIDAD = {
  criterio: 'C3',
  descriptor: 'D07',
  nombre: 'Retroalimentación sobre los resultados',
  polaridad: 'NEGATIVO',
  frecuencia: 3,
  porcentaje: 27.27,
  recurrente: true,
  citas: [{ comentarioId: 1, fragmento: 'Demora mucho la retroalimentación' }],
}
const COMPLETADO = {
  estado: 'COMPLETADO',
  motivo: null,
  mensaje: null,
  docenteId: 1,
  nombreDocente: 'Ana María Ruiz',
  periodoId: 1,
  nombrePeriodo: '2026-2',
  fuente: 'FORMULARIO',
  version: 1,
  generadoEn: '2026-11-03T22:09:31',
  estadoHallazgos: 'PENDIENTE_VALIDACION',
  resultado: {
    tipoModelo: 'REGLAS',
    nComentarios: 11,
    nExcluidos: { INSTRUCCION_SOSPECHOSA: 1 },
    sentimientoGlobal: { positivos: 8, negativos: 3, neutros: 0, mixtos: 0, indice: 0.45 },
    temas: [TEMA_FORTALEZA, TEMA_OPORTUNIDAD],
    aspectos: { fortalezas: [TEMA_FORTALEZA], oportunidades: [TEMA_OPORTUNIDAD] },
    tendencias: [{ criterio: 'C2', descriptor: 'D05', polaridad: 'POSITIVO', porcentajeAnterior: null, porcentajeActual: 36.36, cambio: 'SIN_REFERENCIA' }],
    advertencias: [{ tipo: 'SENSIBLE_ACOSO', cantidad: 1, mensaje: 'Hay un comentario que podría describir acoso.' }],
    criterios: [],
  },
}
const COMENTARIOS = {
  disponible: true,
  total: 1,
  tamano: 10,
  comentarios: [
    {
      indice: 1,
      texto: 'Demora mucho la retroalimentación de los parciales.',
      sentimiento: 'NEGATIVO',
      etiquetas: [{ criterio: 'C3', descriptor: 'D07', nombre: 'Retroalimentación sobre los resultados', polaridad: 'NEGATIVO' }],
    },
  ],
}

function Vista({ esCoordinador = true }) {
  const [criterio, setCriterio] = useState(null)
  return (
    <>
      <ResultadoComentarios idDocente={1} periodoId={1} esCoordinador={esCoordinador} criterioActivo={criterio} onFiltrarCriterio={setCriterio} />
      <ComentariosDocente idDocente={1} periodoId={1} criterio={criterio} onCriterio={setCriterio} />
    </>
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  analisisService.listarComentariosDocente.mockResolvedValue(COMENTARIOS)
})


describe('Vista de comentarios del docente', () => {
  it('muestra el resultado con citas tal cual, el rótulo honesto y la validación pendiente', async () => {
    analisisService.obtenerAnalisisDocente.mockResolvedValue(COMPLETADO)
    render(<Vista />)
    expect(await screen.findByText('Análisis automático por reglas')).toBeInTheDocument()
    expect(screen.getByText('Pendiente de validación por coordinación')).toBeInTheDocument()
    const fortalezas = screen.getByRole('heading', { name: 'Fortalezas' }).closest('section')
    expect(within(fortalezas).getByText('«Sus explicaciones son excelentes y muy claras»')).toBeInTheDocument()
    expect(screen.getByText('+45')).toBeInTheDocument()
    expect(screen.getByText(/Requiere atención de coordinación/)).toBeInTheDocument()
    expect(await screen.findByText('Demora mucho la retroalimentación de los parciales.')).toBeInTheDocument()
  })

  it('tocar un tema filtra los comentarios por su criterio', async () => {
    analisisService.obtenerAnalisisDocente.mockResolvedValue(COMPLETADO)
    render(<Vista />)
    await userEvent.click(await screen.findByRole('button', { name: /Retroalimentación sobre los resultados/ }))
    expect(analisisService.listarComentariosDocente).toHaveBeenLastCalledWith(1, expect.objectContaining({ criterio: 'C3', pagina: 0 }))
    expect(screen.getByRole('button', { name: 'Quitar C3' })).toBeInTheDocument()
  })

  it('la vicerrectoría no ve las advertencias sensibles', async () => {
    analisisService.obtenerAnalisisDocente.mockResolvedValue(COMPLETADO)
    render(<Vista esCoordinador={false} />)
    await screen.findByText('Análisis automático por reglas')
    expect(screen.queryByText(/Requiere atención de coordinación/)).not.toBeInTheDocument()
  })

  it('en proceso vuelve a consultar sola hasta que termina', async () => {
    analisisService.obtenerAnalisisDocente
      .mockResolvedValueOnce({ ...COMPLETADO, estado: 'EN_PROCESO', mensaje: 'El análisis se está generando.', resultado: null })
      .mockResolvedValue(COMPLETADO)
    render(<Vista />)
    expect(await screen.findByText('Estoy analizando los comentarios')).toBeInTheDocument()
    // Sondea cada 4 s: el resultado aparece sin recargar la página.
    expect(await screen.findByText('Análisis automático por reglas', {}, { timeout: 6000 })).toBeInTheDocument()
    expect(analisisService.obtenerAnalisisDocente).toHaveBeenCalledTimes(2)
  })
})
