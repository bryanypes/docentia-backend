import { ArrowRight } from 'lucide-react'
import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { MINIMO_COMENTARIOS, TIPOS_HALLAZGO } from '../estados.js'
import { listarSolicitudes, obtenerPanorama } from '../services/analisisService.js'
import { MensajeIA } from './DocenteIA.jsx'
import './ComentariosParaCoevaluar.css'

/**
 * Antes de coevaluar: cuántos comentarios dejaron los estudiantes y, si ya se analizaron, lo que
 * dicen por criterio de la evaluación. Nunca muestra comentarios textuales. Si la coevaluación ya
 * se envió sin análisis, pide rectificarla con él.
 */
export function ComentariosParaCoevaluar({ idDocente, periodoId, enviada }) {
  const cargarPanorama = useCallback(() => obtenerPanorama(periodoId), [periodoId])
  const panorama = useFetch(cargarPanorama, [cargarPanorama])
  const cargarUltima = useCallback(
    () => listarSolicitudes(idDocente, periodoId).then((lista) => lista[0] ?? null),
    [idDocente, periodoId],
  )
  const ultima = useFetch(cargarUltima, [cargarUltima])

  if (!panorama.datos || ultima.cargando) return null
  const fila = panorama.datos.docentes.find((f) => String(f.docente.idDocente) === String(idDocente))
  if (!fila) return null

  const analizado = ultima.datos?.estado === 'COMPLETADO'
  // Con análisis, cuenta lo que ese análisis leyó; sin él, lo que hay hoy.
  const comentarios = analizado ? (ultima.datos.comentariosRecibidos ?? fila.comentarios) : fila.comentarios
  const hallazgos = (ultima.datos?.borrador?.hallazgos ?? []).filter((h) => h.estado !== 'DESCARTADO')
  const enlace = `/analisis?docente=${encodeURIComponent(idDocente)}&periodo=${encodeURIComponent(periodoId)}`
  const textoComentarios =
    comentarios === 0
      ? 'Sus estudiantes todavía no han escrito comentarios en este periodo.'
      : `Sus estudiantes han escrito ${comentarios} ${comentarios === 1 ? 'comentario' : 'comentarios'} en este periodo.`

  if (!analizado) {
    const alcanza = comentarios >= MINIMO_COMENTARIOS
    return (
      <MensajeIA
        robot="senala"
        tono={enviada ? 'alerta' : 'violeta'}
        titulo={
          enviada
            ? 'Esta coevaluación se hizo sin el análisis de comentarios'
            : 'Primero revisa el análisis de comentarios'
        }
      >
        <p>
          {textoComentarios}{' '}
          {alcanza
            ? 'Todavía no los he analizado.'
            : `Hacen falta al menos ${MINIMO_COMENTARIOS} para analizarlos.`}
        </p>
        {enviada ? (
          <p>
            Analízalos y, si lo que dicen los estudiantes cambia tu valoración, rectifica esta calificación con
            «Solicitar corrección».
          </p>
        ) : (
          <p>Te recomiendo leer lo que dicen los estudiantes antes de calificar.</p>
        )}
        {alcanza && (
          <div>
            <Link to={enlace} className="boton boton--primario">
              Ir al análisis de comentarios <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </div>
        )}
      </MensajeIA>
    )
  }

  return (
    <MensajeIA robot="aprobada" tono="turquesa" titulo="Lo que dicen sus estudiantes">
      <p>
        {textoComentarios} Los agrupé por criterio de la evaluación; tenlo en cuenta al calificar.
      </p>
      {TIPOS_HALLAZGO.map(({ tipo, nombre }) => {
        const delTipo = hallazgos.filter((h) => h.tipo === tipo)
        if (delTipo.length === 0) return null
        return (
          <div key={tipo} className="coevaluar-ia__grupo">
            <strong>{nombre}</strong>
            <ul>
              {delTipo.map((h) => (
                <li key={h.idHallazgo}>
                  {h.nombreCriterio && <span className="coevaluar-ia__criterio">{h.nombreCriterio}</span>}
                  {h.texto}
                  <small>
                    {' '}
                    · {h.citas.length} {h.citas.length === 1 ? 'comentario' : 'comentarios'}
                  </small>
                </li>
              ))}
            </ul>
          </div>
        )
      })}
      {enviada && (
        <p className="coevaluar-ia__nota">
          Si lo que dicen los estudiantes cambia tu valoración, puedes rectificar con «Solicitar corrección».
        </p>
      )}
      <div>
        <Link to={enlace} className="boton boton--contorno">
          Ver el análisis completo
        </Link>
      </div>
    </MensajeIA>
  )
}
