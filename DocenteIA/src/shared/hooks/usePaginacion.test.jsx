import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useSearchParams } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { useFiltrosUrl, usePaginacion } from './usePaginacion.js'

const FILAS = Array.from({ length: 25 }, (_, indice) => `Docente ${indice + 1}`)

function ListaPaginada() {
  const [filtros, actualizarFiltros] = useFiltrosUrl({ busqueda: '' })
  const filas = FILAS.filter((fila) => fila.includes(filtros.busqueda))
  const paginacion = usePaginacion(filas)
  const [parametros] = useSearchParams()

  return (
    <>
      <button type="button" onClick={() => actualizarFiltros({ busqueda: 'Docente 2' })}>
        Filtrar
      </button>
      <output data-testid="rango">{`${paginacion.desde}-${paginacion.hasta} de ${paginacion.total}`}</output>
      <output data-testid="filas">{paginacion.visibles.join(',')}</output>
      <output data-testid="url">{parametros.toString()}</output>
    </>
  )
}

describe('usePaginacion', () => {
  it('conserva parámetros ajenos y vuelve a la primera página al filtrar', async () => {
    render(
      <MemoryRouter initialEntries={['/?pagina=2&tamano=10&orden=nombre']}>
        <ListaPaginada />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('rango')).toHaveTextContent('11-20 de 25')
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    await waitFor(() => expect(screen.getByTestId('rango')).toHaveTextContent('1-7 de 7'))
    expect(screen.getByTestId('filas')).toHaveTextContent('Docente 2,Docente 20,Docente 21,Docente 22,Docente 23,Docente 24,Docente 25')
    expect(screen.getByTestId('url').textContent).not.toContain('tamano=')
    expect(screen.getByTestId('url').textContent).toContain('orden=nombre')
    expect(screen.getByTestId('url').textContent).toContain('busqueda=Docente+2')
    expect(screen.getByTestId('url').textContent).not.toContain('pagina=')
  })
})
