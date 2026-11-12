import { Building2, Filter } from 'lucide-react'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { formatearNota } from '../../../shared/utils/formatters.js'
import './PanelesUniversidad.css'

const TONO_PASO = { completos: 'azul', cita: 'violeta', reunion: 'cielo', acta: 'turquesa' }

/** Embudo del proceso: cuánto del camino de la evaluación al acta firmada ya se recorrió. */
export function EmbudoProceso({ pasos, className = 'estadisticas__ancho' }) {
  return (
    <Panel
      className={`embudo-panel ${className}`}
      titulo="Del resultado al acta firmada"
      descripcion="De los docentes con evaluación completa, cuántos llegaron a cada paso."
      icono={Filter}
      tono="violeta"
    >
      <ol className="embudo">
        {pasos.map((paso, i) => (
          <li key={paso.clave} className={`embudo__paso embudo__paso--${TONO_PASO[paso.clave]}`}>
            <span className="embudo__etiqueta">
              <span className="embudo__numero">{i + 1}</span>
              {paso.etiqueta}
            </span>
            <span className="embudo__cifra">
              <strong>{paso.cantidad}</strong>
              <span>{paso.porcentaje} %</span>
            </span>
            <span className="embudo__riel" aria-hidden="true">
              <span style={{ width: `${paso.porcentaje}%` }} />
            </span>
          </li>
        ))}
      </ol>
      {pasos[0].cantidad > 0 && (
        <p className="embudo__nota">
          {pasos[0].cantidad - pasos[3].cantidad === 0
            ? 'Todos los docentes evaluados ya tienen el acta firmada.'
            : `Faltan ${pasos[0].cantidad - pasos[3].cantidad} actas por firmar entre los docentes evaluados.`}
        </p>
      )}
    </Panel>
  )
}

/** Cada facultad en una fila: docentes, promedio, avance, bajo el umbral y actas firmadas. */
export function PanoramaFacultades({ facultades, umbral, onElegir, className = 'estadisticas__ancho' }) {
  return (
    <Panel
      className={className}
      titulo="Panorama por facultad"
      descripcion={`Promedio y avance de cada facultad, cuántos docentes quedan bajo ${formatearNota(umbral)} y cuántas actas se firmaron. Pulsa una facultad para filtrar.`}
      icono={Building2}
      tono="azul"
    >
      <div className="tabla-contenedor">
        <table className="tabla tabla--apilable panorama-facultades">
          <thead>
            <tr>
              <th scope="col">Facultad</th>
              <th scope="col">Promedio</th>
              <th scope="col">Avance</th>
              <th scope="col">Bajo el umbral</th>
              <th scope="col">Actas firmadas</th>
            </tr>
          </thead>
          <tbody>
            {facultades.map((f) => (
              <tr key={f.nombre}>
                <th scope="row">
                  <button type="button" className="panorama-facultades__nombre" onClick={() => onElegir(f)}>
                    {f.nombre}
                    <span>
                      {f.docentes} {f.docentes === 1 ? 'docente' : 'docentes'}
                    </span>
                  </button>
                </th>
                <td data-etiqueta="Promedio">
                  <strong className="panorama-facultades__promedio">{f.promedio === null ? '—' : formatearNota(f.promedio)}</strong>
                </td>
                <td data-etiqueta="Avance">
                  <span className="panorama-facultades__avance">
                    <span aria-hidden="true">
                      <span style={{ width: `${f.avance}%` }} />
                    </span>
                    {f.avance} %
                  </span>
                </td>
                <td data-etiqueta="Bajo el umbral">
                  <span className={f.bajoUmbral ? 'pildora pildora--alerta' : 'pildora pildora--exito'}>
                    {f.bajoUmbral} {f.bajoUmbral === 1 ? 'docente' : 'docentes'}
                  </span>
                </td>
                <td data-etiqueta="Actas firmadas">
                  {f.actasFirmadas} de {f.completos}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Panel>
  )
}
