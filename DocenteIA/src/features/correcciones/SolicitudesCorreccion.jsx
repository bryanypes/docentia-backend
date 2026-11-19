import { Check, Inbox, LifeBuoy, RefreshCw, RotateCcw, X } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Encabezado } from '../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL, ROLES } from '../../shared/constants/dominio.js'
import { useFetch } from '../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../shared/utils/formatters.js'
import { useSesion } from '../auth/sesionContext.js'
import {
  atenderSolicitud,
  listarMisSolicitudes,
  listarPorVerificar,
  listarTodasLasSolicitudes,
  verificarSolicitud,
} from './correccionesService.js'
import { ESTADO_SOLICITUD, ETIQUETA_APROBADOR, ETIQUETA_TIPO, RESPUESTA_MINIMA, quienLaTiene } from './estados.js'
import './Correcciones.css'

const FILTROS_ADMIN = [
  { clave: 'APROBADA', etiqueta: 'Por atender' },
  { clave: 'PENDIENTE_APROBACION', etiqueta: 'Esperando verificación' },
  { clave: 'CERRADAS', etiqueta: 'Cerradas' },
]

/**
 * Solicitudes para corregir una evaluación ya enviada. Cada rol ve su parte del recorrido: quien la
 * pide sigue las suyas, el superior las verifica y la administración reabre el formulario.
 */
