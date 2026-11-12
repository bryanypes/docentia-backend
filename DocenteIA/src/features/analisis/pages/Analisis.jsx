import { MessagesSquare, RefreshCw, ShieldOff } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarDocentes } from '../../docentes/services/docentesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { MensajeIA, RobotIA } from '../components/DocenteIA.jsx'
import { HallazgoBorrador } from '../components/HallazgoBorrador.jsx'
import { PanoramaAnalisis } from '../components/PanoramaAnalisis.jsx'
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
  descartarHallazgo,
  editarHallazgo,
  listarComentariosFiltrados,
  listarSolicitudes,
  publicarRevision,
  solicitarAnalisis,
  validarHallazgo,
} from '../services/analisisService.js'
import './Analisis.css'

const MOTIVO_MINIMO = 10

function EstadoSolicitud({ solicitud, agotado, onReanudar }) {
  const { estado } = solicitud
  if (estaEnCurso(solicitud)) {
    return (
      <MensajeIA robot="pensando" tono="violeta" titulo={`${ETIQUETA_SOLICITUD[estado]}…`} role="status">
        <p>
          {agotado
            ? 'Me está tomando más de lo normal. Puedes seguir esperando o volver más tarde.'
            : 'Estoy leyendo los comentarios. La pantalla se actualiza sola; puedes salir y volver.'}
        </p>
        {agotado && (
          <div>
            <Boton variante="contorno" onClick={onReanudar}>
              Seguir esperando
            </Boton>
          </div>
        )}
      </MensajeIA>
    )
  }
  if (estado === ESTADOS_SOLICITUD.INSUFICIENTE) {
    return (
      <MensajeIA robot="error" tono="alerta" titulo="Evidencia insuficiente" role="status">
        <p>
          Llegaron {solicitud.encuestasCerradas ?? 0} de {MINIMO_ENCUESTAS} encuestas con comentario que necesito para
          analizar.{' '}
          {solicitud.mensaje}
        </p>
      </MensajeIA>
    )
  }
  if (estado === ESTADOS_SOLICITUD.ERROR) {
    return (
      <MensajeIA robot="error" tono="coral" titulo="No pude generar el análisis" role="alert">
        <p>{solicitud.mensaje ?? 'Puedes volver a solicitarlo.'}</p>
      </MensajeIA>
    )
  }
  return null
}

