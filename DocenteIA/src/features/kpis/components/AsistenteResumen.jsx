import { ArrowRight, CalendarClock, CircleCheck, HeartHandshake, ListChecks, PenLine, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { ETIQUETA_FUENTE } from '../../../shared/constants/dominio.js'
import { formatearFecha, formatearNota } from '../../../shared/utils/formatters.js'
import { UMBRAL_ATENCION } from '../seguimiento.js'
import './AsistenteResumen.css'

const MS_POR_DIA = 24 * 60 * 60 * 1000

function diasHasta(fechaIso) {
  const [anio, mes, dia] = fechaIso.split('-').map(Number)
  const hoy = new Date()
  const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())
  return Math.round((new Date(anio, mes - 1, dia) - inicioHoy) / MS_POR_DIA)
}

/**
 * Lo que el asistente cuenta, armado con reglas sobre los datos del periodo (no lo redacta un
 * modelo de IA). Cada mensaje trae su ícono; el robot cambia de expresión según la situación.
 */
function armarResumen({ analisis, miFuente, recoleccion }) {
  const { total, completos, faltaMia, atencion } = analisis
  const mensajes = []

  if (completos === total) {
    mensajes.push({ Icono: CircleCheck, texto: 'Todos los docentes tienen su evaluación completa.' })
  } else {
    const pct = Math.round((completos / total) * 100)
    mensajes.push({
      Icono: ListChecks,
      texto: `Van ${completos} de ${total} docentes con evaluación completa (${pct} %).`,
    })
  }

  if (miFuente) {
    mensajes.push(
      faltaMia > 0
        ? {
            Icono: PenLine,
            texto: `Te ${faltaMia === 1 ? 'falta 1 calificación' : `faltan ${faltaMia} calificaciones`} de ${ETIQUETA_FUENTE[miFuente].toLowerCase()}.`,
            destacado: true,
          }
        : { Icono: CircleCheck, texto: 'Ya registraste todas tus calificaciones.' },
    )
  }

  if (atencion.length > 0) {
    mensajes.push({
      Icono: HeartHandshake,
      texto: `${atencion.length === 1 ? '1 docente tiene' : `${atencion.length} docentes tienen`} promedio menor a ${formatearNota(UMBRAL_ATENCION)}: buen momento para acordar un compromiso de mejora.`,
    })
  } else if (completos > 0) {
    mensajes.push({ Icono: HeartHandshake, texto: `Ningún docente evaluado está por debajo de ${formatearNota(UMBRAL_ATENCION)}.` })
  }

  if (recoleccion?.fechaCierre) {
    const dias = diasHasta(recoleccion.fechaCierre)
    mensajes.push({
      Icono: CalendarClock,
      texto: recoleccion.abierta
        ? `La recolección de calificaciones cierra el ${formatearFecha(recoleccion.fechaCierre)}${dias >= 0 ? ` (${dias === 0 ? 'hoy' : dias === 1 ? 'mañana' : `en ${dias} días`})` : ''}.`
        : 'La recolección de calificaciones está cerrada.',
    })
  }

  const robot =
    miFuente && faltaMia > 0
      ? robotSenala
      : atencion.length > 0
        ? robotPensando
        : completos === total
          ? robotAprobada
          : robotSaludo

  return { mensajes, robot }
}

/** El avatar de IA saluda y resume el periodo en lenguaje natural, con el siguiente paso a mano. */
export function AsistenteResumen({ nombre, analisis, miFuente, recoleccion }) {
  const { mensajes, robot } = armarResumen({ analisis, miFuente, recoleccion })
  const primerNombre = nombre.split(' ')[0]

  return (
    <section className="asistente" aria-labelledby="asistente-saludo">
      <div className="asistente__robot" aria-hidden="true">
        <img key={robot} src={robot} alt="" />
      </div>

      <div className="asistente__globo">
        <p className="asistente__etiqueta">
          <Sparkles size={14} aria-hidden="true" />
          Asistente DOCENTIA
        </p>
        <h2 id="asistente-saludo" className="asistente__saludo">
          Hola, {primerNombre}. Así va la evaluación hoy:
        </h2>
        <ul className="asistente__mensajes">
          {mensajes.map(({ Icono, texto, destacado }) => (
            <li key={texto} className={destacado ? 'asistente__mensaje--destacado' : undefined}>
              <Icono size={17} aria-hidden="true" />
              <span>{texto}</span>
            </li>
          ))}
        </ul>
        <div className="asistente__pie">
          {miFuente && analisis.faltaMia > 0 && (
            <Link to="/docentes?estado=FALTA_MI_NOTA" className="asistente__accion">
              Ir a calificar
              <ArrowRight size={16} aria-hidden="true" />
            </Link>
          )}
          <span className="asistente__nota">Resumen calculado con los datos del periodo activo.</span>
        </div>
      </div>
    </section>
  )
}
