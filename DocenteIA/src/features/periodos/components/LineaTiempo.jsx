import { CalendarClock, Check, ClipboardList, FileSignature, SearchCheck } from 'lucide-react'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { hoyEnColombia } from '../../agenda/franjas.js'
import { estadoEtapa } from '../etapas.js'
import { obtenerCalendario } from '../services/periodosService.js'
import './LineaTiempo.css'

const ICONO_ETAPA = {
  RECOLECCION_CALIFICACIONES: ClipboardList,
  REVISION_HALLAZGOS: SearchCheck,
  AGENDA_CITAS: CalendarClock,
  FIRMA_ACTAS: FileSignature,
}

const ESTADO = {
  terminada: 'Terminada',
  abierta: 'Abierta',
  proxima: 'Próxima',
  sin: 'Sin programar',
}

const DIA_MS = 24 * 60 * 60 * 1000
const dias = (desde, hasta) => Math.round((Date.parse(hasta) - Date.parse(desde)) / DIA_MS)

/** Qué tanto ha corrido una etapa abierta (0 a 100) y cuántos días le quedan. */
function avance(etapa, hoy) {
  const total = Math.max(1, dias(etapa.fechaApertura, etapa.fechaCierre))
  const corridos = Math.min(total, Math.max(0, dias(etapa.fechaApertura, hoy)))
  return { porcentaje: Math.round((corridos / total) * 100), restantes: dias(hoy, etapa.fechaCierre) }
}

function textoRestante(restantes) {
  if (restantes <= 0) return 'Cierra hoy'
  if (restantes === 1) return 'Cierra mañana'
  return `Cierra en ${restantes} días`
}

/**
 * Las cuatro etapas del periodo activo como un recorrido paso a paso: ícono grande por etapa, línea
 * que las une, semanas de la Resolución 074, fechas y estado. La etapa vigente va resaltada.
 */
export function LineaTiempo() {
  const { datos } = useFetch(obtenerCalendario)
  if (!datos) return null
  const hoy = hoyEnColombia()
  const terminadas = datos.etapas.filter((e) => estadoEtapa(e, hoy) === 'terminada').length

  return (
    <section className="linea-tiempo" aria-labelledby="linea-tiempo-titulo">
      <div className="linea-tiempo__cabecera">
        <h2 id="linea-tiempo-titulo" className="linea-tiempo__titulo">
          Ruta del proceso de evaluación
        </h2>
        <span className="linea-tiempo__resumen">
          {terminadas} de {datos.etapas.length} terminadas
        </span>
      </div>
      <ol className="linea-tiempo__etapas">
        {datos.etapas.map((etapa, indice) => {
          const estado = estadoEtapa(etapa, hoy)
          const vigente = etapa.etapa === datos.etapaVigente?.etapa
          const Icono = estado === 'terminada' ? Check : (ICONO_ETAPA[etapa.etapa] ?? ClipboardList)
          const progreso = estado === 'abierta' ? avance(etapa, hoy) : null
          return (
            <li
              key={etapa.etapa}
              className={`linea-tiempo__etapa linea-tiempo__etapa--${estado}${vigente ? ' linea-tiempo__etapa--vigente' : ''}`}
              aria-current={vigente ? 'step' : undefined}
            >
              <div className="linea-tiempo__marca" aria-hidden="true">
                <span className="linea-tiempo__icono">
                  <Icono size={26} strokeWidth={estado === 'terminada' ? 3 : 2} />
                </span>
              </div>
              <div className="linea-tiempo__texto">
                <span className="linea-tiempo__paso">Paso {indice + 1}</span>
                <h3 className="linea-tiempo__nombre">{etapa.nombreEtapa}</h3>
                <span className="linea-tiempo__semanas">{etapa.semanasResolucion}</span>
                <span className="linea-tiempo__fechas">
                  {etapa.fechaApertura ? (
                    <>
                      {formatearFecha(etapa.fechaApertura)}
                      <span aria-hidden="true"> → </span>
                      <span className="solo-lectores"> al </span>
                      {formatearFecha(etapa.fechaCierre)}
                    </>
                  ) : (
                    'Fechas por definir'
                  )}
                </span>
                <span className="linea-tiempo__estados">
                  <span className={`linea-tiempo__estado linea-tiempo__estado--${estado}`}>{ESTADO[estado]}</span>
                  {vigente && <span className="linea-tiempo__estado linea-tiempo__estado--vigente">Etapa vigente</span>}
                </span>
                {progreso && (
                  <span className="linea-tiempo__progreso">
                    <span className="linea-tiempo__riel" aria-hidden="true">
                      <span className="linea-tiempo__relleno" style={{ width: `${progreso.porcentaje}%` }} />
                    </span>
                    <span>{textoRestante(progreso.restantes)}</span>
                  </span>
                )}
              </div>
            </li>
          )
        })}
      </ol>
      {datos.seguimiento && <p className="linea-tiempo__nota">{datos.seguimiento}</p>}
    </section>
  )
}
