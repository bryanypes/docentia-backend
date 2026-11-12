import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { PanelComparativoPrograma } from './PanelComparativoPrograma.jsx'

vi.mock('../../../shared/hooks/useFetch.js', () => ({ useFetch: vi.fn() }))

describe('PanelComparativoPrograma', () => {
  beforeEach(() => {
    useFetch.mockReturnValue({
      cargando: false,
      error: null,
      datos: {
        nombrePrograma: 'Ingeniería de Software',
        fuentes: [
          { fuente: 'ESTUDIANTES', notaDocente: 4.2, promedioPrograma: 3.8 },
          { fuente: 'COORDINACION', notaDocente: null, promedioPrograma: 4.1 },
          { fuente: 'AUTOEVALUACION', notaDocente: 3.7, promedioPrograma: null },
        ],
      },
    })
  })

  it('muestra las tres fuentes y distingue los datos ausentes', () => {
    render(<PanelComparativoPrograma idDocente={12} />)

    expect(screen.getByText(/Tus notas frente al promedio real del programa/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Estudiantes' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Coordinación' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Autoevaluación' })).toBeInTheDocument()
    expect(screen.getByRole('table', { name: /Comparativo de notas por evaluador/ })).toBeInTheDocument()
    expect(screen.getAllByText('—')).toHaveLength(4)
  })
})
