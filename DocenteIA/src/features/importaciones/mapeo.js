import { CAMPOS_IMPORTACION } from '../../shared/constants/dominio.js'

export const NOMBRE_CAMPO = Object.fromEntries(CAMPOS_IMPORTACION.map((c) => [c.campo, c.nombre]))

/** Campos obligatorios sin columna y campos asignados a más de una columna, según el mapeo actual. */
export function revisarMapeo(mapeo) {
  const asignados = Object.values(mapeo).filter(Boolean)
  const faltantes = CAMPOS_IMPORTACION.filter((c) => c.obligatorio && !asignados.includes(c.campo)).map((c) => c.campo)
  const repetidos = [...new Set(asignados.filter((campo, i) => asignados.indexOf(campo) !== i))]
  return { faltantes, repetidos }
}

/** Solo las columnas con campo asignado: es el JSON `mapeo` que espera la API. */
export function mapeoParaEnviar(mapeo) {
  return Object.fromEntries(Object.entries(mapeo).filter(([, campo]) => Boolean(campo)))
}
