import { CircleCheck, MessageSquareText, MonitorSmartphone, ShieldCheck } from 'lucide-react'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import './ResumenEstudiante.css'

/**
 * Resumen de una evaluación ya enviada. Con la copia local se ven las respuestas por criterio y el
 * comentario; sin ella (otro equipo o almacenamiento borrado) queda un comprobante.
 */
export function ResumenEstudiante({ formulario, copia }) {
  const etiquetaDe = (valor) => formulario.escala.find((e) => e.valor === valor)?.etiqueta ?? '—'

  return (
    <div className="resumen-estudiante">
      <Panel
        icono={CircleCheck}
        tono="turquesa"
        titulo="Evaluación enviada"
        descripcion={`${formulario.docente} · ${formulario.curso} · Grupo ${formulario.grupo}`}
      >
        <ul className="resumen-estudiante__datos">
          <li>
            <ShieldCheck size={16} aria-hidden="true" />
            Quedó registrada de forma anónima: nadie puede ver qué respondiste.
          </li>
          {copia ? (
            <li>
              <MonitorSmartphone size={16} aria-hidden="true" />
              Enviada el {formatearFechaHora(copia.enviadaEn)}. Tus respuestas se guardaron solo en este equipo para que
              puedas repasarlas.
            </li>
          ) : (
            <li>
              <MonitorSmartphone size={16} aria-hidden="true" />
              Tus respuestas no se guardan en el servidor para proteger tu anonimato. Solo las verás desde el equipo con
              el que la enviaste.
            </li>
          )}
        </ul>
      </Panel>

      {copia &&
        formulario.criterios.map((criterio) => (
          <Panel key={criterio.codigo} titulo={criterio.nombre}>
            <ol className="resumen-estudiante__items">
              {criterio.descriptores.map((d) => {
                const valor = copia.respuestas[d.codigo]
                return (
                  <li key={d.codigo}>
                    <span>{d.texto}</span>
                    <span className={`resumen-estudiante__valor resumen-estudiante__valor--${valor ?? 'vacio'}`}>
                      <strong>{valor ?? '—'}</strong> {valor ? etiquetaDe(valor) : 'Sin respuesta'}
                    </span>
                  </li>
                )
              })}
            </ol>
          </Panel>
        ))}

      {copia?.comentario && (
        <Panel icono={MessageSquareText} titulo="Tu comentario">
          <p className="resumen-estudiante__comentario">{copia.comentario}</p>
        </Panel>
      )}
    </div>
  )
}
