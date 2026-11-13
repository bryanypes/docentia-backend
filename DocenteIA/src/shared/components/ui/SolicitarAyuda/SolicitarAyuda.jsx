import { ExternalLink, LifeBuoy } from 'lucide-react'
import { TICKETS_NOVA, URL_MESA_AYUDA } from '../../../constants/ayuda.js'
import './SolicitarAyuda.css'

/**
 * Invitación a abrir un ticket en la mesa de ayuda. El enlace abre NOVA en otra pestaña para no
 * perder lo que la persona tenía en DOCENTIA.
 *
 * @param titulo pregunta o frase que abre la tarjeta
 * @param compacto solo el enlace, sin tarjeta
 * @param variante 'boton' (por defecto) o 'enlace', para un enlace de texto dentro de una frase
 * @param perfil clave de TICKETS_NOVA: lleva a los tickets de ese perfil (por defecto, el general)
 */
export function SolicitarAyuda({
  titulo = '¿Tienes problemas para ingresar a DOCENTIA?',
  children,
  compacto = false,
  variante = 'boton',
  perfil = 'GENERAL',
}) {
  const clase = variante === 'enlace' ? 'solicitar-ayuda__enlace' : 'boton boton--primario solicitar-ayuda__boton'
  const enlace = (
    <a className={clase} href={TICKETS_NOVA[perfil] ?? URL_MESA_AYUDA} target="_blank" rel="noopener noreferrer">
      <LifeBuoy size={16} strokeWidth={2.25} aria-hidden="true" />
      Solicitar ayuda
      <ExternalLink size={14} strokeWidth={2.25} aria-hidden="true" />
      <span className="solo-lectores">(se abre en otra pestaña)</span>
    </a>
  )
  if (compacto) return enlace
  return (
    <aside className="solicitar-ayuda" aria-label="Solicitar ayuda">
      <strong className="solicitar-ayuda__titulo">{titulo}</strong>
      <p className="solicitar-ayuda__texto">
        {children ?? 'Abre una solicitud en la mesa de ayuda de la Corporación y el equipo de soporte te ayudará a recuperar el acceso.'}
      </p>
      {enlace}
    </aside>
  )
}
