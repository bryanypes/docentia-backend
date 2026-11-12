import { Cpu, MessagesSquare, RefreshCw, ShieldOff, Sparkles } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
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
import { formatearEntero, formatearFechaHora } from '../../../shared/utils/formatters.js'
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
  obtenerModelosAnalisis,
  publicarRevision,
  solicitarAnalisis,
  validarHallazgo,
} from '../services/analisisService.js'
import './Analisis.css'

const MOTIVO_MINIMO = 10

function SelectorModelo({ modelos, valor, onCambiar, confirmaExterno, onConfirmarExterno }) {
  const opciones = modelos.opciones ?? []
  const opcionElegida = opciones.find((opcion) => opcion.codigo === valor)
  const puedeUsarLlm = Boolean(modelos.llmDisponible)
  const tokensMaximos = modelos.tokensPorAnalisis?.totalMaximo

  return (
    <fieldset className="analisis__modelos">
      <legend>Selecciona el motor para este análisis</legend>
      <div className="analisis__modelos-opciones">
        {opciones.map((opcion) => {
          const Icono = opcion.codigo === 'LOCAL' ? Cpu : Sparkles
          const disponible = opcion.disponible && (opcion.codigo !== 'LLM' || puedeUsarLlm)
          return (
            <label
              key={opcion.codigo}
              className={`analisis__modelo${valor === opcion.codigo ? ' analisis__modelo--activo' : ''}${!disponible ? ' analisis__modelo--deshabilitado' : ''}`}
            >
              <input
                type="radio"
                name="modeloAnalisis"
                value={opcion.codigo}
                checked={valor === opcion.codigo}
                disabled={!disponible}
                onChange={() => onCambiar(opcion.codigo)}
              />
              <span className="analisis__modelo-icono" aria-hidden="true">
                <Icono size={19} />
              </span>
              <span className="analisis__modelo-contenido">
                <span className="analisis__modelo-titulo">
                  <strong>{opcion.nombre}</strong>
                  {opcion.codigo === modelos.porDefecto && <span className="pildora">Predeterminado</span>}
                </span>
                <span>{opcion.descripcion}</span>
                {opcion.codigo === 'LLM' && !disponible && (
                  <span className="analisis__modelo-ayuda">No está configurado en este entorno.</span>
                )}
                {opcion.codigo === 'LLM' && disponible && modelos.modeloLlm && (
                  <span className="analisis__modelo-ayuda">Modelo: {modelos.modeloLlm}</span>
                )}
              </span>
            </label>
          )
        })}
      </div>
      {valor === 'LOCAL' && (
        <Aviso tipo="info" titulo="Procesamiento local">
          Los comentarios se analizan en DOCENTIA; no se envían a proveedores externos ni consumen tokens.
        </Aviso>
      )}
      {valor === 'LLM' && opcionElegida?.disponible && puedeUsarLlm && (
        <>
          <Aviso tipo="alerta" titulo="Uso de un proveedor externo">
            <p>{modelos.advertencia || 'El análisis se procesará con el modelo externo configurado.'}</p>
            <p>
              Límite por análisis: {formatearEntero(modelos.maxComentariosLlm)} comentarios
              {tokensMaximos != null ? ` y ${formatearEntero(tokensMaximos)} tokens` : ''}.
              {modelos.tokensRestantesHoy != null &&
                ` Presupuesto diario restante: ${formatearEntero(modelos.tokensRestantesHoy)} tokens.`}
            </p>
          </Aviso>
          <label className="analisis__consentimiento">
            <input type="checkbox" checked={confirmaExterno} onChange={(e) => onConfirmarExterno(e.target.checked)} />
            <span>Confirmo que autorizo enviar comentarios anonimizados al proveedor externo para este análisis.</span>
          </label>
        </>
      )}
    </fieldset>
  )
}

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

