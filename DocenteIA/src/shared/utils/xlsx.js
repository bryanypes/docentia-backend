/**
 * Libro de Excel (.xlsx) sin dependencias: hojas con encabezado fijo, autofiltro, anchos, formatos
 * numéricos y fórmulas. El archivo es un ZIP sin compresión con el XML mínimo de Office Open XML.
 *
 * Una hoja: { nombre, titulo?, columnas: [{ titulo, ancho?, formato? }], filas: [[valor | { formula }]] }
 * formato: 'texto' (por defecto), 'entero', 'nota' (0,00) o 'porcentaje' (0 %; el valor va de 0 a 1).
 * Una celda también puede ser { valor, formato } o { formula, formato } para cambiar el formato de su columna.
 */

const FORMATOS = { texto: 0, entero: 1, nota: 2, porcentaje: 3 }
// Estilos (cellXfs): 0 normal, 1 entero, 2 nota, 3 porcentaje, 4 encabezado, 5 título.
const ESTILO_ENCABEZADO = 4
const ESTILO_TITULO = 5

const escapar = (texto) =>
  String(texto)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    // Caracteres de control que el XML no admite.
    // eslint-disable-next-line no-control-regex
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')

/** Letra de la columna: 0 → A, 26 → AA. */
export function letraColumna(indice) {
  let letra = ''
  let n = indice + 1
  while (n > 0) {
    const resto = (n - 1) % 26
    letra = String.fromCharCode(65 + resto) + letra
    n = Math.floor((n - 1) / 26)
  }
  return letra
}

function celda(referencia, valor, estilo) {
  // Una celda puede traer su propio formato: { valor | formula, formato }.
  if (valor && typeof valor === 'object' && valor.formato) {
    const { formato, ...resto } = valor
    return celda(referencia, 'formula' in resto ? resto : resto.valor, FORMATOS[formato])
  }
  if (valor === null || valor === undefined || valor === '') return `<c r="${referencia}" s="${estilo}"/>`
  if (typeof valor === 'object' && 'formula' in valor) {
    return `<c r="${referencia}" s="${estilo}"><f>${escapar(valor.formula)}</f></c>`
  }
  if (typeof valor === 'number' && Number.isFinite(valor)) return `<c r="${referencia}" s="${estilo}"><v>${valor}</v></c>`
  return `<c r="${referencia}" s="${estilo}" t="inlineStr"><is><t xml:space="preserve">${escapar(valor)}</t></is></c>`
}

function xmlHoja({ titulo, columnas, filas }) {
  const desplazamiento = titulo ? 2 : 0
  const filaEncabezado = desplazamiento + 1
  const ultimaColumna = letraColumna(columnas.length - 1)
  const ultimaFila = filaEncabezado + filas.length
  const xml = []

  if (titulo) {
    xml.push(`<row r="1">${celda('A1', titulo, ESTILO_TITULO)}</row>`)
  }
  xml.push(
    `<row r="${filaEncabezado}">${columnas
      .map((c, i) => celda(`${letraColumna(i)}${filaEncabezado}`, c.titulo, ESTILO_ENCABEZADO))
      .join('')}</row>`,
  )
  filas.forEach((fila, f) => {
    const numero = filaEncabezado + 1 + f
    xml.push(
      `<row r="${numero}">${columnas
        .map((c, i) => celda(`${letraColumna(i)}${numero}`, fila[i], FORMATOS[c.formato ?? 'texto']))
        .join('')}</row>`,
    )
  })

  const anchos = columnas.map((c, i) => `<col min="${i + 1}" max="${i + 1}" width="${c.ancho ?? 16}" customWidth="1"/>`).join('')
  return (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
    `<sheetViews><sheetView workbookViewId="0"><pane ySplit="${filaEncabezado}" topLeftCell="A${filaEncabezado + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>` +
    `<cols>${anchos}</cols>` +
    `<sheetData>${xml.join('')}</sheetData>` +
    (filas.length ? `<autoFilter ref="A${filaEncabezado}:${ultimaColumna}${ultimaFila}"/>` : '') +
    '</worksheet>'
  )
}

const ESTILOS =
  '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
  '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
  '<numFmts count="1"><numFmt numFmtId="164" formatCode="0.00"/></numFmts>' +
  '<fonts count="3"><font><sz val="11"/><name val="Calibri"/></font>' +
  '<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>' +
  '<font><b/><sz val="14"/><color rgb="FF1F3A68"/><name val="Calibri"/></font></fonts>' +
  '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>' +
  '<fill><patternFill patternType="solid"><fgColor rgb="FF1F3A68"/><bgColor indexed="64"/></patternFill></fill></fills>' +
  '<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border>' +
  '<border><left/><right/><top/><bottom style="thin"><color rgb="FFD9DDE5"/></bottom><diagonal/></border></borders>' +
  '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>' +
  '<cellXfs count="6">' +
  '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>' +
  '<xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>' +
  '<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>' +
  '<xf numFmtId="9" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>' +
  '<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>' +
  '<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>' +
  '</cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>'

