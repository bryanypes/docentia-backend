import { Check, Info, MessagesSquare, RefreshCw, ShieldOff } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, formatearFechaHora } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarDocentes } from '../../docentes/services/docentesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { ComentariosDocente } from '../components/ComentariosDocente.jsx'
import { MensajeIA, RobotIA } from '../components/DocenteIA.jsx'
import { ResultadoComentarios } from '../components/ResultadoComentarios.jsx'
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

/**
 * Cuándo se puede pedir el análisis, en tres pasos con el estado de cada uno: los estudiantes comentan
 * en su encuesta, cierra la recolección y la coordinación lo solicita.
 */
function GuiaAnalisis({ recoleccionAbierta, fechaCierre, solicitado, esCoordinador }) {
  const pasos = [
    {
      titulo: 'Los estudiantes comentan',
      texto: 'Al final de su encuesta, cada estudiante escribe un comentario anónimo sobre el docente.',
      estado: recoleccionAbierta ? 'actual' : 'hecho',
    },
    {
      titulo: 'Cierra la recolección',
      texto: recoleccionAbierta
        ? `Mientras sigue abierta no se analiza${fechaCierre ? `: cierra el ${formatearFecha(fechaCierre)}` : ''}.`
        : `La recolección ya cerró${fechaCierre ? ` el ${formatearFecha(fechaCierre)}` : ''}.`,
      estado: recoleccionAbierta ? 'pendiente' : 'hecho',
    },
    {
      titulo: esCoordinador ? 'Solicitas el análisis' : 'La coordinación lo solicita',
      texto: `Por docente, con al menos ${MINIMO_ENCUESTAS} encuestas con comentario. Luego se revisa cada hallazgo antes de que el docente lo vea.`,
      estado: recoleccionAbierta ? 'pendiente' : solicitado ? 'hecho' : 'actual',
    },
  ]
  return (
    <ol className="analisis__guia" aria-label="Cuándo se puede pedir el análisis">
      {pasos.map((paso, i) => (
        <li key={paso.titulo} className={`analisis__guia-paso analisis__guia-paso--${paso.estado}`}>
          <span className="analisis__guia-marca" aria-hidden="true">
            {paso.estado === 'hecho' ? <Check size={14} strokeWidth={3} /> : i + 1}
          </span>
          <div>
            <strong>
              {paso.titulo}
              {paso.estado === 'actual' && <em className="analisis__guia-ahora">Ahora</em>}
            </strong>
            <span>{paso.texto}</span>
          </div>
          <span className="solo-lectores">
            {paso.estado === 'hecho' ? ' (cumplido)' : paso.estado === 'actual' ? ' (paso actual)' : ' (pendiente)'}
          </span>
        </li>
      ))}
    </ol>
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

/** Resultado arriba y comentarios abajo; tocar un tema filtra los comentarios por su criterio. */
function VistaComentarios({ idDocente, periodoId, esCoordinador }) {
  const [criterio, setCriterio] = useState(null)
  return (
    <>
      <ResultadoComentarios
        idDocente={idDocente}
        periodoId={periodoId}
        esCoordinador={esCoordinador}
        criterioActivo={criterio}
        onFiltrarCriterio={setCriterio}
      />
      <ComentariosDocente idDocente={idDocente} periodoId={periodoId} criterio={criterio} onCriterio={setCriterio} />
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
  // La recolección solo puede estar abierta en el periodo activo; los anteriores ya cerraron.
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)
  const esPeriodoActivo = Boolean(periodoActivo) && String(periodoActivo.idPeriodo) === periodoId
  const recoleccionAbierta = esPeriodoActivo && Boolean(recoleccion?.abierta)
  const [solicitudNueva, setSolicitudNueva] = useState(null)
  const [errorSolicitar, setErrorSolicitar] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [verAyuda, setVerAyuda] = useState(false)

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
        titulo="Análisis de comentarios de estudiantes"
        subtitulo="Lo que los estudiantes escriben al final de su encuesta, resumido en fortalezas y oportunidades con citas textuales."
        ayuda={
          <button
            type="button"
            className="analisis__ayuda"
            onClick={() => setVerAyuda(true)}
            aria-label="Cómo funciona el análisis y cuándo se puede pedir"
            title="Cómo funciona el análisis y cuándo se puede pedir"
          >
            <Info size={18} aria-hidden="true" />
          </button>
        }
      />
      {verAyuda && (
        <Modal
          titulo="Cómo funciona el análisis"
          descripcion="Qué hace DocenteIA con los comentarios y cuándo se puede pedir."
          icono={Info}
          onCerrar={() => setVerAyuda(false)}
        >
          <MensajeIA robot="saludo" titulo="Hola, soy DocenteIA: analizo los comentarios de los estudiantes" tamano="compacto">
            <p>
              Leo los comentarios anónimos que dejan en su encuesta, ya sin los ofensivos, y los agrupo en fortalezas y
              oportunidades de mejora citando sus palabras. Así sabes cuándo puedes pedirme el análisis:
            </p>
          </MensajeIA>
          <GuiaAnalisis
            recoleccionAbierta={recoleccionAbierta}
            fechaCierre={esPeriodoActivo ? recoleccion?.fechaCierre : null}
            solicitado={Boolean(docenteId && idSolicitud)}
            esCoordinador={esCoordinador}
          />
        </Modal>
      )}
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
                <Boton onClick={solicitar} disabled={enviando || recoleccionAbierta || estaEnCurso(ultima.datos)}>
                  {enviando ? 'Solicitando…' : idSolicitud ? 'Solicitar un análisis nuevo' : 'Solicitar análisis'}
                </Boton>
                {recoleccionAbierta && (
                  <span className="campo__ayuda">Disponible cuando cierre la recolección de calificaciones.</span>
                )}
              </div>
            )}
            <ErrorApi error={errorSolicitar} />
            {/* Resultado del análisis y comentarios (contrato api-comentarios-ia); la key reinicia los
                filtros al cambiar de docente o periodo. */}
            <VistaComentarios
              key={`${docenteId}-${periodoId}-${idSolicitud ?? 'sin'}`}
              idDocente={docenteId}
              periodoId={periodoId}
              esCoordinador={esCoordinador}
            />
            {idSolicitud ? (
              <DetalleSolicitud key={idSolicitud} idSolicitud={idSolicitud} puedeValidar={esCoordinador} />
            ) : (
              <MensajeIA robot="esperando" tono="violeta" titulo="Revisión de hallazgos">
                <p>
                  {esCoordinador
                    ? `Cuando pidas el análisis con «Solicitar análisis», aquí revisarás y publicarás cada hallazgo. Necesito al menos ${MINIMO_ENCUESTAS} encuestas con comentario.`
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
