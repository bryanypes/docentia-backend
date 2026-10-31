import { CircleCheck, CircleX, Info, TriangleAlert } from 'lucide-react'
import './Aviso.css'

const ROL_ACCESIBLE = { error: 'alert', alerta: 'status', exito: 'status', info: undefined }

// La forma del icono distingue el tipo además del color (WCAG 1.4.1).
const ICONO = { error: CircleX, alerta: TriangleAlert, exito: CircleCheck, info: Info }

/** Caja de mensaje con los colores de estado del maquetado. tipo: 'alerta' | 'error' | 'exito' | 'info' */
export function Aviso({ tipo = 'info', titulo, children, className = '' }) {
  const Icono = ICONO[tipo]
  return (
    <div className={`aviso aviso--${tipo} ${className}`} role={ROL_ACCESIBLE[tipo]}>
      <Icono size={18} aria-hidden="true" className="aviso__icono" />
      <div className="aviso__contenido">
        {titulo && <strong className="aviso__titulo">{titulo}</strong>}
        {children && <div className="aviso__texto">{children}</div>}
      </div>
    </div>
  )
}
