import { listarFuentes, nivelDeNota } from '../../../utils/formatters.js'
import './EtiquetaNivel.css'

/**
 * Píldora con el estado de un promedio: nivel de desempeño si está completo,
 * o "Pendiente" con las fuentes que faltan. Nunca depende solo del color.
 */
export function EtiquetaNivel({ promedio }) {
  if (!promedio) return <span className="etiqueta-nivel etiqueta-nivel--neutro">Sin periodo activo</span>
  if (promedio.estado === 'INCOMPLETO') {
    const faltan = listarFuentes(promedio.fuentesPendientes)
    return (
      <span className="etiqueta-nivel etiqueta-nivel--pendiente">
        Pendiente{faltan ? `: ${faltan}` : ''}
      </span>
    )
  }
  const nivel = nivelDeNota(promedio.valor)
  return <span className={`etiqueta-nivel etiqueta-nivel--${nivel.clase}`}>{nivel.etiqueta}</span>
}