function ComentariosFiltrados({ idSolicitud, cantidad }) {
  const cargar = useCallback(() => listarComentariosFiltrados(idSolicitud), [idSolicitud])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])
  const paginacion = usePaginacion(datos ?? [], {
    clavePagina: 'paginaComentarios',
    claveTamano: 'tamanoComentarios',
  })

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
          {paginacion.visibles.map((comentario) => (
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
      {datos?.length > 0 && <Paginacion {...paginacion} etiqueta="comentarios excluidos" />}
    </Panel>
  )
}

function Borrador({ solicitud, puedeValidar, onValidado }) {
  const [validando, setValidando] = useState(null)
  const [errores, setErrores] = useState({})
  const [publicando, setPublicando] = useState(false)
  const [errorPublicar, setErrorPublicar] = useState(null)
  const { borrador } = solicitud

  async function aplicar(hallazgo, accion) {
    setValidando(hallazgo.idHallazgo)
    setErrores((actuales) => ({ ...actuales, [hallazgo.idHallazgo]: null }))
    try {
      await accion()
      onValidado()
      return true
    } catch (error) {
      setErrores((actuales) => ({ ...actuales, [hallazgo.idHallazgo]: error }))
      return false
    } finally {
      setValidando(null)
    }
  }

  async function publicar() {
    setPublicando(true)
    setErrorPublicar(null)
    try {
      await publicarRevision(solicitud.idSolicitud)
      onValidado()
    } catch (error) {
      setErrorPublicar(error)
    } finally {
      setPublicando(false)
    }
  }

  const validados = borrador.hallazgos.filter((hallazgo) => hallazgo.estado === 'VALIDADO').length
  const fortalezas = borrador.hallazgos.filter((hallazgo) => hallazgo.tipo === 'FORTALEZA').length
  const oportunidades = borrador.hallazgos.length - fortalezas
  const publicada = borrador.estado === 'VALIDADO'
  const editable = puedeValidar && !publicada

  return (
    <>
      <Panel
        titulo="Borrador del análisis"
        descripcion={`Generado el ${formatearFechaHora(borrador.fechaGeneracion)} con ${borrador.modelo} (${borrador.versionInstruccion}). ${solicitud.comentariosEnviados} comentarios analizados.`}
        icono={MessagesSquare}
      >
        <MensajeIA
          robot={publicada ? 'aprobada' : 'senala'}
          tono={publicada ? 'turquesa' : 'azul'}
          titulo={`Leí ${solicitud.comentariosEnviados} comentarios y encontré ${fortalezas} ${fortalezas === 1 ? 'fortaleza' : 'fortalezas'} y ${oportunidades} ${oportunidades === 1 ? 'oportunidad' : 'oportunidades'} de mejora.`}
        >
          <p>
            <strong>
              {validados} de {borrador.hallazgos.length} hallazgos validados.
            </strong>{' '}
            Cada uno cita palabras textuales de los estudiantes para que puedas verificarlo. El docente no ve este
            borrador: solo lo que la coordinación valide y publique.
          </p>
        </MensajeIA>
        {publicada && <Aviso tipo="exito" titulo="Revisión publicada">El docente ya ve los hallazgos incluidos.</Aviso>}
        {editable && (
          <div className="analisis__acciones">
            <Boton onClick={publicar} disabled={publicando}>
              {publicando ? 'Publicando…' : 'Publicar revisión'}
            </Boton>
            <span className="campo__ayuda">Incluye o descarta cada hallazgo antes de publicar.</span>
          </div>
        )}
        <ErrorApi error={errorPublicar} />
      </Panel>
      {TIPOS_HALLAZGO.map(({ tipo, nombre }) => {
        const hallazgos = borrador.hallazgos.filter((hallazgo) => hallazgo.tipo === tipo)
        if (hallazgos.length === 0) return null
        return (
          <Panel
            key={tipo}
            titulo={nombre}
            descripcion={`${hallazgos.length} ${hallazgos.length === 1 ? 'hallazgo' : 'hallazgos'} propuestos por DocenteIA`}
            tono={tipo === 'FORTALEZA' ? 'turquesa' : 'coral'}
            acciones={<RobotIA robot={tipo === 'FORTALEZA' ? 'aprobada' : 'senala'} />}
          >
            {hallazgos.map((hallazgo) => (
              <HallazgoBorrador
                key={hallazgo.idHallazgo}
                hallazgo={hallazgo}
                puedeValidar={editable}
                validando={validando === hallazgo.idHallazgo}
                error={errores[hallazgo.idHallazgo]}
                onValidar={(h) => aplicar(h, () => validarHallazgo(h.idHallazgo))}
                onDescartar={(h, motivo) => aplicar(h, () => descartarHallazgo(h.idHallazgo, motivo))}
                onEditar={(h, texto) => aplicar(h, () => editarHallazgo(h.idHallazgo, texto))}
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
  const [motivo, setMotivo] = useState('')

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
    setMotivo('')
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(nombre, valor)
    else nuevos.delete(nombre)
    setParametros(nuevos, { replace: true, state: { conservarFoco: true } })
  }

  // Si ya hay un análisis, pedir otro es regenerarlo: lleva motivo (10 a 300 caracteres).
  const pideMotivo = Boolean(idSolicitud)
  const motivoListo = motivo.trim().length >= MOTIVO_MINIMO

  async function solicitar() {
    setEnviando(true)
    setErrorSolicitar(null)
    try {
      const nueva = pideMotivo
        ? await solicitarAnalisis(docenteId, periodoId, motivo.trim())
        : await solicitarAnalisis(docenteId, periodoId)
      setSolicitudNueva({ clave, id: nueva.idSolicitud })
      setMotivo('')
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
              <option value="">Todos · panorama</option>
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

        <MensajeIA robot="saludo" titulo="Hola, soy DocenteIA" tamano={docenteId ? 'compacto' : 'normal'}>
          <ol className="analisis__pasos-ia">
            <li>
              <span>1</span>Leo los comentarios abiertos de los estudiantes, ya anonimizados y sin los ofensivos.
            </li>
            <li>
              <span>2</span>Los agrupo en fortalezas y oportunidades de mejora, citando sus palabras textuales.
            </li>
            <li>
              <span>3</span>La coordinación revisa cada hallazgo y decide qué ve el docente.
            </li>
          </ol>
        </MensajeIA>

        {!docenteId || !periodoId ? (
          periodoId ? (
            <PanoramaAnalisis periodoId={periodoId} onElegirDocente={(id) => cambiarFiltro('docente', id)} />
          ) : (
            <MensajeIA robot="esperando" tono="violeta" titulo="Elige un periodo para empezar">
              <p>Te mostraré el panorama de todos los docentes y, si eliges uno, su análisis en detalle.</p>
            </MensajeIA>
          )
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
                {pideMotivo && (
                  <Campo
                    id="motivoAnalisis"
                    etiqueta="Motivo para pedir otro análisis"
                    ayuda="Entre 10 y 300 caracteres. Debe pasar una hora desde la última solicitud."
                  >
                    <input
                      id="motivoAnalisis"
                      className="campo__control"
                      value={motivo}
                      maxLength={300}
                      onChange={(e) => setMotivo(e.target.value)}
                    />
                  </Campo>
                )}
                <Boton
                  onClick={solicitar}
                  disabled={enviando || estaEnCurso(ultima.datos) || (pideMotivo && !motivoListo)}
                >
                  {enviando ? 'Solicitando…' : idSolicitud ? 'Solicitar un análisis nuevo' : 'Solicitar análisis'}
                </Boton>
              </div>
            )}
            <ErrorApi error={errorSolicitar} />
            {idSolicitud ? (
              <DetalleSolicitud key={idSolicitud} idSolicitud={idSolicitud} puedeValidar={esCoordinador} />
            ) : (
              <MensajeIA robot="esperando" tono="violeta" titulo="Todavía no he analizado a este docente en este periodo">
                <p>
                  {esCoordinador
                    ? `Pídemelo con «Solicitar análisis». Necesito al menos ${MINIMO_ENCUESTAS} encuestas con comentario.`
                    : 'La coordinación aún no lo ha solicitado.'}
                </p>
              </MensajeIA>
            )}
          </>
        )}
      </div>
    </>
  )
}
