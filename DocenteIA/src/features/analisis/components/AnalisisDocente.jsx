import { ClipboardCheck, LayoutDashboard, MessageSquareQuote, RefreshCw, ShieldOff, Sparkles } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
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
import { ComentariosDocente } from './ComentariosDocente.jsx'
import { RobotIA } from './DocenteIA.jsx'
import { HallazgoBorrador } from './HallazgoBorrador.jsx'
import { ResultadoComentarios } from './ResultadoComentarios.jsx'
import './AnalisisDocente.css'

const POR_DECIDIR = ['PENDIENTE_VALIDACION', 'SIN_CITA_VERIFICADA']

/**
 * Franja de arriba: en palabras simples, en qué va el análisis de este docente y el único botón que
 * toca ahora (solicitar, publicar o volver a intentar).
 */
function FranjaEstado({ solicitud, agotado, esCoordinador, enviando, publicando, onSolicitar, onPublicar, onRevisar, onReanudar }) {
  let tono = 'violeta'
  let robot = 'esperando'
  let titulo
  let detalle
  let accion = null
  let secundaria = null
  const pedirOtro = esCoordinador && (
    <Boton variante="enlace" onClick={onSolicitar} disabled={enviando}>
      {enviando ? 'Solicitando…' : 'Analizar de nuevo'}
    </Boton>
  )

  if (!solicitud) {
    titulo = 'Sin solicitar'
    detalle = esCoordinador
      ? `Pídeme el análisis y leo los comentarios de los estudiantes de este docente. Necesito al menos ${MINIMO_ENCUESTAS} encuestas con comentario.`
      : 'La coordinación aún no lo ha solicitado.'
    accion = esCoordinador && (
      <Boton onClick={onSolicitar} disabled={enviando}>
        {enviando ? 'Solicitando…' : 'Solicitar análisis'}
      </Boton>
    )
  } else if (estaEnCurso(solicitud)) {
    robot = 'pensando'
    titulo = `${ETIQUETA_SOLICITUD[solicitud.estado]}…`
    detalle = agotado
      ? 'Me está tomando más de lo normal. Puedes seguir esperando o volver más tarde.'
      : 'Estoy leyendo los comentarios. La pantalla se actualiza sola; puedes salir y volver.'
    accion = agotado && (
      <Boton variante="contorno" onClick={onReanudar}>
        Seguir esperando
      </Boton>
    )
  } else if (solicitud.estado === ESTADOS_SOLICITUD.INSUFICIENTE) {
    tono = 'alerta'
    robot = 'error'
    titulo = 'Faltan comentarios'
    detalle = `Llegaron ${solicitud.encuestasCerradas ?? 0} de ${MINIMO_ENCUESTAS} encuestas con comentario que necesito para analizar.`
    accion = esCoordinador && (
      <Boton variante="contorno" onClick={onSolicitar} disabled={enviando}>
        Volver a intentar
      </Boton>
    )
  } else if (solicitud.estado === ESTADOS_SOLICITUD.ERROR) {
    tono = 'coral'
    robot = 'error'
    titulo = 'No pude generar el análisis'
    detalle = solicitud.mensaje ?? 'Puedes volver a solicitarlo.'
    accion = esCoordinador && (
      <Boton onClick={onSolicitar} disabled={enviando}>
        Solicitar de nuevo
      </Boton>
    )
  } else {
    const hallazgos = solicitud.borrador?.hallazgos ?? []
    const validados = hallazgos.filter((h) => h.estado === 'VALIDADO').length
    const porDecidir = hallazgos.filter((h) => POR_DECIDIR.includes(h.estado)).length
    const publicada = solicitud.borrador?.estado === 'VALIDADO'
    if (publicada) {
      tono = 'turquesa'
      robot = 'aprobada'
      titulo = 'Publicado'
      detalle = 'El docente ya ve los hallazgos incluidos en su reunión.'
      secundaria = pedirOtro
    } else {
      robot = 'senala'
      titulo = 'Listo para revisar'
      detalle = (
        <>
          Leí {solicitud.comentariosEnviados} comentarios. <strong>{`${validados} de ${hallazgos.length} hallazgos validados.`}</strong>
          {porDecidir > 0 ? ` Te faltan ${porDecidir} por decidir.` : ' Ya puedes publicar.'}
        </>
      )
      accion = esCoordinador && (
        <>
          {porDecidir > 0 && (
            <Boton variante="contorno" icono={ClipboardCheck} onClick={onRevisar}>
              Revisar hallazgos
            </Boton>
          )}
          <Boton onClick={onPublicar} disabled={publicando}>
            {publicando ? 'Publicando…' : 'Publicar revisión'}
          </Boton>
        </>
      )
      secundaria = pedirOtro
    }
  }

  return (
    <section className={`franja-ia franja-ia--${tono}`} aria-live="polite">
      <RobotIA robot={robot} />
      <div className="franja-ia__texto">
        <strong className="franja-ia__titulo">{titulo}</strong>
        <p>{detalle}</p>
        {solicitud && (
          <span className="franja-ia__meta">
            Solicitud #{solicitud.idSolicitud} · pedida el {formatearFechaHora(solicitud.fechaSolicitud)}
          </span>
        )}
      </div>
      {(accion || secundaria) && (
        <div className="franja-ia__acciones">
          {accion}
          {secundaria}
        </div>
      )}
    </section>
  )
}

