import { BarraNota } from '../../../shared/components/ui/BarraNota/BarraNota.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_FUENTE, ORIGEN_FUENTE } from '../../../shared/constants/dominio.js'
import {
  formatearFecha,
  formatearNota,
  formatearPeso,
  listarFuentes,
  nivelDeNota,
} from '../../../shared/utils/formatters.js'
import './PanelesExpediente.css'

/** Valor del promedio ponderado. Si está incompleto no muestra ningún número parcial. */
export function ValorPromedio({ promedio, tamano = 'grande' }) {
  if (!promedio || promedio.estado === 'INCOMPLETO') {
    return <strong className={`valor-promedio valor-promedio--${tamano} valor-promedio--pendiente`}>Pendiente</strong>
  }
  const nivel = nivelDeNota(promedio.valor)
  return (
    <strong className={`valor-promedio valor-promedio--${tamano} valor-promedio--${nivel.clase}`}>
      {formatearNota(promedio.valor)}
    </strong>
  )
}

/** Nota de cada fuente con su peso y el promedio ponderado. Solo para coordinación y vicerrectoría. */
export function PanelFuentes({ notasPorFuente, promedio, descripcion, children }) {
  return (
    <Panel titulo="Calificación por fuente" descripcion={descripcion}>
      <ul className="fuentes">
        {notasPorFuente.map((f) => (
          <li key={f.fuente} className="fuentes__fila">
            <span className="fuentes__nombre">
              <strong>{ETIQUETA_FUENTE[f.fuente]}</strong>
              <span>{f.nota === null ? 'Sin registrar' : ORIGEN_FUENTE[f.fuente]}</span>
            </span>
            <span className="fuentes__peso">{formatearPeso(f.peso)}</span>
            <strong className="fuentes__nota">{formatearNota(f.nota)}</strong>
          </li>
        ))}
      </ul>
      <div className="fuentes__total">
        <span>Promedio ponderado</span>
        <ValorPromedio promedio={promedio} />
      </div>
      {promedio?.estado === 'INCOMPLETO' && (
        <p className="fuentes__nota-pie">Falta la calificación de {listarFuentes(promedio.fuentesPendientes)}.</p>
      )}
      {children}
    </Panel>
  )
}

/** Resultado ya ponderado de cada dimensión. Sirve para la vista del docente y la completa. */
export function PanelDimensiones({ titulo = 'Resultado por dimensión', dimensiones }) {
  return (
    <Panel titulo={titulo} descripcion="Resultado ponderado entre las tres fuentes.">
      {dimensiones.length === 0 ? (
        <p className="expediente__vacio">Aún no hay resultados en el periodo activo.</p>
      ) : (
        <ul className="dimensiones">
          {dimensiones.map((d) => (
            <li key={d.dimension}>
              <div className="dimensiones__fila">
                <span>{d.nombre}</span>
                <strong>{d.estado === 'COMPLETO' ? formatearNota(d.resultado) : 'Pendiente'}</strong>
              </div>
              <BarraNota valor={d.estado === 'COMPLETO' ? d.resultado : null} />
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}

const ALTO_GRAFICO = 110
// La escala arranca en 3 para que se noten las diferencias entre periodos; las notas menores
// conservan una barra mínima y su valor siempre va escrito encima.
const NOTA_MINIMA_GRAFICO = 3
const ALTO_MINIMO_BARRA = 6

/** Barras con el promedio de los periodos anteriores en los que el docente fue calificado. */
export function PanelHistorico({ historico }) {
  const completos = historico.filter((h) => h.promedio.estado === 'COMPLETO')
  const ancho = Math.max(400, completos.length * 64 + 20)

  return (
    <Panel titulo="Histórico de evaluaciones" descripcion="Promedio ponderado de periodos anteriores.">
      {completos.length === 0 ? (
        <p className="expediente__vacio">El docente no tiene periodos anteriores con promedio completo.</p>
      ) : (
        <>
          <svg viewBox={`0 0 ${ancho} 140`} className="historico" aria-hidden="true">
            <line x1="0" y1={ALTO_GRAFICO} x2={ancho} y2={ALTO_GRAFICO} className="historico__eje" />
            {completos.map((h, i) => {
              const alto = Math.max(
                ALTO_MINIMO_BARRA,
                ((h.promedio.valor - NOTA_MINIMA_GRAFICO) / (5 - NOTA_MINIMA_GRAFICO)) * 88,
              )
              const x = 14 + i * 64
              const ultimo = i === completos.length - 1
              return (
                <g key={h.periodo.idPeriodo}>
                  <rect
                    x={x}
                    y={ALTO_GRAFICO - alto}
                    width="44"
                    height={alto}
                    rx="4"
                    className={ultimo ? 'historico__barra historico__barra--actual' : 'historico__barra'}
                  />
                  <text x={x + 22} y={ALTO_GRAFICO - alto - 6} className="historico__valor">
                    {formatearNota(h.promedio.valor)}
                  </text>
                  <text x={x + 22} y="128" className="historico__periodo">
                    {h.periodo.nombre}
                  </text>
                </g>
              )
            })}
          </svg>
          <table className="solo-lectores">
            <caption>Histórico de evaluaciones</caption>
            <tbody>
              {completos.map((h) => (
                <tr key={h.periodo.idPeriodo}>
                  <th scope="row">{h.periodo.nombre}</th>
                  <td>{formatearNota(h.promedio.valor)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </Panel>
  )
}

export function PanelCompromisos({ titulo = 'Compromisos vigentes', compromisos }) {
  return (
    <Panel titulo={titulo}>
      {compromisos.length === 0 ? (
        <p className="expediente__vacio">No hay compromisos de mejora vigentes.</p>
      ) : (
        <ul className="compromisos">
          {compromisos.map((c) => (
            <li key={c.idCompromiso} className="compromisos__item">
              <p className="compromisos__descripcion">{c.descripcion}</p>
              <div className="compromisos__meta">
                Vence {formatearFecha(c.fechaLimite)} · Periodo {c.nombrePeriodo}
              </div>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}

/** Fila de datos clave del docente (programa, facultad, periodo, estado). */
export function DatosDocente({ docente, periodo, promedio }) {
  const datos = [
    { clave: 'Programa', valor: docente.nombrePrograma },
    { clave: 'Facultad', valor: docente.nombreFacultad },
    { clave: 'Periodo', valor: periodo?.nombre ?? 'Sin periodo activo' },
    { clave: 'Estado', valor: <EtiquetaNivel promedio={promedio} /> },
  ]
  return (
    <dl className="datos-docente">
      {datos.map((d) => (
        <div key={d.clave} className="datos-docente__item">
          <dt className="rotulo">{d.clave}</dt>
          <dd>{d.valor}</dd>
        </div>
      ))}
    </dl>
  )
}
