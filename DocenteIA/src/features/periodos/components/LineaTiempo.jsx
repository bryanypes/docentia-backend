import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { obtenerCalendario } from '../services/periodosService.js'
import './LineaTiempo.css'

/** Las cuatro etapas del periodo activo con sus semanas de la Resolución 074. */
export function LineaTiempo() {
  const { datos } = useFetch(obtenerCalendario)
  if (!datos) return null

  return (
    <section className="linea-tiempo" aria-label="Calendario del periodo">
      <ol className="linea-tiempo__etapas">
        {datos.etapas.map((etapa) => {
          const vigente = etapa.etapa === datos.etapaVigente?.etapa
          return (
            <li key={etapa.etapa} className={vigente ? 'linea-tiempo__etapa linea-tiempo__etapa--vigente' : 'linea-tiempo__etapa'}>
              <strong>{etapa.nombreEtapa}</strong>
              <span>{etapa.semanasResolucion}</span>
              <span>
                {etapa.fechaApertura
                  ? `${formatearFecha(etapa.fechaApertura)} a ${formatearFecha(etapa.fechaCierre)}`
                  : 'Sin programar'}
              </span>
              {vigente && <span className="pildora pildora--info">Etapa vigente</span>}
            </li>
          )
        })}
      </ol>
      <p className="campo__ayuda">{datos.seguimiento}</p>
    </section>
  )
}
