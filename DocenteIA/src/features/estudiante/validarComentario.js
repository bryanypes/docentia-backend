/** Mismas reglas que el backend: obligatorio, 10 caracteres, 3 letras y no un carácter repetido. */
export function errorComentario(texto) {
  const limpio = (texto ?? '').trim()
  if (!limpio) return 'Escribe un comentario sobre el profesor.'
  if (limpio.length < 10) return 'El comentario debe tener al menos 10 caracteres.'
  if ((limpio.match(/\p{L}/gu) ?? []).length < 3) return 'El comentario debe tener al menos 3 letras, no solo signos o números.'
  if (new Set(limpio.replace(/\s/g, '')).size === 1) return 'El comentario no puede ser un mismo carácter repetido.'
  return null
}
