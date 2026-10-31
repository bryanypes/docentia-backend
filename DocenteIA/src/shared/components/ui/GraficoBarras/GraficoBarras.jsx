import { useId } from 'react'
import './GraficoBarras.css'

/**
 * Barras horizontales de una sola serie (un solo color: compara magnitudes, no identidades).
 * Cada barra lleva su valor escrito al final; el detalle aparece al pasar el mouse o al enfocar la
 * fila con el teclado. Es una lista: los lectores de pantalla leen etiqueta, valor y detalle.
 *
 * filas: [{ id, etiqueta, valor, textoValor, detalle?, destacada? }]
 * maximo: valor que ocupa el ancho completo (por ejemplo 5 para notas, o el total de docentes)
 */
export function GraficoBarras({ titulo, filas, maximo }) {
  const base = useId()
  const tope = maximo || Math.max(1, ...filas.map((f) => f.valor))

  return (
    <ul className="grafico-barras" aria-label={titulo}>
      {filas.map((fila) => {
        const ancho = Math.max(0, Math.min(100, (fila.valor / tope) * 100))
        const idDetalle = `${base}-${fila.id}`
        return (
          <li
            key={fila.id}
            className="grafico-barras__fila"
            tabIndex={fila.detalle ? 0 : undefined}
            aria-describedby={fila.detalle ? idDetalle : undefined}
          >
            <span className="grafico-barras__etiqueta">{fila.etiqueta}</span>
            <span className="grafico-barras__riel" aria-hidden="true">
              <span
                className={`grafico-barras__barra${fila.destacada ? ' grafico-barras__barra--destacada' : ''}`}
                style={{ width: `${ancho}%` }}
              />
            </span>
            <span className="grafico-barras__valor">{fila.textoValor}</span>
            {fila.detalle && (
              <span id={idDetalle} className="grafico-barras__detalle" role="tooltip">
                {fila.detalle}
              </span>
            )}
          </li>
        )
      })}
    </ul>
  )
}