export default function SolicitudesCorreccion() {
  const { usuario } = useSesion()
  const esAdministrador = usuario.rol === ROLES.ADMINISTRADOR
  const verifica = usuario.rol === ROLES.COORDINADOR || usuario.rol === ROLES.VICERRECTOR

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Solicitudes de corrección`}
        titulo="Solicitudes de corrección"
        subtitulo="Para corregir una autoevaluación o coevaluación ya enviada: quien la envió la pide, su superior la verifica y la administración reabre el formulario."
      />
      <div className="pagina correcciones">
        {esAdministrador && <ListaAdministracion />}
        {verifica && <ListaPorVerificar rol={usuario.rol} />}
        {!esAdministrador && <ListaPropias />}
      </div>
    </>
  )
}

function ListaPorVerificar({ rol }) {
  const cargar = useCallback(() => listarPorVerificar(), [])
  const lista = useFetch(cargar, [cargar])
  const pendientes = (lista.datos ?? []).filter((s) => s.estado === 'PENDIENTE_APROBACION')
  const resueltas = (lista.datos ?? []).filter((s) => s.estado !== 'PENDIENTE_APROBACION')

  return (
    <Panel
      titulo="Por verificar"
      descripcion={
        rol === ROLES.COORDINADOR
          ? 'Autoevaluaciones de docentes de tu facultad que piden corregirse.'
          : 'Autoevaluaciones de coordinadores y coevaluaciones hechas por la coordinación.'
      }
      icono={Inbox}
      tono="violeta"
    >
      <Estado consulta={lista} vacio="No tienes solicitudes por verificar." hayDatos={pendientes.length > 0} />
      <ul className="correcciones__lista">
        {pendientes.map((s) => (
          <Tarjeta key={s.idSolicitud} solicitud={s} modo="verificar" onCambio={lista.recargar} />
        ))}
      </ul>
      {resueltas.length > 0 && (
        <details className="correcciones__historial">
          <summary>Ya verificadas ({resueltas.length})</summary>
          <ul className="correcciones__lista">
            {resueltas.map((s) => (
              <Tarjeta key={s.idSolicitud} solicitud={s} />
            ))}
          </ul>
        </details>
      )}
    </Panel>
  )
}

function ListaAdministracion() {
  const cargar = useCallback(() => listarTodasLasSolicitudes(), [])
  const lista = useFetch(cargar, [cargar])
  const [filtro, setFiltro] = useState('APROBADA')
  const todas = lista.datos ?? []
  const contar = (clave) =>
    todas.filter((s) => (clave === 'CERRADAS' ? ['RECHAZADA', 'ATENDIDA'].includes(s.estado) : s.estado === clave))
      .length
  const visibles = todas.filter((s) =>
    filtro === 'CERRADAS' ? ['RECHAZADA', 'ATENDIDA'].includes(s.estado) : s.estado === filtro,
  )

  return (
    <Panel
      titulo="Solicitudes de la plataforma"
      descripcion="Las aprobadas por el superior esperan que reabras el formulario. No ves notas ni respuestas, solo el motivo."
      icono={Inbox}
      tono="violeta"
    >
      <div className="correcciones__filtros" role="group" aria-label="Filtrar solicitudes">
        {FILTROS_ADMIN.map((f) => (
          <button
            key={f.clave}
            type="button"
            className="correcciones__filtro"
            aria-pressed={filtro === f.clave}
            onClick={() => setFiltro(f.clave)}
          >
            {f.etiqueta} <strong>{contar(f.clave)}</strong>
          </button>
        ))}
      </div>
      <Estado consulta={lista} vacio="No hay solicitudes en este estado." hayDatos={visibles.length > 0} />
      <ul className="correcciones__lista">
        {visibles.map((s) => (
          <Tarjeta key={s.idSolicitud} solicitud={s} modo={s.estado === 'APROBADA' ? 'atender' : null} onCambio={lista.recargar} />
        ))}
      </ul>
    </Panel>
  )
}

function ListaPropias() {
  const cargar = useCallback(() => listarMisSolicitudes(), [])
  const lista = useFetch(cargar, [cargar])
  return (
    <Panel
      titulo="Mis solicitudes"
      descripcion="Las pides desde la evaluación ya enviada, con el botón «Necesito corregir esta evaluación»."
      icono={LifeBuoy}
    >
      <Estado consulta={lista} vacio="No has pedido corregir ninguna evaluación." hayDatos={(lista.datos ?? []).length > 0} />
      <ul className="correcciones__lista">
        {(lista.datos ?? []).map((s) => (
          <Tarjeta key={s.idSolicitud} solicitud={s} propia />
        ))}
      </ul>
    </Panel>
  )
}

function Estado({ consulta, vacio, hayDatos }) {
  if (consulta.cargando && !consulta.datos) return <Cargando texto="Cargando las solicitudes" bloques={1} />
  if (consulta.error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar las solicitudes" mensaje={consulta.error.message}>
        <Boton icono={RefreshCw} onClick={consulta.recargar}>
          Intentar de nuevo
        </Boton>
      </EstadoVista>
    )
  }
  if (consulta.datos && !hayDatos) return <p className="correcciones__vacio">{vacio}</p>
  return null
}

/** Una solicitud con su recorrido; con `modo` muestra las acciones de quien la decide. */
function Tarjeta({ solicitud: s, modo = null, propia = false, onCambio }) {
  const estado = ESTADO_SOLICITUD[s.estado]
  return (
    <li className="correccion">
      <div className="correccion__cabecera">
        <strong>
          {ETIQUETA_TIPO[s.tipo]} · {s.nombreDocente}
        </strong>
        <span className={`pildora pildora--${estado.clase}`}>{estado.etiqueta}</span>
      </div>
      <p className="correccion__meta">
        Periodo {s.periodo}
        {s.nombreFacultad ? ` · ${s.nombreFacultad}` : ''} · pedida por {propia ? 'ti' : s.nombreSolicitante} el{' '}
        {formatearFechaHora(s.fechaSolicitud)}
        {s.rolAprobador ? ` · la verifica ${ETIQUETA_APROBADOR[s.rolAprobador]}` : ' · sin superior: va directo a administración'}
      </p>
      <blockquote className="correccion__motivo">{s.motivo}</blockquote>
      <ol className="correccion__recorrido">
        {s.nombreAprobador && (
          <li>
            {s.estado === 'RECHAZADA' && !s.nombreAdministrador ? 'Rechazó' : 'Aprobó'} {s.nombreAprobador} el{' '}
            {formatearFechaHora(s.fechaAprobacion)}
            {s.respuestaAprobador && <span>: «{s.respuestaAprobador}»</span>}
          </li>
        )}
        {s.nombreAdministrador && (
          <li>
            {s.estado === 'ATENDIDA' ? 'Reabrió el formulario' : 'Rechazó'} {s.nombreAdministrador} el{' '}
            {formatearFechaHora(s.fechaAtencion)}
            {s.respuestaAdministrador && <span>: «{s.respuestaAdministrador}»</span>}
          </li>
        )}
      </ol>
      {propia && <p className="correccion__ahora">{quienLaTiene(s)}</p>}
      {modo && <Decision solicitud={s} modo={modo} onCambio={onCambio} />}
    </li>
  )
}

function Decision({ solicitud, modo, onCambio }) {
  const [respuesta, setRespuesta] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState(null)
  const [hecho, setHecho] = useState(null)
  const accion = modo === 'verificar' ? verificarSolicitud : atenderSolicitud
  const puedeRechazar = respuesta.trim().length >= RESPUESTA_MINIMA
  const id = `respuesta-${solicitud.idSolicitud}`

  async function decidir(aprobada) {
    setOcupado(true)
    setError(null)
    try {
      await accion(solicitud.idSolicitud, aprobada, respuesta.trim())
      setHecho(
        aprobada
          ? modo === 'verificar'
            ? 'Aprobada. La administración la recibió para reabrir el formulario.'
            : 'Formulario reabierto. Avisamos al solicitante.'
          : 'Solicitud rechazada. Avisamos al solicitante.',
      )
      onCambio?.()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setOcupado(false)
    }
  }

  if (hecho) return <Aviso tipo="exito" titulo={hecho} />

  return (
    <div className="correccion__decision">
      <label htmlFor={id} className="rotulo rotulo--campo">
        Respuesta para el solicitante <span className="campo__ayuda">(obligatoria si rechazas)</span>
      </label>
      <textarea
        id={id}
        className="campo__control"
        rows={2}
        maxLength={500}
        value={respuesta}
        onChange={(e) => setRespuesta(e.target.value)}
      />
      <ErrorApi error={error} />
      <div className="acciones-fila">
        <Boton icono={modo === 'verificar' ? Check : RotateCcw} disabled={ocupado} onClick={() => decidir(true)}>
          {modo === 'verificar' ? 'Aprobar corrección' : 'Reabrir formulario'}
        </Boton>
        <Boton variante="contorno" icono={X} disabled={ocupado || !puedeRechazar} onClick={() => decidir(false)}>
          Rechazar
        </Boton>
      </div>
    </div>
  )
}
