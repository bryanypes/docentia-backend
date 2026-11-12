import './Boton.css'

/**
 * Botón del maquetado. variante: 'primario' | 'secundario' | 'contorno' | 'enlace'.
 * `icono` es un componente de lucide-react; va antes del texto y es decorativo (el texto lo nombra).
 */
export function Boton({ variante = 'primario', bloque = false, icono: Icono, className = '', type = 'button', children, ...props }) {
  const clases = ['boton', `boton--${variante}`, bloque && 'boton--bloque', className]
    .filter(Boolean)
    .join(' ')
  return (
    <button type={type} className={clases} {...props}>
      {Icono && <Icono size={16} strokeWidth={2.25} aria-hidden="true" />}
      {children}
    </button>
  )
}
