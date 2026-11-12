import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import './TablaCobertura.css'

function porcentaje(valor) {
  return `${Number(valor).toLocaleString('es-CO')} %`
}

/** Semáforo de la cobertura: el color acompaña al número, nunca lo reemplaza. */
function tonoCobertura(valor) {
  const n = Number(valor)
  if (n >= 80) return 'alta'
  if (n >= 50) return 'media'
  return 'baja'
}

/**
 * Cobertura como barras con semáforo: matriculados, respondieron y porcentaje. Solo cuenta; nunca
 * dice quién respondió. Con `conEnlace` cada docente lleva a su expediente.
 */
export function TablaCobertura({ filas, primeraColumna, conEnlace = false }) {
  return (
    <ul className="cobertura" aria-label={`Cobertura por ${primeraColumna.toLowerCase()}`}>
      {filas.map((f) => {
        const tono = tonoCobertura(f.porcentaje)
        const faltan = Math.max(0, f.matriculados - f.respondieron)
        return (
          <li key={`${f.idDocente ?? ''}-${f.nombre}`} className="cobertura__fila">
            <div className="cobertura__nombre">
              <strong>{f.nombre}</strong>
              <span>
                {f.respondieron} de {f.matriculados} respondieron{faltan ? ` · faltan ${faltan}` : ''}
              </span>
            </div>
            <div className="cobertura__medida">
              <span className="cobertura__riel" aria-hidden="true">
                <span className={`cobertura__relleno cobertura__relleno--${tono}`} style={{ width: `${Math.min(100, Number(f.porcentaje))}%` }} />
              </span>
              <span className={`cobertura__valor cobertura__valor--${tono}`}>{porcentaje(f.porcentaje)}</span>
            </div>
            {conEnlace && f.idDocente && (
              <BotonAccion tipo="ver" nombre={f.nombre} a={`/docentes/${f.idDocente}`} />
            )}
          </li>
        )
      })}
    </ul>
  )
}
