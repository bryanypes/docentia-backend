import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { textoFormula } from '../../../shared/utils/formatters.js'
import { PanelFuentes } from './PanelesExpediente.jsx'

const NOTAS = [
  { fuente: 'ESTUDIANTES', nota: 4.5, peso: 50 },
  { fuente: 'COORDINACION', nota: 4, peso: 30 },
  { fuente: 'AUTOEVALUACION', nota: null, peso: 20 },
]

describe('PanelFuentes', () => {
  it('muestra la fórmula con los pesos', () => {
    render(<PanelFuentes notasPorFuente={NOTAS} promedio={null} />)
    expect(screen.getByText('50 % estudiantes · 30 % coordinación · 20 % autoevaluación')).toBeInTheDocument()
  })

  it('avisa del promedio parcial con el evaluador que falta', () => {
    render(
      <PanelFuentes
        notasPorFuente={NOTAS}
        promedio={{ estado: 'PARCIAL', valor: 4.31, evaluadoresPendientes: ['AUTOEVALUACION'], categoria: null }}
      />,
    )
    expect(screen.getByText('Promedio parcial')).toBeInTheDocument()
    expect(screen.getByText(/Falta Autoevaluación/)).toBeInTheDocument()
  })

  it('arma el texto de la fórmula', () => {
    expect(textoFormula({ pesoEstudiantes: 50, pesoCoordinacion: 30, pesoAutoevaluacion: 20 })).toContain('20 % autoevaluación')
  })
})
