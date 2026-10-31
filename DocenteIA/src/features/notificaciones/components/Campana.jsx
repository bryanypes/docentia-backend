import { Bell } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import {
  contarNoLeidas,
  listarNotificaciones,
  marcarLeida,
} from '../services/notificacionesService.js'
import './Notificaciones.css'

const INTERVALO_MS = 60000
const ULTIMAS = 8

/** Campana de la cabecera. Consulta el conteo cada minuto solo con la pestaña visible. */
export function Campana() {
  const navegar = useNavigate()
  const [conteo, setConteo] = useState(0)
  const [abierta, setAbierta] = useState(false)
  const [lista, setLista] = useState(null)
  const [error, setError] = useState(false)
  const raiz = useRef(null)

  const actualizar = useCallback(() => {
    if (document.visibilityState !== 'visible') return
    contarNoLeidas()
      .then((datos) => setConteo(datos.noLeidas))
      .catch(() => {})
  }, [])

  useEffect(() => {
    actualizar()
    const temporizador = setInterval(actualizar, INTERVALO_MS)
    document.addEventListener('visibilitychange', actualizar)
    window.addEventListener('docentia:notificaciones', actualizar)
    return () => {
      clearInterval(temporizador)
      document.removeEventListener('visibilitychange', actualizar)
      window.removeEventListener('docentia:notificaciones', actualizar)
    }
  }, [actualizar])

  useEffect(() => {
    if (!abierta) return undefined
    function alPulsar(evento) {
      if (!raiz.current?.contains(evento.target)) setAbierta(false)
    }
    function alTeclear(evento) {
      if (evento.key === 'Escape') setAbierta(false)
    }
    document.addEventListener('mousedown', alPulsar)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alPulsar)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [abierta])

  async function alternar() {
    if (abierta) {
      setAbierta(false)
      return
    }
    setAbierta(true)
    setError(false)
    setLista(null)
    try {
      setLista((await listarNotificaciones({ tamano: ULTIMAS })).contenido)
    } catch {
      setError(true)
    }
  }

  async function abrir(notificacion) {
    setAbierta(false)
    try {
      if (!notificacion.leida) await marcarLeida(notificacion.id)
    } finally {
      actualizar()
    }
    if (notificacion.enlace) navegar(notificacion.enlace)
  }

  return (
    <div className="campana" ref={raiz}>
      <button
        type="button"
        className="campana__boton"
        aria-expanded={abierta}
        aria-haspopup="true"
        aria-label={conteo > 0 ? `Notificaciones: ${conteo} sin leer` : 'Notificaciones'}
        onClick={alternar}
      >
        <Bell size={18} aria-hidden="true" />
        {conteo > 0 && <span className="campana__contador">{conteo > 99 ? '99+' : conteo}</span>}
      </button>
      <span className="solo-lectores" aria-live="polite">
        {conteo > 0 ? `${conteo} notificaciones sin leer` : ''}
      </span>
      {abierta && (
        <div className="campana__menu">
          <strong className="campana__titulo">Notificaciones</strong>
          {error && <p className="campo__ayuda">No fue posible cargarlas.</p>}
          {!lista && !error && <p className="campo__ayuda">Cargando…</p>}
          {lista?.length === 0 && <p className="campo__ayuda">No tienes notificaciones.</p>}
          {lista?.length > 0 && (
            <ul className="campana__lista">
              {lista.map((n) => (
                <li key={n.id}>
                  <button type="button" className="campana__item" onClick={() => abrir(n)}>
                    <strong>
                      {!n.leida && <span className="pildora pildora--info">Nueva</span>} {n.titulo}
                    </strong>
                    <span>{n.mensaje}</span>
                    <small>{formatearFechaHora(n.creadaEn)}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <Link to="/notificaciones" onClick={() => setAbierta(false)}>
            Ver todas
          </Link>
        </div>
      )}
    </div>
  )
}
