const MAXIMO_BYTES_PNG = 300_000
export const MAXIMO_LADO_PNG = 2000
const CABECERA_PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

/**
 * Revisa un archivo subido como firma antes de dibujarlo: debe ser un PNG real (por su cabecera, no
 * por su nombre), de hasta 300 KB. El servidor repite estas comprobaciones y lo recodifica.
 */
export async function revisarPngFirma(archivo) {
  if (!archivo) return 'Elige un archivo PNG con tu firma.'
  if (archivo.size > MAXIMO_BYTES_PNG) return 'La imagen supera los 300 KB.'
  const cabecera = new Uint8Array(await archivo.slice(0, 8).arrayBuffer())
  if (cabecera.length < 8 || CABECERA_PNG.some((b, i) => cabecera[i] !== b)) {
    return 'El archivo no es un PNG válido. Otros formatos, como SVG o JPG, no se aceptan.'
  }
  return null
}
