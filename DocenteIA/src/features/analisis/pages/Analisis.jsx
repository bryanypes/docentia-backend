import { MessagesSquare, RefreshCw, ShieldOff } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarDocentes } from '../../docentes/services/docentesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { HallazgoBorrador } from '../components/HallazgoBorrador.jsx'
import {
  CATEGORIAS_FILTRO,
  ESTADOS_SOLICITUD,
  estaEnCurso,
  ETIQUETA_SOLICITUD,
  MINIMO_ENCUESTAS,
  TIPOS_HALLAZGO,
} from '../estados.js'
import { useSeguimientoSolicitud } from '../hooks/useSeguimientoSolicitud.js'
import {
  listarComentariosFiltrados,
  listarSolicitudes,
  solicitarAnalisis,
  validarHallazgo,
} from '../services/analisisService.js'
import './Analisis.css'

function EstadoSolicitud({ solicitud, agotado, onReanudar }) {
  const { estado } = solicitud
  if (estaEnCurso(solicitud)) {
    return (
      <div className="analisis__espera" role="status">
        <img src={robotPensando} alt="" className="analisis__espera-robot" />
        <div>
          <strong>{ETIQUETA_SOLICITUD[estado]}…</strong>
          <p className="campo__ayuda">
            {agotado
              ? 'Está tardando más de lo normal. Puedes seguir esperando o volver más tarde.'
              : 'Se actualiza solo cada pocos segundos; puedes salir de la pantalla.'}
          </p>
          {agotado && (
            <Boton variante="contorno" onClick={onReanudar}>
              Seguir esperando
            </Boton>
          )}
        </div>
      </div>
    )
  }
  if (estado === ESTADOS_SOLICITUD.INSUFICIENTE) {
    return (
      <Aviso tipo="alerta" titulo="Evidencia insuficiente">
        {solicitud.encuestasCerradas ?? 0} de {MINIMO_ENCUESTAS} encuestas con comentario. {solicitud.mensaje}
      </Aviso>
    )
  }
  if (estado === ESTADOS_SOLICITUD.ERROR) {
    return (
      <Aviso tipo="error" titulo="No se pudo generar el análisis">
        {solicitud.mensaje ?? 'Puedes volver a solicitarlo.'}
      </Aviso>
    )
  }
  return null
}

