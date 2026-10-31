import { MessagesSquare } from 'lucide-react'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { TIPOS_HALLAZGO } from '../estados.js'
import { obtenerResultadoPropio } from '../services/analisisService.js'

/** Lo que el docente ve del análisis: solo lo que la coordinación incluyó y publicó. */
export function PanelHallazgosPropios() {
  const { datos, error } = useFetch(obtenerResultadoPropio)
  const hallazgos = datos?.hallazgos ?? []

  return (
    <Panel titulo="Lo que dicen tus estudiantes" icono={MessagesSquare}>
      <ErrorApi error={error} />
      {datos && hallazgos.length === 0 && (
        <p>
          La coordinación revisa los comentarios de tus estudiantes. Verás las fortalezas y oportunidades de mejora
          cuando las publique.
        </p>
      )}
      {TIPOS_HALLAZGO.map(({ tipo, nombre }) => {
        const delTipo = hallazgos.filter((hallazgo) => hallazgo.tipo === tipo)
        if (delTipo.length === 0) return null
        return (
          <div key={tipo}>
            <h3 className="acta__subtitulo">{nombre}</h3>
            <ul>
              {delTipo.map((hallazgo) => (
                <li key={hallazgo.texto}>{hallazgo.texto}</li>
              ))}
            </ul>
          </div>
        )
      })}
    </Panel>
  )
}
