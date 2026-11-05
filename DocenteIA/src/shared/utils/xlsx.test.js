import { describe, expect, it } from 'vitest'
import { generarXlsx, letraColumna } from './xlsx.js'

const texto = (bytes) => new TextDecoder().decode(bytes)

describe('xlsx', () => {
  it('nombra las columnas como Excel', () => {
    expect([0, 25, 26, 51, 52].map(letraColumna)).toEqual(['A', 'Z', 'AA', 'AZ', 'BA'])
  })

  it('arma un ZIP con el libro, las hojas, el autofiltro y las fórmulas', () => {
    const bytes = generarXlsx([
      {
        nombre: 'Docentes',
        titulo: 'Prueba',
        columnas: [{ titulo: 'Nombre' }, { titulo: 'Nota', formato: 'nota' }],
        filas: [
          ['Ana <López> & Cía', 4.5],
          ['Luis', { formula: 'B4*2', formato: 'entero' }],
        ],
      },
    ])
    // Firma de un ZIP y de su directorio central.
    expect([...bytes.slice(0, 4)]).toEqual([0x50, 0x4b, 0x03, 0x04])
    const contenido = texto(bytes)
    expect(contenido).toContain('[Content_Types].xml')
    expect(contenido).toContain('<sheet name="Docentes"')
    expect(contenido).toContain('<autoFilter ref="A3:B5"/>')
    expect(contenido).toContain('state="frozen"')
    expect(contenido).toContain('Ana &lt;López&gt; &amp; Cía')
    expect(contenido).toContain('<c r="B4" s="2"><v>4.5</v></c>')
    expect(contenido).toContain('<c r="B5" s="1"><f>B4*2</f></c>')
  })
})
