import { render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { obtenerCalendario } from '../services/periodosService.js'
import { estadoEtapa } from '../etapas.js'
import { LineaTiempo } from './LineaTiempo.jsx'

vi.mock('../services/periodosService.js')

const ETAPAS = [
  {
    etapa: 'RECOLECCION_CALIFICACIONES',
    nombreEtapa: 'Recolección de calificaciones',
    semanasResolucion: 'Semanas 11 a 14',
    fechaApertura: '2026-09-28',
    fechaCierre: '2026-10-30',
  },
  {
    etapa: 'REVISION_HALLAZGOS',
    nombreEtapa: 'Revisión de hallazgos',
    semanasResolucion: 'Semana 15 (consolidación)',
    fechaApertura: '2026-10-13',
    fechaCierre: '2026-11-20',
  },
  {
    etapa: 'FIRMA_ACTAS',
    nombreEtapa: 'Firma de actas',
    semanasResolucion: 'Semana 16 y posteriores',
    fechaApertura: '2026-11-23',
    fechaCierre: '2026-12-04',
  },
]

describe('LineaTiempo', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    // 10 de noviembre de 2026, mediodía en Colombia.
    vi.setSystemTime(new Date('2026-11-10T17:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra cada etapa como un paso con su estado y resalta la vigente', async () => {
    obtenerCalendario.mockResolvedValue({ etapas: ETAPAS, etapaVigente: ETAPAS[1], seguimiento: 'Seguimiento: período siguiente' })
    render(<LineaTiempo />)

    const pasos = await screen.findAllByRole('listitem')
    expect(pasos).toHaveLength(3)
    expect(within(pasos[0]).getByText('Paso 1')).toBeInTheDocument()
    expect(within(pasos[0]).getByText('Terminada')).toBeInTheDocument()
    expect(pasos[1]).toHaveAttribute('aria-current', 'step')
    expect(within(pasos[1]).getByText('Etapa vigente')).toBeInTheDocument()
    expect(within(pasos[1]).getByText('Cierra en 10 días')).toBeInTheDocument()
    expect(within(pasos[2]).getByText('Próxima')).toBeInTheDocument()
    expect(screen.getByText('1 de 3 terminadas')).toBeInTheDocument()
  })

  it('una etapa sin fechas queda sin programar', () => {
    expect(estadoEtapa({ fechaApertura: null, fechaCierre: null }, '2026-11-10')).toBe('sin')
  })
})
