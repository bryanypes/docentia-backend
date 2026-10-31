import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { obtenerCalendario } from '../services/periodosService.js'
import { LineaTiempo } from './LineaTiempo.jsx'

vi.mock('../services/periodosService.js')

describe('LineaTiempo', () => {
  it('marca la etapa vigente con su semana', async () => {
    const etapa = {
      etapa: 'REVISION_HALLAZGOS',
      nombreEtapa: 'Revisión de hallazgos',
      semanasResolucion: 'Semana 15 (consolidación)',
      fechaApertura: '2026-10-13',
      fechaCierre: '2026-11-20',
    }
    obtenerCalendario.mockResolvedValue({ etapas: [etapa], etapaVigente: etapa, seguimiento: 'Seguimiento: período siguiente' })
    render(<LineaTiempo />)
    expect(await screen.findByText('Etapa vigente')).toBeInTheDocument()
    expect(screen.getByText('Semana 15 (consolidación)')).toBeInTheDocument()
  })
})
