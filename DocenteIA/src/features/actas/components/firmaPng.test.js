import { describe, expect, it } from 'vitest'
import { revisarPngFirma } from './firmaPng.js'

const CABECERA_PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

function archivo(bytes, nombre, tipo) {
  return new File([new Uint8Array(bytes)], nombre, { type: tipo })
}

describe('revisarPngFirma', () => {
  it('acepta un archivo con cabecera PNG real y tamaño permitido', async () => {
    expect(await revisarPngFirma(archivo([...CABECERA_PNG, 0, 0, 0, 13], 'firma.png', 'image/png'))).toBeNull()
  })

  it('rechaza un SVG aunque se llame .png', async () => {
    const svg = new TextEncoder().encode('<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>')
    expect(await revisarPngFirma(archivo(svg, 'firma.png', 'image/png'))).toMatch(/no es un PNG válido/)
  })

  it('rechaza un JPG', async () => {
    expect(await revisarPngFirma(archivo([0xff, 0xd8, 0xff, 0xe0, 0, 0, 0, 0], 'firma.jpg', 'image/jpeg'))).toMatch(
      /no es un PNG válido/,
    )
  })

  it('rechaza más de 300 KB', async () => {
    const grande = new Uint8Array(300_001)
    grande.set(CABECERA_PNG)
    expect(await revisarPngFirma(archivo(grande, 'firma.png', 'image/png'))).toMatch(/300 KB/)
  })

  it('pide un archivo si no se eligió ninguno', async () => {
    expect(await revisarPngFirma(undefined)).toMatch(/Elige/)
  })
})
