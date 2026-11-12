import { describe, expect, it } from 'vitest'
import { errorComentario } from './validarComentario.js'

describe('errorComentario', () => {
  it('rechaza vacío, corto, sin letras y repetido', () => {
    for (const malo of ['', '   ', 'corto', '..........', '1234567890!', 'aaaaaaaaaaaa', 'a a a a a a a']) {
      expect(errorComentario(malo)).not.toBeNull()
    }
  })

  it('acepta un comentario real con espacios en los extremos', () => {
    expect(errorComentario('  Explica con ejemplos  ')).toBeNull()
  })
})
