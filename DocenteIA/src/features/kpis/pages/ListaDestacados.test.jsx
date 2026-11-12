import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ListaDestacados } from './Inicio.jsx'

const docente = (id, nombre, programa, estado, valor) => ({
  docente: { idDocente: id, nombreCompleto: nombre, nombrePrograma: programa },
  promedio: { estado, valor },
})

describe('ListaDestacados', () => {
  it('lista solo los completos con el umbral o más, de mayor a menor', () => {
    const seguimiento = {
      datos: {
        docentes: [
          docente(1, 'Julián Hoyos', 'Ingeniería de Software', 'COMPLETO', 4.34),
          docente(2, 'Camilo Vidal', 'Ingeniería de Software', 'COMPLETO', 4.75),
          docente(3, 'Ana Parcial', 'Derecho', 'PARCIAL', 4.9),
          docente(4, 'Luis Justo', 'Derecho', 'COMPLETO', 4.5),
        ],
      },
    }
    render(<ListaDestacados seguimiento={seguimiento} umbral={4.5} />)
    const filas = within(screen.getByRole('table')).getAllByRole('row').slice(1)
    expect(filas.map((f) => f.textContent)).toEqual(['Camilo VidalIngeniería de Software4,75', 'Luis JustoDerecho4,50'])
  })

  it('sin destacados lo dice', () => {
    render(<ListaDestacados seguimiento={{ datos: { docentes: [] } }} umbral={4.5} />)
    expect(screen.getByText(/Ningún docente/)).toBeInTheDocument()
  })
})
