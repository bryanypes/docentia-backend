import { listarFuentes } from '../../../utils/formatters.js'
import './EtiquetaNivel.css'

/** Chip con la categoría de desempeño que entrega el backend (código y etiqueta). */
export function ChipCategoria({ categoria, sufijo = '' }) {
  const clase = categoria.codigo.toLowerCase().replace('_', '-')
  return (
    <span className={`etiqueta-nivel etiqueta-nivel--${clase}`}>
      {categoria.etiqueta}
      {sufijo}
    </span>
  )
}

/**
 * Píldora con el estado de un promedio: la categoría si existe (parcial si faltan fuentes) o
 * "Pendiente" con las fuentes que faltan. Nunca depende solo del color.
 */
export function EtiquetaNivel({ promedio }) {
  if (!promedio) return <span className="etiqueta-nivel etiqueta-nivel--neutro">Sin periodo activo</span>
  if (!promedio.categoria) {
    const faltan = listarFuentes(promedio.fuentesPendientes)
    return (
      <span className="etiqueta-nivel etiqueta-nivel--pendiente">
        Pendiente{faltan ? `: ${faltan}` : ''}
      </span>
    )
  }
  return <ChipCategoria categoria={promedio.categoria} sufijo={promedio.estado === 'PARCIAL' ? ' · parcial' : ''} />
}
