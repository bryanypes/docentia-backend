import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { EtiquetaNivel } from './EtiquetaNivel.jsx'

const categoria = (codigo, etiqueta) => ({ codigo, etiqueta, accionInstitucional: 'x' })

describe('EtiquetaNivel', () => {
  it.each([
    ['MUY_ALTO', 'Muy alto', 'muy-alto'],
    ['ALTO', 'Alto', 'alto'],
    ['ACEPTABLE', 'Aceptable', 'aceptable'],
    ['BAJO', 'Bajo', 'bajo'],
    ['MUY_BAJO', 'Muy bajo', 'muy-bajo'],
  ])('muestra %s con texto y su clase', (codigo, etiqueta, clase) => {
    render(<EtiquetaNivel promedio={{ estado: 'COMPLETO', categoria: categoria(codigo, etiqueta) }} />)
    expect(screen.getByText(etiqueta)).toHaveClass(`etiqueta-nivel--${clase}`)
  })

  it('marca el promedio parcial', () => {
    render(<EtiquetaNivel promedio={{ estado: 'PARCIAL', categoria: categoria('ALTO', 'Alto') }} />)
    expect(screen.getByText('Alto · parcial')).toBeInTheDocument()
  })

  it('sin categoría nombra las fuentes que faltan', () => {
    render(<EtiquetaNivel promedio={{ estado: 'SIN_NOTA', categoria: null, fuentesPendientes: ['AUTOEVALUACION'] }} />)
    expect(screen.getByText('Pendiente: Autoevaluación')).toBeInTheDocument()
  })
})
