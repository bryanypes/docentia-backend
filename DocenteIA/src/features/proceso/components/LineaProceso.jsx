import {
  CalendarCheck,
  CalendarClock,
  CalendarX,
  Check,
  FilePen,
  FileSignature,
  RefreshCcw,
  ShieldCheck,
  UserCheck,
  UserX,
} from 'lucide-react'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { ESTADOS_PROCESO, PASOS_PROCESO } from '../estados.js'
import './Proceso.css'

const ICONO_HITO = {
  RESERVA: CalendarClock,
  CAMBIO: RefreshCcw,
  DECLINACION: CalendarX,
  CONFIRMACION: CalendarCheck,
  ASISTENCIA: UserCheck,
  INASISTENCIA: UserX,
  ACTA: FilePen,
  FIRMA: FileSignature,
  CIERRE: ShieldCheck,
}

/** Los ocho pasos del proceso, con el actual resaltado en el color de su estado. */
export function PasosProceso({ proceso }) {
  const { paso, estado } = proceso
  const tono = ESTADOS_PROCESO[estado]?.tono ?? 'neutro'
  const terminado = estado === 'FINALIZADO'
  return (
    <ol className="proceso__pasos" aria-label="Pasos del proceso de evaluación">
      {PASOS_PROCESO.map((p) => {
        const hecho = p.paso < paso || terminado
        const actual = p.paso === paso && !terminado
        const clase = hecho ? 'hecho' : actual ? `actual proceso__paso--${tono}` : 'pendiente'
        return (
          <li key={p.paso} className={`proceso__paso proceso__paso--${clase}`} aria-current={actual ? 'step' : undefined}>
            <span className="proceso__marca" aria-hidden="true">
              {hecho ? <Check size={14} strokeWidth={3} /> : p.paso}
            </span>
            <span className="proceso__titulo-paso">{p.titulo}</span>
            {hecho && <span className="solo-lectores"> (completado)</span>}
          </li>
        )
      })}
    </ol>
  )
}

/** Trazabilidad: qué pasó y cuándo, del suceso más antiguo al más reciente. */
export function HitosProceso({ hitos }) {
  if (!hitos?.length) {
    return <p className="campo__ayuda">Aún no hay sucesos registrados.</p>
  }
  return (
    <ol className="proceso__hitos">
      {hitos.map((hito, indice) => {
        const Icono = ICONO_HITO[hito.codigo] ?? Check
        return (
          <li key={`${hito.codigo}-${indice}`} className={`proceso__hito proceso__hito--${hito.codigo.toLowerCase()}`}>
            <span className="proceso__hito-icono" aria-hidden="true">
              <Icono size={15} />
            </span>
            <div>
              <strong>{hito.titulo}</strong>
              {hito.detalle && <span className="proceso__hito-detalle"> · {hito.detalle}</span>}
              <div className="proceso__hito-fecha">{formatearFechaHora(hito.fecha)}</div>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