function ComentariosFiltrados({ idSolicitud, cantidad }) {
  const cargar = useCallback(() => listarComentariosFiltrados(idSolicitud), [idSolicitud])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])

  return (
    <Panel
      titulo={`Comentarios excluidos por el filtro (${cantidad ?? 0})`}
      descripcion="Son ofensivos o hablan de la persona y no de su práctica docente. No se enviaron al análisis."
      icono={ShieldOff}
    >
      {cargando && <Cargando texto="Cargando comentarios excluidos" bloques={1} />}
      {error && (
        <EstadoVista tipo="error" titulo="No fue posible cargarlos" mensaje={error.message}>
          <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      )}
      {datos?.length === 0 && <p>El filtro no excluyó ningún comentario.</p>}
      {datos?.length > 0 && (
        <ul className="analisis__filtrados">
          {datos.map((comentario) => (
            <li key={comentario.idComentario}>
              <span className="pildora pildora--alerta">
                {CATEGORIAS_FILTRO[comentario.categoria] ?? comentario.categoria}
              </span>
              <details>
                <summary>Ver el comentario excluido</summary>
                <p className="analisis__comentario">{comentario.texto}</p>
              </details>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}

function Borrador({ solicitud, puedeValidar, onValidado }) {
  const [validando, setValidando] = useState(null)
  const [errores, setErrores] = useState({})
  const { borrador } = solicitud

  async function validar(hallazgo) {
    setValidando(hallazgo.idHallazgo)
    setErrores((actuales) => ({ ...actuales, [hallazgo.idHallazgo]: null }))
    try {
      await validarHallazgo(hallazgo.idHallazgo)
      onValidado()
    } catch (error) {
      setErrores((actuales) => ({ ...actuales, [hallazgo.idHallazgo]: error }))
    } finally {
      setValidando(null)
    }
  }

  const validados = borrador.hallazgos.filter((hallazgo) => hallazgo.estado === 'VALIDADO').length

  return (
    <>
      <Panel
        titulo="Borrador del análisis"
        descripcion={`Generado el ${formatearFechaHora(borrador.fechaGeneracion)} con ${borrador.modelo} (${borrador.versionInstruccion}). ${solicitud.comentariosEnviados} comentarios analizados.`}
        icono={MessagesSquare}
      >
        <p>
          <strong>
            {validados} de {borrador.hallazgos.length} hallazgos validados.
          </strong>{' '}
          El docente no ve este borrador: solo verá lo que la coordinación valide.
        </p>
      </Panel>
      {TIPOS_HALLAZGO.map(({ tipo, nombre }) => {
        const hallazgos = borrador.hallazgos.filter((hallazgo) => hallazgo.tipo === tipo)
        if (hallazgos.length === 0) return null
        return (
          <Panel key={tipo} titulo={nombre}>
            {hallazgos.map((hallazgo) => (
              <HallazgoBorrador
                key={hallazgo.idHallazgo}
                hallazgo={hallazgo}
                puedeValidar={puedeValidar}
                validando={validando === hallazgo.idHallazgo}
                error={errores[hallazgo.idHallazgo]}
                onValidar={validar}
              />
            ))}
          </Panel>
        )
      })}
    </>
  )
}

function DetalleSolicitud({ idSolicitud, puedeValidar }) {
  const { solicitud, error, agotado, reanudar } = useSeguimientoSolicitud(idSolicitud)

  if (error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar el análisis" mensaje={error.message}>
        <Boton icono={RefreshCw} onClick={reanudar}>Intentar de nuevo</Boton>
      </EstadoVista>
    )
  }
  if (!solicitud) return <Cargando texto="Cargando el análisis" bloques={2} />

  return (
    <>
      <p className="campo__ayuda">
        Solicitud #{solicitud.idSolicitud} · {ETIQUETA_SOLICITUD[solicitud.estado]} · pedida el{' '}
        {formatearFechaHora(solicitud.fechaSolicitud)}
      </p>
      <EstadoSolicitud solicitud={solicitud} agotado={agotado} onReanudar={reanudar} />
      {solicitud.estado === ESTADOS_SOLICITUD.COMPLETADO && (
        <>
          <Borrador solicitud={solicitud} puedeValidar={puedeValidar} onValidado={reanudar} />
          <ComentariosFiltrados idSolicitud={solicitud.idSolicitud} cantidad={solicitud.comentariosFiltrados} />
        </>
      )}
    </>
  )
}

export default function Analisis() {
  const { usuario } = useSesion()
  const esCoordinador = usuario.rol === ROLES.COORDINADOR
  const [parametros, setParametros] = useSearchParams()
  const docenteId = parametros.get('docente') ?? ''
  const docentes = useFetch(() => listarDocentes(null))
  const periodos = useFetch(listarPeriodos)
  const periodoActivo = periodos.datos?.find((periodo) => periodo.esActivo)
  const periodoId = parametros.get('periodo') ?? (periodoActivo ? String(periodoActivo.idPeriodo) : '')
  const [solicitudNueva, setSolicitudNueva] = useState(null)
  const [errorSolicitar, setErrorSolicitar] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const cargarUltima = useCallback(
    () =>
      docenteId && periodoId
        ? listarSolicitudes(docenteId, periodoId).then((lista) => lista[0] ?? null)
        : Promise.resolve(null),
    [docenteId, periodoId],
  )
  const ultima = useFetch(cargarUltima, [cargarUltima])
  // La recién solicitada manda sobre la última consultada, solo para el mismo docente y periodo.
  const clave = `${docenteId}-${periodoId}`
  const idSolicitud = solicitudNueva?.clave === clave ? solicitudNueva.id : (ultima.datos?.idSolicitud ?? null)

  function cambiarFiltro(nombre, valor) {
    setErrorSolicitar(null)
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(nombre, valor)
    else nuevos.delete(nombre)
    setParametros(nuevos, { replace: true, state: { conservarFoco: true } })
  }

  async function solicitar() {
    setEnviando(true)
    setErrorSolicitar(null)
    try {
      const nueva = await solicitarAnalisis(docenteId, periodoId)
      setSolicitudNueva({ clave, id: nueva.idSolicitud })
    } catch (error) {
      setErrorSolicitar(error)
      // Ya había una en curso: se muestra esa.
      if (error.status === 409 && !error.datos?.etapa) ultima.recargar()
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <Encabezado
        miga={`${esCoordinador ? 'Coordinación' : 'Vicerrectoría'} · Análisis de comentarios`}
        titulo="Análisis de comentarios"
        subtitulo="Resume los comentarios abiertos en fortalezas y oportunidades, con citas textuales verificables."
      />
      <div className="pagina">
        <div className="analisis__filtros">
          <Campo id="docenteAnalisis" etiqueta="Docente">
            <select
              id="docenteAnalisis"
              className="campo__control"
              value={docenteId}
              onChange={(e) => cambiarFiltro('docente', e.target.value)}
            >
              <option value="">Elige un docente</option>
              {(docentes.datos ?? []).map((docente) => (
                <option key={docente.idDocente} value={docente.idDocente}>
                  {docente.nombreCompleto} · {docente.nombrePrograma}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="periodoAnalisis" etiqueta="Periodo">
            <select
              id="periodoAnalisis"
              className="campo__control"
              value={periodoId}
              onChange={(e) => cambiarFiltro('periodo', e.target.value)}
            >
              <option value="">Elige un periodo</option>
              {(periodos.datos ?? []).map((periodo) => (
                <option key={periodo.idPeriodo} value={periodo.idPeriodo}>
                  {periodo.nombre}
                  {periodo.esActivo ? ' (activo)' : ''}
                </option>
              ))}
            </select>
          </Campo>
        </div>

        {!docenteId || !periodoId ? (
          <EstadoVista
            titulo="Elige un docente y un periodo"
            mensaje="Verás el último análisis o podrás solicitar uno nuevo."
          />
        ) : ultima.error ? (
          <EstadoVista tipo="error" titulo="No fue posible consultar el análisis" mensaje={ultima.error.message}>
            <Boton icono={RefreshCw} onClick={ultima.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        ) : ultima.cargando ? (
          <Cargando texto="Consultando el último análisis" bloques={2} />
        ) : (
          <>
            {esCoordinador && (
              <div className="analisis__acciones">
                <Boton onClick={solicitar} disabled={enviando || estaEnCurso(ultima.datos)}>
                  {enviando ? 'Solicitando…' : idSolicitud ? 'Solicitar un análisis nuevo' : 'Solicitar análisis'}
                </Boton>
              </div>
            )}
            <ErrorApi error={errorSolicitar} />
            {idSolicitud ? (
              <DetalleSolicitud key={idSolicitud} idSolicitud={idSolicitud} puedeValidar={esCoordinador} />
            ) : (
              <EstadoVista
                titulo="Sin análisis para este docente y periodo"
                mensaje={
                  esCoordinador
                    ? `Solicítalo con el botón de arriba. Se necesitan al menos ${MINIMO_ENCUESTAS} encuestas con comentario.`
                    : 'La coordinación aún no lo ha solicitado.'
                }
              />
            )}
          </>
        )}
      </div>
    </>
  )
}
