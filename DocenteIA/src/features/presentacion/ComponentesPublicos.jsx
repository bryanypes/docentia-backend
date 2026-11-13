import { ExternalLink } from 'lucide-react'
import { SERVICIOS_NOVA, URL_NOVA } from '../../shared/constants/ayuda.js'

/** Enlace a otro sitio: abre en otra pestaña y lo anuncia a los lectores de pantalla. */
export function EnlaceExterno({ href, className, children }) {
  return (
    <a href={href} className={className} target="_blank" rel="noopener noreferrer">
      {children}
      <ExternalLink size={14} strokeWidth={2.25} aria-hidden="true" />
      <span className="solo-lectores">(se abre en otra pestaña)</span>
    </a>
  )
}

/** Pie de las páginas públicas. */
export function PiePublico() {
  return (
    <footer className="presentacion__pie">
      <span>
        <strong>DOCENTIA</strong> · Evaluación Docente · Universidad Autónoma del Cauca
      </span>
      <span className="presentacion__pie-enlaces">
        <EnlaceExterno href={SERVICIOS_NOVA.POLITICA_DATOS} className="presentacion__pie-enlace">
          Política de tratamiento de datos
        </EnlaceExterno>
        <EnlaceExterno href={URL_NOVA} className="presentacion__pie-enlace">
          Portal NOVA
        </EnlaceExterno>
      </span>
    </footer>
  )
}
