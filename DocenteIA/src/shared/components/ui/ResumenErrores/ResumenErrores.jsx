import { CircleAlert } from 'lucide-react'
import './ResumenErrores.css'

/**
 * Resumen de errores de un formulario al intentar enviarlo. Cada error enlaza a su campo.
 * El formulario le pasa `ref` y lo enfoca para que lectores de pantalla y teclado lo encuentren.
 * errores: [{ id, mensaje }], donde id es el del control que debe recibir el foco.
 */
export function ResumenErrores({ errores, ref }) {
  if (errores.length === 0) return null

  function irAlCampo(evento, id) {
    evento.preventDefault()
    const control = document.getElementById(id)
    control?.scrollIntoView({ block: 'center' })
    control?.focus({ preventScroll: true })
  }

  return (
    <div ref={ref} className="resumen-errores" tabIndex={-1} role="alert" aria-labelledby="resumen-errores-titulo">
      <strong id="resumen-errores-titulo" className="resumen-errores__titulo">
        <CircleAlert size={18} aria-hidden="true" />
        {errores.length === 1 ? 'Revisa 1 campo antes de guardar' : `Revisa ${errores.length} campos antes de guardar`}
      </strong>
      <ul className="resumen-errores__lista">
        {errores.map(({ id, mensaje }) => (
          <li key={id}>
            <a href={`#${id}`} onClick={(e) => irAlCampo(e, id)}>
              {mensaje}
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}
