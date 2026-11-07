import { describe, expect, it } from 'vitest'
import { analizarSeguimiento } from './seguimiento.js'

const docente = (nombre, valor) => ({
  docente: { nombreFacultad: 'F', nombrePrograma: nombre },
  promedio: { estado: 'COMPLETO', valor, evaluadoresPendientes: [] },
})

describe('analizarSeguimiento', () => {
  it('usa el umbral recibido, no uno fijo', () => {
    const lista = [docente('A', 3.9), docente('B', 3.7)]
    expect(analizarSeguimiento(lista, null, 'nombrePrograma', 3.8).atencion).toHaveLength(1)
    expect(analizarSeguimiento(lista, null, 'nombrePrograma', 4).atencion).toHaveLength(2)
  })

  it('reparte en las cinco categorías', () => {
    const { porNivel } = analizarSeguimiento([docente('A', 2.85), docente('B', 1.9)], null, 'nombrePrograma', 3.8)
    expect(porNivel.map((n) => n.cantidad)).toEqual([0, 0, 0, 1, 1])
  })
})
