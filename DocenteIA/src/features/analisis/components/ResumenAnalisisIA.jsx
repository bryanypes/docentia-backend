import { ChevronDown, CircleCheck, ExternalLink, MessageSquareQuote, Sparkles, TriangleAlert } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { ESTADO_ANALISIS } from '../estados.js'
import { obtenerAnalisisDocente, solicitarAnalisis } from '../services/analisisService.js'
import { ComentariosDocente } from './ComentariosDocente.jsx'
import { MensajeIA } from './DocenteIA.jsx'
import './ResumenAnalisisIA.css'

const MAXIMO_TEMAS = 3
const SONDEO_MS = 4000
const EN_CURSO = ['EN_COLA', 'EN_PROCESO']
// Estados en los que la coordinación puede pedir el análisis desde aquí mismo.
const SE_PUEDE_PEDIR = ['NO_DISPONIBLE_AUN', 'INSUFICIENTE', 'ERROR', 'SIN_COMENTARIOS']

function ListaTemas({ titulo, Icono, tono, temas, vacio }) {
  return (
    <section className={`resumen-ia__grupo resumen-ia__grupo--${tono}`}>
      <h3>
        <Icono size={15} aria-hidden="true" />
        {titulo}
      </h3>
      {temas.length === 0 ? (
        <p className="resumen-ia__vacio">{vacio}</p>
      ) : (
        <ul>
          {temas.slice(0, MAXIMO_TEMAS).map((t) => (
            <li key={`${t.criterio}-${t.descriptor}`}>
              <span>{t.nombre}</span>
              <small>
                {t.frecuencia} {t.frecuencia === 1 ? 'comentario' : 'comentarios'}
              </small>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Resumen corto del análisis de los comentarios de los estudiantes: sentimiento, las principales
 * fortalezas y oportunidades, y un botón que despliega los comentarios. El detalle completo (citas,
 * tendencias, validación) vive en Análisis de comentarios. La coordinación puede solicitarlo aquí;
 * mientras se genera, el resumen se actualiza solo.
 */
export function ResumenAnalisisIA({ idDocente, periodoId, esCoordinador = false }) {
  const cargar = useCallback(
    () => (periodoId ? obtenerAnalisisDocente(idDocente, periodoId) : Promise.resolve(null)),
    [idDocente, periodoId],
  )
  const analisis = useFetch(cargar, [cargar])
  const [verComentarios, setVerComentarios] = useState(false)
  const [criterio, setCriterio] = useState(null)
  const datos = analisis.datos
  const r = datos?.resultado
  const { recargar } = analisis
  const [enviando, setEnviando] = useState(false)
  const [errorSolicitar, setErrorSolicitar] = useState(null)

  useEffect(() => {
    if (!datos || !EN_CURSO.includes(datos.estado)) return undefined
    const temporizador = setTimeout(recargar, SONDEO_MS)
    return () => clearTimeout(temporizador)
  }, [datos, recargar])

  async function solicitar() {
    setEnviando(true)
    setErrorSolicitar(null)
    try {
      await solicitarAnalisis(idDocente, periodoId)
      recargar()
    } catch (error) {
      setErrorSolicitar(error)
    } finally {
      setEnviando(false)
    }
  }
  const puedePedir = esCoordinador && datos && !EN_CURSO.includes(datos.estado) && SE_PUEDE_PEDIR.includes(datos.estado)
  const enlace = `/analisis?docente=${encodeURIComponent(idDocente)}&periodo=${encodeURIComponent(periodoId ?? '')}`

  let contenido
  if (!periodoId) {
    contenido = <p className="resumen-ia__vacio">No hay un periodo activo para consultar el análisis.</p>
  } else if (analisis.cargando && !datos) {
    contenido = <p className="resumen-ia__vacio">Consultando el análisis…</p>
  } else if (analisis.error) {
    contenido = (
      <MensajeIA robot="error" tono="coral" tamano="compacto" titulo="No fue posible consultar el análisis" role="alert">
        <p>{analisis.error.message}</p>
      </MensajeIA>
    )
  } else if (datos && !r) {
    const estado = ESTADO_ANALISIS[datos.estado] ?? ESTADO_ANALISIS.NO_DISPONIBLE_AUN
    contenido = (
      <MensajeIA robot={estado.robot} tono={estado.tono} tamano="compacto" titulo={estado.titulo} role="status">
        <p>
          {datos.mensaje}
        </p>
      </MensajeIA>
    )
  } else if (r) {
    const indice = Math.round(r.sentimientoGlobal.indice * 100)
    contenido = (
      <>
        <div className="resumen-ia__cifras">
          <span>
            <strong>{r.nComentarios}</strong> comentarios analizados
          </span>
          <span className={indice >= 0 ? 'resumen-ia__positivo' : 'resumen-ia__negativo'}>
            Sentimiento <strong>{indice > 0 ? `+${indice}` : indice}</strong>
          </span>
          <span>
            {r.sentimientoGlobal.positivos} positivos · {r.sentimientoGlobal.negativos} negativos
          </span>
          {datos.estadoHallazgos === 'PENDIENTE_VALIDACION' && (
            <span className="resumen-ia__rotulo">Pendiente de validación por coordinación</span>
          )}
        </div>
        <div className="resumen-ia__grupos">
          <ListaTemas
            titulo="Fortalezas"
            Icono={CircleCheck}
            tono="turquesa"
            temas={r.aspectos.fortalezas}
            vacio="No hay fortalezas recurrentes."
          />
          <ListaTemas
            titulo="Oportunidades de mejora"
            Icono={TriangleAlert}
            tono="coral"
            temas={r.aspectos.oportunidades}
            vacio="No hay oportunidades recurrentes."
          />
        </div>
      </>
    )
  }

  return (
    <Panel
      className="resumen-ia"
      titulo="Análisis IA de los comentarios de estudiantes"
      descripcion={
        r?.tipoModelo === 'LLM'
          ? 'Propuesto por un modelo de lenguaje a partir de lo que escribieron los estudiantes.'
          : 'Análisis automático de lo que escribieron los estudiantes al final de su encuesta.'
      }
      icono={Sparkles}
      tono="violeta"
    >
      {contenido}
      <ErrorApi error={errorSolicitar} />
      <div className="resumen-ia__acciones">
        {puedePedir && (
          <Boton icono={Sparkles} onClick={solicitar} disabled={enviando}>
            {enviando ? 'Solicitando…' : datos.estado === 'NO_DISPONIBLE_AUN' ? 'Solicitar análisis' : 'Solicitar de nuevo'}
          </Boton>
        )}
        {r && (
          <Boton
            variante="contorno"
            icono={verComentarios ? ChevronDown : MessageSquareQuote}
            aria-expanded={verComentarios}
            onClick={() => setVerComentarios((v) => !v)}
          >
            {verComentarios ? 'Ocultar comentarios' : 'Ver comentarios de estudiantes'}
          </Boton>
        )}
        {periodoId && (
          <Link to={enlace} className="resumen-ia__enlace">
            Abrir el análisis completo <ExternalLink size={14} aria-hidden="true" />
          </Link>
        )}
      </div>
      {verComentarios && r && (
        <ComentariosDocente idDocente={idDocente} periodoId={periodoId} criterio={criterio} onCriterio={setCriterio} />
      )}
    </Panel>
  )
}
