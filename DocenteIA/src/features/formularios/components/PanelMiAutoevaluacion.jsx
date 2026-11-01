import { ClipboardCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota } from '../../../shared/utils/formatters.js'
import { obtenerAutoevaluacion } from '../services/formulariosService.js'

function media(criterio) {
  const valores = criterio.descriptores.map(({ valor }) => valor).filter((valor) => valor !== null)
  return valores.length === 0 ? null : valores.reduce((suma, valor) => suma + valor, 0) / valores.length
}

/** Desglose por criterio de lo que el docente respondió. Nunca incluye otras fuentes (A4). */
export function PanelMiAutoevaluacion() {
  const { datos, error } = useFetch(obtenerAutoevaluacion)
  if (error || !datos) return null

  return (
    <Panel titulo="Mi autoevaluación por criterio" descripcion="Solo tus propias respuestas." icono={ClipboardCheck}>
      {datos.respondidos === 0 ? (
        <p>
          Aún no la has respondido. <Link to="/mi-autoevaluacion">Ir a mi autoevaluación</Link>
        </p>
      ) : (
        <ul className="dimensiones">
          {datos.criterios.map((criterio) => (
            <li key={criterio.codigo}>
              <div className="dimensiones__fila">
                <span>{criterio.nombre}</span>
                <strong>{formatearNota(media(criterio))}</strong>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}
