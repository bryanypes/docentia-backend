import { X } from 'lucide-react'
import { useEffect, useId, useRef } from 'react'
import './Modal.css'

/**
 * Ventana modal sobre la página, con <dialog> nativo: el navegador atrapa el foco, bloquea el fondo
 * y devuelve el foco a quien la abrió al cerrarse. Esc, la X y el clic fuera llaman a `onCerrar`;
 * quien la usa decide si cierra de verdad (por ejemplo, para confirmar cambios sin guardar).
 */
export function Modal({ titulo, descripcion, icono: Icono, onCerrar, ancho = 'mediano', children }) {
  const dialogo = useRef(null)
  const idTitulo = useId()
  const idDescripcion = useId()
  // La última versión de onCerrar, sin volver a registrar los eventos en cada render.
  const cerrar = useRef(onCerrar)
  useEffect(() => {
    cerrar.current = onCerrar
  })

  useEffect(() => {
    const elemento = dialogo.current
    // El diálogo se desmonta (no solo se cierra), así que el foco se devuelve a mano a quien lo abrió.
    const abridor = document.activeElement
    elemento.showModal()
    // Esc dispara "cancel": se intercepta para que decida onCerrar.
    const alCancelar = (evento) => {
      evento.preventDefault()
      cerrar.current?.()
    }
    elemento.addEventListener('cancel', alCancelar)
    return () => {
      elemento.removeEventListener('cancel', alCancelar)
      if (elemento.open) elemento.close()
      if (abridor instanceof HTMLElement && abridor.isConnected) abridor.focus()
    }
  }, [])

  // Clic en el fondo (el propio <dialog>, fuera de la caja) equivale a cerrar.
  function alHacerClic(evento) {
    if (evento.target === dialogo.current) cerrar.current?.()
  }

  return (
    <dialog
      ref={dialogo}
      className={`modal modal--${ancho}`}
      aria-labelledby={idTitulo}
      aria-describedby={descripcion ? idDescripcion : undefined}
      onClick={alHacerClic}
    >
      <div className="modal__caja">
        <header className="modal__cabecera">
          {Icono && (
            <span className="modal__icono" aria-hidden="true">
              <Icono size={20} />
            </span>
          )}
          <div className="modal__textos">
            <h2 id={idTitulo} className="modal__titulo">
              {titulo}
            </h2>
            {descripcion && (
              <p id={idDescripcion} className="modal__descripcion">
                {descripcion}
              </p>
            )}
          </div>
          <button type="button" className="modal__cerrar" onClick={() => cerrar.current?.()}>
            <X size={20} aria-hidden="true" />
            <span className="solo-lectores">Cerrar</span>
          </button>
        </header>
        <div className="modal__cuerpo">{children}</div>
      </div>
    </dialog>
  )
}
