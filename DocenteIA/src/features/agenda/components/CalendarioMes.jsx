import { ChevronLeft, ChevronRight } from 'lucide-react'
import { etiquetaDiaLarga, etiquetaMes, semanasDelMes } from '../franjas.js'
import './CalendarioMes.css'

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const NOMBRES_DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']

/**
 * Calendario de un mes para elegir un día. Cada día puede llevar una marca (cuántas franjas tiene y
 * de qué tono); hoy va con un anillo y el día elegido, relleno. Los días sin marca quedan apagados
 * si `soloConMarca`.
 *
 * @param marcas Map de "AAAA-MM-DD" → { cantidad, tono, texto }
 */
export function CalendarioMes({ mes, onMes, seleccionado, onElegir, marcas, hoy, soloConMarca = false, titulo }) {
  const semanas = semanasDelMes(mes)
  const esteMes = mes.slice(0, 7)
  return (
    <div className="calendario-mes">
      <div className="calendario-mes__cabecera">
        <button type="button" className="calendario-mes__flecha" aria-label="Mes anterior" onClick={() => onMes(-1)}>
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <h3 className="calendario-mes__titulo" aria-live="polite">
          {etiquetaMes(mes)}
        </h3>
        <button type="button" className="calendario-mes__flecha" aria-label="Mes siguiente" onClick={() => onMes(1)}>
          <ChevronRight size={18} aria-hidden="true" />
        </button>
      </div>
      <table className="calendario-mes__tabla" aria-label={titulo ?? etiquetaMes(mes)}>
        <thead>
          <tr>
            {DIAS.map((dia, i) => (
              <th key={dia} scope="col" abbr={NOMBRES_DIAS[i]}>
                {dia}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {semanas.map((semana) => (
            <tr key={semana[0]}>
              {semana.map((dia) => {
                const marca = marcas?.get(dia)
                const fuera = dia.slice(0, 7) !== esteMes
                const pasado = dia < hoy
                const deshabilitado = pasado || (soloConMarca && !marca)
                const clases = [
                  'calendario-mes__dia',
                  fuera && 'calendario-mes__dia--fuera',
                  dia === hoy && 'calendario-mes__dia--hoy',
                  dia === seleccionado && 'calendario-mes__dia--elegido',
                  marca && `calendario-mes__dia--${marca.tono}`,
                ]
                  .filter(Boolean)
                  .join(' ')
                return (
                  <td key={dia}>
                    <button
                      type="button"
                      className={clases}
                      disabled={deshabilitado}
                      aria-pressed={dia === seleccionado}
                      aria-current={dia === hoy ? 'date' : undefined}
                      aria-label={`${etiquetaDiaLarga(dia)}${dia === hoy ? ', hoy' : ''}${marca ? `: ${marca.texto}` : ''}`}
                      onClick={() => onElegir(dia)}
                    >
                      <span className="calendario-mes__numero">{Number(dia.slice(8))}</span>
                      {marca && <span className="calendario-mes__marca">{marca.cantidad}</span>}
                    </button>
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
