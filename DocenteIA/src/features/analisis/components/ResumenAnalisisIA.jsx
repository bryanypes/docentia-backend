import { ChevronDown, CircleCheck, ExternalLink, MessageSquareQuote, Sparkles, TriangleAlert } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { ESTADO_ANALISIS } from '../estados.js'
import { obtenerAnalisisDocente } from '../services/analisisService.js'
import { ComentariosDocente } from './ComentariosDocente.jsx'
import { MensajeIA } from './DocenteIA.jsx'
import './ResumenAnalisisIA.css'

const MAXIMO_TEMAS = 3

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
 * tendencias, validación) vive en Análisis de comentarios.
 */
export function ResumenAnalisisIA({ idDocente, periodoId, className = '' }) {
  const cargar = useCallback(
    () => (periodoId ? obtenerAnalisisDocente(idDocente, periodoId) : Promise.resolve(null)),
    [idDocente, periodoId],
  )
  const analisis = useFetch(cargar, [cargar])
  const [verComentarios, setVerComentarios] = useState(false)
  const [criterio, setCriterio] = useState(null)
  const datos = analisis.datos
  const r = datos?.resultado
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
          {datos.motivo === 'VENTANA_ABIERTA' ? ' Se puede pedir cuando cierre la recolección de calificaciones.' : ''}
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
      className={`resumen-ia ${className}`}
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
      <div className="resumen-ia__acciones">
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