function archivosDelLibro(hojas) {
  const tipos =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
    '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
    '<Default Extension="xml" ContentType="application/xml"/>' +
    '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
    '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>' +
    hojas
      .map(
        (_, i) =>
          `<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`,
      )
      .join('') +
    '</Types>'
  const relaciones =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>' +
    '</Relationships>'
  const libro =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
    `<sheets>${hojas.map((h, i) => `<sheet name="${escapar(nombreHoja(h.nombre))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>`).join('')}</sheets>` +
    '<calcPr calcId="191029" fullCalcOnLoad="1"/>' +
    '</workbook>'
  const relacionesLibro =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
    hojas
      .map(
        (_, i) =>
          `<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>`,
      )
      .join('') +
    `<Relationship Id="rId${hojas.length + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>` +
    '</Relationships>'

  return [
    ['[Content_Types].xml', tipos],
    ['_rels/.rels', relaciones],
    ['xl/workbook.xml', libro],
    ['xl/_rels/workbook.xml.rels', relacionesLibro],
    ['xl/styles.xml', ESTILOS],
    ...hojas.map((h, i) => [`xl/worksheets/sheet${i + 1}.xml`, xmlHoja(h)]),
  ]
}

/** Excel limita el nombre de la hoja a 31 caracteres y prohíbe : \ / ? * [ ]. */
function nombreHoja(nombre) {
  return String(nombre).replace(/[:\\/?*[\]]/g, ' ').slice(0, 31)
}

const TABLA_CRC = (() => {
  const tabla = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    tabla[n] = c >>> 0
  }
  return tabla
})()

function crc32(bytes) {
  let crc = 0xffffffff
  for (const byte of bytes) crc = TABLA_CRC[(crc ^ byte) & 0xff] ^ (crc >>> 8)
  return (crc ^ 0xffffffff) >>> 0
}

/** ZIP sin compresión (método 0): suficiente para el XML de un libro. */
function comprimir(archivos) {
  const codificador = new TextEncoder()
  const partes = []
  const central = []
  let desplazamiento = 0

  for (const [nombre, contenido] of archivos) {
    const nombreBytes = codificador.encode(nombre)
    const datos = codificador.encode(contenido)
    const crc = crc32(datos)

    const local = new DataView(new ArrayBuffer(30))
    local.setUint32(0, 0x04034b50, true)
    local.setUint16(4, 20, true)
    local.setUint16(6, 0x0800, true) // nombres en UTF-8
    local.setUint32(14, crc, true)
    local.setUint32(18, datos.length, true)
    local.setUint32(22, datos.length, true)
    local.setUint16(26, nombreBytes.length, true)
    partes.push(new Uint8Array(local.buffer), nombreBytes, datos)

    const entrada = new DataView(new ArrayBuffer(46))
    entrada.setUint32(0, 0x02014b50, true)
    entrada.setUint16(4, 20, true)
    entrada.setUint16(6, 20, true)
    entrada.setUint16(8, 0x0800, true)
    entrada.setUint32(16, crc, true)
    entrada.setUint32(20, datos.length, true)
    entrada.setUint32(24, datos.length, true)
    entrada.setUint16(28, nombreBytes.length, true)
    entrada.setUint32(42, desplazamiento, true)
    central.push(new Uint8Array(entrada.buffer), nombreBytes)

    desplazamiento += 30 + nombreBytes.length + datos.length
  }

  const tamanoCentral = central.reduce((a, p) => a + p.length, 0)
  const fin = new DataView(new ArrayBuffer(22))
  fin.setUint32(0, 0x06054b50, true)
  fin.setUint16(8, archivos.length, true)
  fin.setUint16(10, archivos.length, true)
  fin.setUint32(12, tamanoCentral, true)
  fin.setUint32(16, desplazamiento, true)

  const todo = [...partes, ...central, new Uint8Array(fin.buffer)]
  const salida = new Uint8Array(todo.reduce((a, p) => a + p.length, 0))
  let posicion = 0
  for (const parte of todo) {
    salida.set(parte, posicion)
    posicion += parte.length
  }
  return salida
}

/** Bytes del .xlsx. */
export function generarXlsx(hojas) {
  return comprimir(archivosDelLibro(hojas))
}

/** Genera el libro y lo descarga con el nombre dado. */
export function descargarXlsx(nombreArchivo, hojas) {
  const blob = new Blob([generarXlsx(hojas)], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.append(enlace)
  enlace.click()
  enlace.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
