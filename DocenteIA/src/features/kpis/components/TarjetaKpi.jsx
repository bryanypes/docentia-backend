import { Minus, TrendingDown, TrendingUp } from 'lucide-react'
import { Ayuda } from '../../../shared/components/ui/Ayuda/Ayuda.jsx'
import { formatearEntero, formatearNota, nivelDeNota } from '../../../shared/utils/formatters.js'
import './TarjetaKpi.css'

/**
 * La comparación contra el periodo anterior, dicha en palabras: qué pasó, cuánto y frente a qué.
 * Ejemplos: "Subió 0,29 frente a 2026-1 (era 4,04)", "1 más que en 2026-1 (era 1)".
 */
function describirComparacion({ variacion, valorAnterior }, periodoAnterior, conDecimales) {
  if (!periodoAnterior) return { sentido: 'igual', texto: 'Primer periodo con datos.' }
  if (variacion === null) return { sentido: 'igual', texto: `Sin dato en ${periodoAnterior.nombre} para comparar.` }
  const numero = Number(variacion)
  const formato = conDecimales ? formatearNota : formatearEntero
  const antes = formato(valorAnterior)
  const cuanto = formato(Math.abs(numero))
  const periodo = periodoAnterior.nombre
  if (numero === 0) return { sentido: 'igual', texto: `Igual que en ${periodo}.` }
  if (conDecimales) {
    return {
      sentido: numero > 0 ? 'sube' : 'baja',
      texto: `${numero > 0 ? 'Subió' : 'Bajó'} ${cuanto} frente a ${periodo} (era ${antes}).`,
    }
  }
  return {
    sentido: numero > 0 ? 'sube' : 'baja',
    texto: `${cuanto} ${numero > 0 ? 'más' : 'menos'} que en ${periodo} (${Number(valorAnterior) === 1 ? 'era' : 'eran'} ${antes}).`,
  }
}

const ICONO_SENTIDO = { sube: TrendingUp, baja: TrendingDown, igual: Minus }

/** Una sola línea en texto gris; el color solo va en el ícono (y nunca solo: la frase lo dice). */
function Comparacion({ indicador, periodoAnterior, conDecimales }) {
  const { sentido, texto } = describirComparacion(indicador, periodoAnterior, conDecimales)
  const Icono = ICONO_SENTIDO[sentido]
  return (
    <p className={`comparacion comparacion--${sentido}`}>
      <Icono size={15} aria-hidden="true" />
      <span>{texto}</span>
    </p>
  )
}

/** Indicador: ícono, qué mide, el número, una línea de comparación y la explicación en ⓘ. */
export function TarjetaKpi({ icono: Icono, rotulo, valor, unidad, indicador, conDecimales = false, periodoAnterior, explicacion }) {
  return (
    <article className="tarjeta-kpi">
      <div className="tarjeta-kpi__cabecera">
        <span className="tarjeta-kpi__icono" aria-hidden="true">
          <Icono size={18} />
        </span>
        <h3 className="tarjeta-kpi__rotulo">{rotulo}</h3>
        <Ayuda texto={explicacion} etiqueta={`Qué mide: ${rotulo}`} />
      </div>
      <div className="tarjeta-kpi__valor">
        <strong className="tarjeta-kpi__numero">{valor}</strong>
        {unidad && <span>{unidad}</span>}
      </div>
      <Comparacion indicador={indicador} periodoAnterior={periodoAnterior} conDecimales={conDecimales} />
    </article>
  )
}

/** La cifra con la que abre el tablero: el promedio general, con su nivel y el acento amarillo. */
export function TarjetaPromedio({ indicador, periodo, periodoAnterior, ambito }) {
  const valor = indicador.valor
  const nivel = valor === null ? null : nivelDeNota(valor)
  return (
    <article className="tarjeta-promedio">
      <div className="tarjeta-promedio__cabecera">
        <h3 className="tarjeta-promedio__rotulo">Promedio general · {periodo.nombre}</h3>
        <Ayuda
          etiqueta="Qué mide: promedio general"
          texto={
            valor === null
              ? 'Aparece cuando al menos un docente tenga las tres calificaciones.'
              : `Promedio ${ambito} con evaluación completa: estudiantes, coordinación y vicerrectoría.`
          }
        />
      </div>
      <div className="tarjeta-promedio__valor">
        <strong>{valor === null ? '—' : formatearNota(valor)}</strong>
        <span>de 5</span>
        {nivel && <span className="tarjeta-promedio__nivel">{nivel.etiqueta}</span>}
      </div>
      <Comparacion indicador={indicador} periodoAnterior={periodoAnterior} conDecimales />
    </article>
  )
}