function ComentariosFiltrados({ idSolicitud, cantidad }) {
  const cargar = useCallback(() => listarComentariosFiltrados(idSolicitud), [idSolicitud])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])

  return (
    <details className="revision-ia__filtrados">
      <summary>
        <ShieldOff size={16} aria-hidden="true" />
        Comentarios excluidos por el filtro ({cantidad ?? 0})
        <span>Ofensivos o sobre la persona, no sobre su práctica docente. No se enviaron al análisis.</span>
      </summary>
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
              <span className="pildora pildora--alerta">{CATEGORIAS_FILTRO[comentario.categoria] ?? comentario.categoria}</span>
              <details>
                <summary>Ver el comentario excluido</summary>
                <p className="analisis__comentario">{comentario.texto}</p>
              </details>
            </li>
          ))}
        </ul>
      )}
    </details>
  )
}

/** Hallazgos del borrador para validar, descartar o editar, agrupados por tipo. */
function Revision({ solicitud, puedeValidar, onValidado }) {
  const [validando, setValidando] = useState(null)
  const [errores, setErrores] = useState({})
  const borrador = solicitud.borrador
  const editable = puedeValidar && borrador?.estado !== 'VALIDADO'

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

  if (!borrador) return <p className="revision-ia__vacio">Los hallazgos aparecen cuando el análisis termina.</p>

  return (
    <div className="revision-ia">
      <p className="revision-ia__nota">
        Cada hallazgo cita palabras textuales de los estudiantes para que puedas verificarlo. El docente no ve este
        borrador: solo lo que la coordinación valide y publique. Generado con {borrador.modelo} el{' '}
        {formatearFechaHora(borrador.fechaGeneracion)}.
      </p>
      {TIPOS_HALLAZGO.map(({ tipo, nombre }) => {
        const hallazgos = borrador.hallazgos.filter((hallazgo) => hallazgo.tipo === tipo)
        if (hallazgos.length === 0) return null
        return (
          <section key={tipo} className={`revision-ia__grupo revision-ia__grupo--${tipo === 'FORTALEZA' ? 'turquesa' : 'coral'}`}>
            <h3>
              {nombre} <span>{hallazgos.length}</span>
            </h3>
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
          </section>
        )
      })}
      <ComentariosFiltrados idSolicitud={solicitud.idSolicitud} cantidad={solicitud.comentariosFiltrados} />
    </div>
  )
}

/**
 * Análisis de un docente: la franja dice en qué va y qué hacer; abajo, tres pestañas sin repetir
 * nada: Resumen (lo que dicen los estudiantes), Comentarios (agrupados por tema) y Revisión
 * (hallazgos por validar antes de publicar).
 */
