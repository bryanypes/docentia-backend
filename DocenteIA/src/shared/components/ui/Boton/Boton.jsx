import './Boton.css'

/**
 * Botón del maquetado. variante: 'primario' | 'secundario' | 'contorno' | 'peligro' | 'enlace'.
 * `icono` es un componente de lucide-react; va antes del texto y es decorativo (el texto lo nombra).
 * Sin texto queda cuadrado y debe llevar aria-label.
 */
export function Boton({ variante = 'primario', bloque = false, icono: Icono, className = '', type = 'button', children, ...props }) {
  const soloIcono = Icono && (children === undefined || children === null || children === false)
  const clases = ['boton', `boton--${variante}`, bloque && 'boton--bloque', soloIcono && 'boton--icono', className]
    .filter(Boolean)
    .join(' ')
  return (
    <button type={type} className={clases} {...props}>
      {Icono && <Icono size={16} strokeWidth={2.25} aria-hidden="true" />}
      {children}
    </button>
  )
}