function DetalleSolicitud({ idSolicitud, puedeValidar, clave, onTerminar }) {
  const { solicitud, error, agotado, reanudar } = useSeguimientoSolicitud(idSolicitud)

  useEffect(() => {
    if (solicitud && Number(solicitud.idSolicitud) === Number(idSolicitud) && !estaEnCurso(solicitud)) {
      onTerminar(clave)
    }
  }, [solicitud, idSolicitud, clave, onTerminar])

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
  const modelos = useFetch(obtenerModelosAnalisis)
  const periodoActivo = periodos.datos?.find((periodo) => periodo.esActivo)
  const periodoId = parametros.get('periodo') ?? (periodoActivo ? String(periodoActivo.idPeriodo) : '')
  const [solicitudNueva, setSolicitudNueva] = useState(null)
  const [errorSolicitar, setErrorSolicitar] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [motivo, setMotivo] = useState('')
  const [modeloElegido, setModeloElegido] = useState(null)
  const [confirmaExterno, setConfirmaExterno] = useState(false)
  const [solicitudPendienteClave, setSolicitudPendienteClave] = useState(null)

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
  const liberarSolicitud = useCallback((claveTerminada) => {
    setSolicitudPendienteClave((actual) => (actual === claveTerminada ? null : actual))
  }, [])
  const modelo = modeloElegido ?? modelos.datos?.porDefecto ?? ''
  const opcionModelo = modelos.datos?.opciones?.find((opcion) => opcion.codigo === modelo)
  const modeloDisponible = Boolean(
    opcionModelo?.disponible && (modelo !== 'LLM' || modelos.datos?.llmDisponible),
  )
  const modeloConfirmado = modelo !== 'LLM' || confirmaExterno

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
    if (!modeloDisponible || !modeloConfirmado) return
    setEnviando(true)
    setErrorSolicitar(null)
    try {
      const nueva = await solicitarAnalisis(docenteId, periodoId, pideMotivo ? motivo.trim() : null, modelo)
      setSolicitudNueva({ clave, id: nueva.idSolicitud })
      setSolicitudPendienteClave(clave)
      setMotivo('')
      setConfirmaExterno(false)
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
              <Panel
                className="analisis__modelo-panel"
                titulo="Motor de análisis"
                descripcion="Elige si DocenteIA procesa los comentarios localmente o usa el modelo externo configurado."
                icono={Sparkles}
                tono="violeta"
              >
                {modelos.cargando && <Cargando texto="Consultando modelos disponibles" bloques={1} />}
                {modelos.error && (
                  <EstadoVista tipo="error" titulo="No fue posible consultar los modelos" mensaje={modelos.error.message}>
                    <Boton icono={RefreshCw} onClick={modelos.recargar}>Intentar de nuevo</Boton>
                  </EstadoVista>
                )}
                {modelos.datos && (
                  <SelectorModelo
                    modelos={modelos.datos}
                    valor={modelo}
                    onCambiar={(nuevoModelo) => {
                      setModeloElegido(nuevoModelo)
                      setConfirmaExterno(false)
                    }}
                    confirmaExterno={confirmaExterno}
                    onConfirmarExterno={setConfirmaExterno}
                  />
                )}
              </Panel>
            )}
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
                  disabled={
                    enviando ||
                    !modeloDisponible ||
                    !modeloConfirmado ||
                    solicitudPendienteClave === clave ||
                    estaEnCurso(ultima.datos) ||
                    (pideMotivo && !motivoListo)
                  }
                >
                  {enviando ? 'Solicitando…' : idSolicitud ? 'Regenerar análisis' : 'Solicitar análisis'}
                </Boton>
              </div>
            )}
            <ErrorApi error={errorSolicitar} />
            {idSolicitud ? (
              <DetalleSolicitud
                key={idSolicitud}
                idSolicitud={idSolicitud}
                puedeValidar={esCoordinador}
                clave={clave}
                onTerminar={liberarSolicitud}
              />
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
