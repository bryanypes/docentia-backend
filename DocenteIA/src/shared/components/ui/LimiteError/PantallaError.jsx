import { House, RefreshCw } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import robotError from '../../../../assets/ilustraciones/robot-error.svg'
import './PantallaError.css'

const SEGUNDOS = 8
// Reintentos automáticos seguidos antes de dejar la decisión al usuario, para no entrar en un ciclo.
const MAXIMO_AUTOMATICOS = 2
const VENTANA_MS = 2 * 60 * 1000
const CLAVE = 'docentia.reintentos-error'

function leerReintentos() {
  try {
    const guardado = JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null')
    if (guardado && Date.now() - guardado.desde < VENTANA_MS) return guardado
  } catch {
    // Sin almacenamiento disponible: se cuenta desde cero.
  }
  return { cantidad: 0, desde: Date.now() }
}

function guardarReintentos(valor) {
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify(valor))
  } catch {
    // Sin almacenamiento: el contador vive solo en esta pantalla.
  }
}

/**
 * Pantalla de error con DocenteIA: se reintenta sola unos segundos después (hasta dos veces seguidas)
 * y siempre deja recargar a mano o volver al inicio.
 */
export function PantallaError({ enLinea = false, onReintentar }) {
  const [reintentos] = useState(leerReintentos)
  const automatico = reintentos.cantidad < MAXIMO_AUTOMATICOS
  const [restantes, setRestantes] = useState(SEGUNDOS)
  const reintentar = useRef(onReintentar)
  const boton = useRef(null)

  useEffect(() => {
    reintentar.current = onReintentar
  }, [onReintentar])

  useEffect(() => {
    boton.current?.focus()
  }, [])

  useEffect(() => {
    if (!automatico) return undefined
    const temporizador = setInterval(() => setRestantes((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(temporizador)
  }, [automatico])

  useEffect(() => {
    if (automatico && restantes === 0) {
      guardarReintentos({ cantidad: reintentos.cantidad + 1, desde: reintentos.desde })
      reintentar.current()
    }
  }, [automatico, restantes, reintentos])

  function recargarAhora() {
    guardarReintentos({ cantidad: 0, desde: Date.now() })
    onReintentar()
  }

  const Envoltura = enLinea ? 'div' : 'main'
  return (
    <Envoltura className={`pantalla-error${enLinea ? ' pantalla-error--en-linea' : ''}`}>
      <div className="pantalla-error__tarjeta" role="alert" aria-live="assertive">
        <img className="pantalla-error__robot" src={robotError} alt="" width="150" height="147" />
        <h1 className="pantalla-error__titulo">Algo salió mal</h1>
        <p className="pantalla-error__texto">
          {enLinea ? 'Esta pantalla no se pudo mostrar.' : 'La aplicación no se pudo mostrar.'}{' '}
          {automatico
            ? 'DocenteIA lo está intentando de nuevo por ti.'
            : 'Ya lo intentamos varias veces; recarga cuando quieras.'}
        </p>

        {automatico && (
          <div className="pantalla-error__cuenta" aria-live="polite">
            <span
              className="pantalla-error__anillo"
              style={{ '--avance': `${((SEGUNDOS - restantes) / SEGUNDOS) * 360}deg` }}
              aria-hidden="true"
            >
              <span>{restantes}</span>
            </span>
            <span>
              Reintentando en {restantes} {restantes === 1 ? 'segundo' : 'segundos'}
            </span>
          </div>
        )}

        <div className="pantalla-error__acciones">
          <button ref={boton} type="button" className="boton boton--primario" onClick={recargarAhora}>
            <RefreshCw size={16} aria-hidden="true" />
            Recargar ahora
          </button>
          <a href="/inicio" className="boton boton--contorno">
            <House size={16} aria-hidden="true" />
            Ir al inicio
          </a>
        </div>
      </div>
    </Envoltura>
  )
}
