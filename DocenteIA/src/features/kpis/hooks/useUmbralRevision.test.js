import { describe, expect, it } from 'vitest'
import { paraRevisar, textoUmbral } from './useUmbralRevision.js'

describe('umbral de revisión', () => {
  it('solo marca un promedio completo por debajo del umbral', () => {
    expect(paraRevisar({ estado: 'COMPLETO', valor: 3.99 }, 4)).toBe(true)
    expect(paraRevisar({ estado: 'COMPLETO', valor: 4 }, 4)).toBe(false)
    expect(paraRevisar({ estado: 'PARCIAL', valor: 2.5 }, 4)).toBe(false)
    expect(paraRevisar(null, 4)).toBe(false)
  })

  it('se lee con un decimal y coma', () => {
    expect(textoUmbral(4)).toBe('4,0')
    expect(textoUmbral(3.8)).toBe('3,8')
  })
})