export function AnalisisDocente({ idDocente, periodoId, esCoordinador }) {
  const cargarUltima = useCallback(
    () => listarSolicitudes(idDocente, periodoId).then((lista) => lista[0] ?? null),
    [idDocente, periodoId],
  )
  const ultima = useFetch(cargarUltima, [cargarUltima])
  const [idNueva, setIdNueva] = useState(null)
  const [errorAccion, setErrorAccion] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [publicando, setPublicando] = useState(false)
  const [pestanaElegida, setPestanaElegida] = useState(null)
  const [criterio, setCriterio] = useState(null)
  const [version, setVersion] = useState(null)

  const idSolicitud = idNueva ?? ultima.datos?.idSolicitud ?? null
  const seguimiento = useSeguimientoSolicitud(idSolicitud)
  const solicitud = idSolicitud ? seguimiento.solicitud : null
  const hallazgos = solicitud?.borrador?.hallazgos ?? []
  const porDecidir = hallazgos.filter((h) => POR_DECIDIR.includes(h.estado)).length
  const enRevision = solicitud?.estado === ESTADOS_SOLICITUD.COMPLETADO && solicitud.borrador?.estado !== 'VALIDADO'
  // Con hallazgos por revisar se abre directo la revisión; si no, el resumen.
  const pestana = pestanaElegida ?? (enRevision ? 'revision' : 'resumen')

  async function solicitar() {
    setEnviando(true)
    setErrorAccion(null)
    try {
      const nueva = await solicitarAnalisis(idDocente, periodoId)
      setIdNueva(nueva.idSolicitud)
      setPestanaElegida(null)
    } catch (error) {
      setErrorAccion(error)
      // Ya había una en curso: se muestra esa.
      if (error.status === 409 && !error.datos?.etapa) ultima.recargar()
    } finally {
      setEnviando(false)
    }
  }

  async function publicar() {
    setPublicando(true)
    setErrorAccion(null)
    try {
      await publicarRevision(solicitud.idSolicitud)
      seguimiento.reanudar()
    } catch (error) {
      setErrorAccion(error)
    } finally {
      setPublicando(false)
    }
  }

  if (ultima.error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible consultar el análisis" mensaje={ultima.error.message}>
        <Boton icono={RefreshCw} onClick={ultima.recargar}>Intentar de nuevo</Boton>
      </EstadoVista>
    )
  }
  if (ultima.cargando || (idSolicitud && !solicitud && !seguimiento.error)) {
    return <Cargando texto="Consultando el análisis" bloques={2} />
  }
  if (seguimiento.error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar el análisis" mensaje={seguimiento.error.message}>
        <Boton icono={RefreshCw} onClick={seguimiento.reanudar}>Intentar de nuevo</Boton>
      </EstadoVista>
    )
  }

  const pestanas = [
    { clave: 'resumen', etiqueta: 'Resumen', Icono: LayoutDashboard },
    { clave: 'comentarios', etiqueta: 'Comentarios', Icono: MessageSquareQuote },
    { clave: 'revision', etiqueta: 'Revisión', Icono: Sparkles, contador: porDecidir },
  ]

  return (
    <div className="analisis-docente">
      <FranjaEstado
        solicitud={solicitud}
        agotado={seguimiento.agotado}
        esCoordinador={esCoordinador}
        enviando={enviando}
        publicando={publicando}
        onSolicitar={solicitar}
        onPublicar={publicar}
        onRevisar={() => setPestanaElegida('revision')}
        onReanudar={seguimiento.reanudar}
      />
      <ErrorApi error={errorAccion} />

      <section className="analisis-docente__tarjeta">
        <div className="analisis-docente__pestanas" role="tablist" aria-label="Secciones del análisis">
          {pestanas.map(({ clave, etiqueta, Icono, contador }) => (
            <button
              key={clave}
              id={`analisis-${clave}`}
              type="button"
              role="tab"
              aria-selected={pestana === clave}
              aria-controls="analisis-contenido"
              className={`analisis-docente__pestana${pestana === clave ? ' analisis-docente__pestana--activa' : ''}`}
              onClick={() => setPestanaElegida(clave)}
            >
              <Icono size={16} aria-hidden="true" />
              {etiqueta}
              {contador > 0 && (
                <span className="analisis-docente__contador">
                  {contador}
                  <span className="solo-lectores"> por decidir</span>
                </span>
              )}
            </button>
          ))}
        </div>
        <div id="analisis-contenido" className="analisis-docente__contenido" role="tabpanel" aria-labelledby={`analisis-${pestana}`}>
          {/* La key recarga el resultado y los comentarios cuando cambia la solicitud. */}
          <div hidden={pestana !== 'resumen'}>
            <ResultadoComentarios
              key={idSolicitud ?? 'sin'}
              compacto
              idDocente={idDocente}
              periodoId={periodoId}
              esCoordinador={esCoordinador}
              criterioActivo={criterio}
              onVersion={setVersion}
              onFiltrarCriterio={(c) => {
                setCriterio(c)
                if (c) setPestanaElegida('comentarios')
              }}
            />
          </div>
          {pestana === 'comentarios' && (
            <ComentariosDocente
              key={version ?? 'sin-resultado'}
              idDocente={idDocente}
              periodoId={periodoId}
              criterio={criterio}
              onCriterio={setCriterio}
            />
          )}
          {pestana === 'revision' &&
            (solicitud ? (
              <Revision solicitud={solicitud} puedeValidar={esCoordinador} onValidado={seguimiento.reanudar} />
            ) : (
              <p className="revision-ia__vacio">Cuando se solicite el análisis, aquí se revisa cada hallazgo antes de publicarlo.</p>
            ))}
        </div>
      </section>
    </div>
  )
}
