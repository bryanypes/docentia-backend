import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Aviso } from '../Aviso/Aviso.jsx'
import './MensajeNavegacion.css'

/**
 * Mensaje de éxito que otra pantalla deja en `state.mensaje` al navegar aquí (por ejemplo, "Se creó
 * la cuenta…"). Se copia al montar y se borra del historial para que no reaparezca al recargar o
 * al volver atrás. Se puede cerrar.
 */
export function MensajeNavegacion() {
  const navegar = useNavigate()
  const { state, pathname } = useLocation()
  const entrante = state?.mensaje ?? null
  const [mensaje, setMensaje] = useState(entrante)
  // La página puede seguir montada cuando llega un mensaje nuevo (por ejemplo, al cerrar un modal):
  // se toma durante el render, comparando con el último que llegó.
  const [ultimoEntrante, setUltimoEntrante] = useState(entrante)
  if (entrante !== ultimoEntrante) {
    setUltimoEntrante(entrante)
    if (entrante) setMensaje(entrante)
  }

  useEffect(() => {
    if (entrante) navegar(pathname, { replace: true, state: { conservarFoco: true } })
  }, [entrante, pathname, navegar])

  if (!mensaje) return null

  return (
    <div className="mensaje-navegacion">
      <Aviso tipo="exito" titulo={mensaje} />
      <button type="button" className="mensaje-navegacion__cerrar" onClick={() => setMensaje(null)}>
        <X size={16} aria-hidden="true" />
        <span className="solo-lectores">Cerrar mensaje</span>
      </button>
    </div>
  )
}
