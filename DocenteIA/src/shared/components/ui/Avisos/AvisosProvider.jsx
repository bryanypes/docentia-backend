import { CircleCheck, CircleX, Info, TriangleAlert, X } from 'lucide-react'
import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { AvisosContext } from './avisosContext.js'
import './Avisos.css'

const ICONO = { exito: CircleCheck, error: CircleX, alerta: TriangleAlert, info: Info, peligro: TriangleAlert }
const DURACION_MS = 5000

/** Ventana de confirmación con el ícono centrado; Esc y Cancelar responden que no. */
function Confirmacion({ pedido, onResponder }) {
  const dialogo = useRef(null)
  const idTitulo = useId()
  const idMensaje = useId()
  const Icono = ICONO[pedido.tipo] ?? Info

  useEffect(() => {
    const elemento = dialogo.current
    const abridor = document.activeElement
    elemento.showModal?.()
    const alCancelar = (evento) => {
      evento.preventDefault()
      onResponder(false)
    }
    elemento.addEventListener('cancel', alCancelar)
    return () => {
      elemento.removeEventListener('cancel', alCancelar)
      if (elemento.open) elemento.close?.()
      if (abridor instanceof HTMLElement && abridor.isConnected) abridor.focus()
    }
  }, [onResponder])

  return (
    <dialog
      ref={dialogo}
      className={`confirmacion confirmacion--${pedido.tipo}`}
      aria-labelledby={idTitulo}
      aria-describedby={idMensaje}
      onClick={(e) => e.target === dialogo.current && onResponder(false)}
    >
      <div className="confirmacion__caja">
        <span className="confirmacion__icono" aria-hidden="true">
          <Icono size={30} />
        </span>
        <h2 id={idTitulo} className="confirmacion__titulo">
          {pedido.titulo}
        </h2>
        <p id={idMensaje} className="confirmacion__mensaje">
          {pedido.mensaje}
        </p>
        <div className="confirmacion__acciones">
          <button type="button" className="boton boton--secundario" onClick={() => onResponder(false)}>
            {pedido.textoCancelar ?? 'Cancelar'}
          </button>
          <button
            type="button"
            className={`boton ${pedido.tipo === 'peligro' ? 'boton--peligro-lleno' : 'boton--primario'}`}
            // eslint-disable-next-line jsx-a11y/no-autofocus -- la acción principal del diálogo
            autoFocus
            onClick={() => onResponder(true)}
          >
            {pedido.textoConfirmar ?? 'Confirmar'}
          </button>
        </div>
      </div>
    </dialog>
  )
}

function Notificacion({ aviso, onCerrar }) {
  const Icono = ICONO[aviso.tipo] ?? Info
  useEffect(() => {
    if (aviso.tipo === 'error') return undefined
    const temporizador = setTimeout(() => onCerrar(aviso.id), DURACION_MS)
    return () => clearTimeout(temporizador)
  }, [aviso, onCerrar])
  return (
    <li className={`notificacion-flotante notificacion-flotante--${aviso.tipo}`} role={aviso.tipo === 'error' ? 'alert' : 'status'}>
      <span className="notificacion-flotante__icono" aria-hidden="true">
        <Icono size={20} />
      </span>
      <div className="notificacion-flotante__texto">
        <strong>{aviso.titulo}</strong>
        {aviso.mensaje && <div className="notificacion-flotante__mensaje">{aviso.mensaje}</div>}
      </div>
      <button type="button" className="notificacion-flotante__cerrar" onClick={() => onCerrar(aviso.id)}>
        <X size={16} aria-hidden="true" />
        <span className="solo-lectores">Cerrar aviso</span>
      </button>
    </li>
  )
}

/**
 * Avisos de la plataforma: notificaciones flotantes que no tapan el contenido y confirmaciones en un
 * modal con el ícono centrado. `confirmar` devuelve una promesa con la respuesta.
 */
export function AvisosProvider({ children }) {
  const [avisos, setAvisos] = useState([])
  const [pedido, setPedido] = useState(null)
  const siguiente = useRef(1)

  const cerrar = useCallback((id) => setAvisos((actuales) => actuales.filter((a) => a.id !== id)), [])

  const notificar = useCallback((aviso) => {
    const id = siguiente.current++
    setAvisos((actuales) => [...actuales.slice(-3), { tipo: 'exito', ...aviso, id }])
  }, [])

  const confirmar = useCallback(
    (opciones) => new Promise((resolver) => setPedido({ tipo: 'info', ...opciones, resolver })),
    [],
  )

  const responder = useCallback(
    (respuesta) => {
      setPedido((actual) => {
        actual?.resolver(respuesta)
        return null
      })
    },
    [],
  )

  const valor = useMemo(() => ({ notificar, confirmar }), [notificar, confirmar])

  return (
    <AvisosContext.Provider value={valor}>
      {children}
      {createPortal(
        <ol className="notificaciones-flotantes" aria-label="Avisos">
          {avisos.map((aviso) => (
            <Notificacion key={aviso.id} aviso={aviso} onCerrar={cerrar} />
          ))}
        </ol>,
        document.body,
      )}
      {pedido && <Confirmacion pedido={pedido} onResponder={responder} />}
    </AvisosContext.Provider>
  )
}
