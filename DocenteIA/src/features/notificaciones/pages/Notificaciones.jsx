import { Bell, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import {
  guardarPreferencias,
  listarNotificaciones,
  marcarLeida,
  marcarTodasLeidas,
  obtenerPreferencias,
} from '../services/notificacionesService.js'
import '../components/Notificaciones.css'

function Preferencias() {
  const preferencias = useFetch(obtenerPreferencias)
  const [error, setError] = useState(null)
  const [guardando, setGuardando] = useState(false)

  async function cambiar(evento) {
    const valor = evento.target.checked
    setGuardando(true)
    setError(null)
    try {
      await guardarPreferencias(valor)
      preferencias.recargar()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div>
      <label htmlFor="correoHabilitado">
        <input
          id="correoHabilitado"
          type="checkbox"
          disabled={!preferencias.datos || guardando}
          checked={preferencias.datos?.correoHabilitado ?? false}
          onChange={cambiar}
        />{' '}
        Recibir avisos por correo
      </label>
      <ErrorApi error={error ?? preferencias.error} />
    </div>
  )
}

export default function Notificaciones() {
  const navegar = useNavigate()
  const [soloNoLeidas, setSoloNoLeidas] = useState(false)
  const [pagina, setPagina] = useState(0)
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const cargar = useCallback(() => listarNotificaciones({ soloNoLeidas, pagina }), [soloNoLeidas, pagina])
  const lista = useFetch(cargar, [cargar])

  async function abrir(notificacion) {
    setError(null)
    try {
      if (!notificacion.leida) await marcarLeida(notificacion.id)
      if (notificacion.enlace) navegar(notificacion.enlace)
      else lista.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  async function leerTodas() {
    setOcupado(true)
    setError(null)
    try {
      await marcarTodasLeidas()
      lista.recargar()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setOcupado(false)
    }
  }

  const datos = lista.datos

  return (
    <>
      <Encabezado miga="Mi cuenta · Notificaciones" titulo="Notificaciones" subtitulo="Avisos de tu evaluación y de tus tareas pendientes." />
      <div className="pagina">
        <Panel titulo="Preferencias">
          <Preferencias />
        </Panel>
        <Panel titulo="Bandeja" icono={Bell}>
          <div className="notificaciones__barra">
            <label htmlFor="soloNoLeidas">
              <input
                id="soloNoLeidas"
                type="checkbox"
                checked={soloNoLeidas}
                onChange={(e) => {
                  setSoloNoLeidas(e.target.checked)
                  setPagina(0)
                }}
              />{' '}
              Solo no leídas
            </label>
            <Boton variante="contorno" disabled={ocupado} onClick={leerTodas}>
              Marcar todas como leídas
            </Boton>
          </div>
          <ErrorApi error={error} />
          {lista.cargando && !datos && <Cargando texto="Cargando notificaciones" bloques={1} />}
          {lista.error && (
            <EstadoVista tipo="error" titulo="No fue posible cargar las notificaciones" mensaje={lista.error.message}>
              <Boton icono={RefreshCw} onClick={lista.recargar}>Intentar de nuevo</Boton>
            </EstadoVista>
          )}
          {datos?.contenido.length === 0 && <EstadoVista titulo="No hay notificaciones" mensaje="Aquí verás tus avisos." />}
          {datos?.contenido.length > 0 && (
            <ul className="notificaciones__lista">
              {datos.contenido.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={`campana__item${n.leida ? ' campana__item--leida' : ''}`}
                    onClick={() => abrir(n)}
                  >
                    <strong>
                      <span className={`pildora pildora--${n.leida ? 'neutro' : 'info'}`}>
                        {n.leida ? 'Leída' : 'Nueva'}
                      </span>{' '}
                      {n.titulo}
                    </strong>
                    <span>{n.mensaje}</span>
                    <small>{formatearFechaHora(n.creadaEn)}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {datos && datos.totalPaginas > 1 && (
            <div className="notificaciones__paginas">
              <Boton variante="contorno" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                Anterior
              </Boton>
              <span>
                Página {pagina + 1} de {datos.totalPaginas}
              </span>
              <Boton
                variante="contorno"
                disabled={pagina + 1 >= datos.totalPaginas}
                onClick={() => setPagina((p) => p + 1)}
              >
                Siguiente
              </Boton>
            </div>
          )}
        </Panel>
      </div>
    </>
  )
}
