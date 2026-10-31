import { Info } from 'lucide-react'
import { useId } from 'react'
import './Ayuda.css'

/**
 * Ícono ⓘ que muestra una explicación corta al pasar el mouse o al enfocarlo con el teclado.
 * Saca de la vista el texto secundario sin perderlo: los lectores de pantalla lo leen igual.
 */
export function Ayuda({ texto, etiqueta = 'Más información' }) {
  const id = useId()
  return (
    <span className="ayuda">
      <button type="button" className="ayuda__boton" aria-describedby={id}>
        <Info size={15} aria-hidden="true" />
        <span className="solo-lectores">{etiqueta}</span>
      </button>
      <span id={id} role="tooltip" className="ayuda__texto">
        {texto}
      </span>
    </span>
  )
}
