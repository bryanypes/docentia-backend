import { nivelDeNota, porcentajeNota } from '../../../utils/formatters.js'
import './BarraNota.css'

/** Barra horizontal de una nota sobre 5, coloreada por nivel de desempeño. */
export function BarraNota({ valor, grosor = 'media' }) {
  const nivel = nivelDeNota(valor)
  return (
    <div className={`barra-nota barra-nota--${grosor}`} aria-hidden="true">
      <div
        className={`barra-nota__relleno barra-nota__relleno--${nivel?.clase ?? 'vacio'}`}
        style={{ width: porcentajeNota(valor) }}
      />
    </div>
  )
}
