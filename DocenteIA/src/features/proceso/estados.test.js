import { describe, expect, it } from 'vitest'
import { describirCobertura } from './estados.js'

describe('describirCobertura', () => {
  it('sin matrícula no muestra "0 de 0": dice de dónde salió la nota', () => {
    expect(describirCobertura({ respondidas: 0, esperadas: 0, minimoRespuestas: 5, origenNota: 'MANUAL' })).toEqual({
      titulo: 'Sin matrícula en la app',
      detalle: 'La nota de estudiantes está registrada sin formularios.',
    })
  })

  it('bajo el mínimo de anonimato avisa cuántas faltan y el origen de la nota vigente', () => {
    const r = describirCobertura({ respondidas: 4, esperadas: 6, minimoRespuestas: 5, origenNota: 'CSV' })
    expect(r.titulo).toBe('4 de 6 respondieron')
    expect(r.detalle).toBe('Falta 1 para usar el formulario (mínimo 5, por anonimato). La nota actual está cargada por CSV.')
  })

  it('con la nota del formulario lo dice', () => {
    expect(describirCobertura({ respondidas: 8, esperadas: 10, minimoRespuestas: 5, origenNota: 'FORMULARIO' }).detalle).toBe(
      'La nota sale de sus formularios.',
    )
  })
})
