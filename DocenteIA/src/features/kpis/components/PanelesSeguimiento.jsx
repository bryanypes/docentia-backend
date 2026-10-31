import {
  ArrowRight,
  Building2,
  ChartBarStacked,
  CircleCheck,
  ClipboardCheck,
  GraduationCap,
  HeartHandshake,
  Landmark,
  Users,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { GraficoBarras } from '../../../shared/components/ui/GraficoBarras/GraficoBarras.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_FUENTE } from '../../../shared/constants/dominio.js'
import { formatearNota, iniciales } from '../../../shared/utils/formatters.js'
import { FUENTES, UMBRAL_ATENCION } from '../seguimiento.js'
import './PanelesSeguimiento.css'

const MAXIMO_ATENCION = 5
const ICONO_FUENTE = { ESTUDIANTES: GraduationCap, COORDINACION: Users, VICERRECTORIA: Landmark }

const porcentaje = (parte, total) => (total ? Math.round((parte / total) * 100) : 0)
const docentesTexto = (n) => (n === 1 ? '1 docente' : `${n} docentes`)

/** Anillo de avance: el riel es un paso más claro del mismo azul que el relleno. */
function Anillo({ valor }) {
  const radio = 52
  const circunferencia = 2 * Math.PI * radio
  return (
    <svg viewBox="0 0 128 128" className="anillo" aria-hidden="true">
      <circle cx="64" cy="64" r={radio} className="anillo__riel" />
      <circle
        cx="64"
        cy="64"
        r={radio}
        className="anillo__relleno"
        strokeDasharray={circunferencia}
        strokeDashoffset={circunferencia * (1 - valor / 100)}
      />
      <text x="64" y="62" className="anillo__valor">
        {valor} %
      </text>
      <text x="64" y="82" className="anillo__texto">
        completo
      </text>
    </svg>
  )
}

/** Cuántos docentes ya tienen las tres calificaciones y qué falta para cerrar el periodo. */
export function PanelAvance({ analisis, miFuente, periodo }) {
  const { total, completos, faltanPorFuente } = analisis
  const avance = porcentaje(completos, total)

  return (
    <Panel
      icono={ClipboardCheck}
      titulo="Avance de la evaluación"
      descripcion={`Un docente queda evaluado cuando tiene las tres calificaciones de ${periodo.nombre}.`}
    >
      <div
        className="avance"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={total}
        aria-valuenow={completos}
        aria-valuetext={`${completos} de ${total} docentes con evaluación completa, ${avance} %`}
        aria-label="Avance de la evaluación"
      >
        <Anillo valor={avance} />
        <div className="avance__resumen">
          <p className="avance__cifra">
            <strong>{completos}</strong> de {docentesTexto(total)}
          </p>
          <p className="avance__detalle">con evaluación completa</p>
        </div>
      </div>

      <h3 className="avance__subtitulo">Calificaciones que faltan</h3>
      <ul className="avance__faltantes">
        {FUENTES.map((fuente) => {
          const Icono = ICONO_FUENTE[fuente]
          const faltan = faltanPorFuente[fuente]
          const mia = fuente === miFuente
          return (
            <li key={fuente} className={`avance__fuente${mia ? ' avance__fuente--mia' : ''}${faltan === 0 ? ' avance__fuente--lista' : ''}`}>
              <span className="avance__fuente-icono" aria-hidden="true">
                {faltan === 0 ? <CircleCheck size={17} /> : <Icono size={17} />}
              </span>
              <span className="avance__fuente-nombre">
                {ETIQUETA_FUENTE[fuente]}
                {mia && <small>la tuya</small>}
              </span>
              <strong>{faltan === 0 ? 'Completa' : `Faltan ${faltan}`}</strong>
            </li>
          )
        })}
      </ul>

    </Panel>
  )
}

/**
 * Parte de un todo: una barra apilada con los cuatro niveles. La leyenda lleva siempre nombre,
 * cantidad y porcentaje (el color nunca va solo); al pasar por un nivel se resalta su segmento.
 */
export function PanelDistribucion({ analisis }) {
  const { completos, porNivel } = analisis
  const [activo, setActivo] = useState(null)

  return (
    <Panel
      icono={ChartBarStacked}
      titulo="Cómo les fue a los docentes"
      descripcion={`Los ${completos} docentes con evaluación completa, según el nivel de su promedio.`}
    >
      {completos === 0 ? (
        <p className="paneles__vacio">Aún ningún docente tiene las tres calificaciones.</p>
      ) : (
        <>
          <div className="distribucion__barra" aria-hidden="true">
            {porNivel
              .filter((n) => n.cantidad > 0)
              .map((n) => (
                <span
                  key={n.clase}
                  className={`distribucion__segmento distribucion__segmento--${n.clase}${activo && activo !== n.clase ? ' distribucion__segmento--atenuado' : ''}`}
                  style={{ flexGrow: n.cantidad }}
                  onMouseEnter={() => setActivo(n.clase)}
                  onMouseLeave={() => setActivo(null)}
                />
              ))}
          </div>
          <ul className="distribucion__leyenda">
            {porNivel.map((n) => (
              <li
                key={n.clase}
                tabIndex={0}
                className={activo === n.clase ? 'distribucion__item--activo' : undefined}
                onMouseEnter={() => setActivo(n.clase)}
                onMouseLeave={() => setActivo(null)}
                onFocus={() => setActivo(n.clase)}
                onBlur={() => setActivo(null)}
              >
                <span className={`distribucion__muestra distribucion__muestra--${n.clase}`} aria-hidden="true" />
                <span className="distribucion__nivel">
                  {n.etiqueta}
                  <small>{n.rango}</small>
                </span>
                <strong className="distribucion__cantidad">{n.cantidad}</strong>
                <span className="distribucion__porcentaje">{porcentaje(n.cantidad, completos)} %</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </Panel>
  )
}

/** Docentes con promedio por debajo de "Bueno", del más bajo al más alto. */
export function PanelAtencion({ analisis }) {
  const { atencion, completos } = analisis
  const visibles = atencion.slice(0, MAXIMO_ATENCION)
  return (
    <Panel
      icono={HeartHandshake}
      titulo="Requieren seguimiento"
      descripcion={`Promedio menor a ${formatearNota(UMBRAL_ATENCION)}: primeros candidatos a un compromiso de mejora.`}
    >
      {completos === 0 ? (
        <p className="paneles__vacio">Aparecerán cuando haya docentes con las tres calificaciones.</p>
      ) : visibles.length === 0 ? (
        <p className="atencion__vacio">
          <CircleCheck size={18} aria-hidden="true" />
          <span>
            <strong>Todo en orden.</strong> Ningún docente evaluado está por debajo de {formatearNota(UMBRAL_ATENCION)}.
          </span>
        </p>
      ) : (
        <ul className="atencion">
          {visibles.map(({ docente, promedio }) => (
            <li key={docente.idDocente}>
              <Link to={`/docentes/${docente.idDocente}`} className="atencion__fila">
                <span className="atencion__avatar" aria-hidden="true">
                  {iniciales(docente.nombreCompleto)}
                </span>
                <span className="atencion__datos">
                  <strong>{docente.nombreCompleto}</strong>
                  <span>{docente.nombrePrograma}</span>
                </span>
                <span className="atencion__nota">{formatearNota(promedio.valor)}</span>
                <EtiquetaNivel promedio={promedio} />
                <ArrowRight size={16} aria-hidden="true" className="atencion__flecha" />
              </Link>
            </li>
          ))}
          {atencion.length > visibles.length && (
            <li className="paneles__vacio">Y {atencion.length - visibles.length} más en la lista de docentes.</li>
          )}
        </ul>
      )}
    </Panel>
  )
}

/** Promedio de cada facultad (vicerrectoría) o programa (coordinación), sobre 5. */
export function PanelPorGrupo({ analisis, agrupacion }) {
  const { porGrupo } = analisis
  const nombre = agrupacion === 'facultad' ? 'facultad' : 'programa'
  return (
    <Panel
      icono={Building2}
      titulo={`Promedio por ${nombre}`}
      descripcion={`Docentes con evaluación completa en cada ${nombre}, sobre 5.`}
    >
      {porGrupo.length === 0 ? (
        <p className="paneles__vacio">Aún no hay docentes con las tres calificaciones.</p>
      ) : (
        <GraficoBarras
          titulo={`Promedio por ${nombre}`}
          maximo={5}
          filas={porGrupo.map((g) => ({
            id: g.nombre,
            etiqueta: g.nombre,
            valor: g.promedio,
            textoValor: formatearNota(g.promedio),
            detalle: `Promedio de ${docentesTexto(g.cantidad)} con evaluación completa.`,
          }))}
        />
      )}
    </Panel>
  )
}
