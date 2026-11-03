import { Eye, Pencil, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import './BotonAccion.css'

const TIPOS = {
  ver: { Icono: Eye, texto: 'Ver información', clase: 'ver' },
  editar: { Icono: Pencil, texto: 'Editar', clase: 'editar' },
  eliminar: { Icono: Trash2, texto: 'Eliminar', clase: 'eliminar' },
}

/**
 * Acción de fila con el mismo aspecto en toda la plataforma: ojo para ver, lápiz para editar y
 * basura para eliminar. `nombre` completa la etiqueta accesible ("Eliminar Ingeniería").
 * Con `a` se dibuja como enlace.
 */
export function BotonAccion({ tipo, nombre, texto, a, state, className = '', ...props }) {
  const { Icono, texto: porDefecto, clase } = TIPOS[tipo]
  const visible = texto ?? porDefecto
  const etiqueta = nombre ? `${visible} · ${nombre}` : undefined
  const clases = `boton-accion boton-accion--${clase} ${className}`
  const contenido = (
    <>
      <Icono size={15} strokeWidth={2.25} aria-hidden="true" />
      <span className="boton-accion__texto">{visible}</span>
    </>
  )
  if (a) {
    return (
      <Link to={a} state={state} className={clases} aria-label={etiqueta} title={etiqueta ?? visible} {...props}>
        {contenido}
      </Link>
    )
  }
  return (
    <button type="button" className={clases} aria-label={etiqueta} title={etiqueta ?? visible} {...props}>
      {contenido}
    </button>
  )
}
