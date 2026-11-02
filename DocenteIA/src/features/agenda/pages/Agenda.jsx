import { CalendarDays, ChevronLeft, ChevronRight, MousePointerClick, RefreshCw, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { ControlAsistencia } from '../../proceso/components/ControlAsistencia.jsx'
import { ETIQUETA_ASISTENCIA, ETIQUETA_CONFIRMACION } from '../../proceso/estados.js'
import {
  aHora,
  aMinutos,
  DURACION_FRANJA_MIN,
  diasDeSemana,
  etiquetaDia,
  etiquetaDiaLarga,
  etiquetaSemana,
  formatearHora,
  hoyEnColombia,
  lunesDe,
  sumarDias,
} from '../franjas.js'
import {
  abrirFranjasEnLote,
  cerrarFranja,
  cerrarFranjasEnLote,
  eliminarFranja,
  listarFranjas,
} from '../services/agendaService.js'
import './Agenda.css'

/** Horario que muestra el calendario; se amplía si hay franjas por fuera. */
const PRIMERA_HORA = 7 * 60
const ULTIMA_HORA = 20 * 60

/** Cómo se ve cada franja existente: el tono y el texto corto de la celda. */
function aspectoFranja(franja) {
  if (franja.estado === 'CERRADA') return { tono: 'cerrada', texto: 'Cerrada' }
  if (!franja.reserva) return { tono: 'libre', texto: 'Libre' }
  const { asistencia, confirmacion } = franja.reserva
  if (asistencia) return { tono: 'asistio', texto: ETIQUETA_ASISTENCIA[asistencia] }
  if (confirmacion === 'CONFIRMADA') return { tono: 'confirmada', texto: 'Confirmó' }
  return { tono: 'reservada', texto: 'Sin confirmar' }
}

function describirFranja(franja) {
  if (!franja.fecha) return `Franja #${franja.idFranja}`
  return `${formatearFecha(franja.fecha)} ${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`
}

/** Qué se aplicó y qué se omitió, con su motivo, en una apertura o un cierre en lote. */
function ReporteLote({ reporte }) {
  const verbo = reporte.accion === 'ABRIR' ? 'abiertas' : 'cerradas'
  const { aplicadas, omitidas } = reporte
  const titulo = `Franjas ${verbo}: ${aplicadas.length}. Omitidas: ${omitidas.length}.`
  return (
    <Aviso tipo={omitidas.length > 0 ? 'alerta' : 'exito'} titulo={titulo}>
      {omitidas.length > 0 && (
        <ul>
          {omitidas.map((omitida) => (
            <li key={`${omitida.indice}-${omitida.idFranja}`}>
              {describirFranja(omitida)}: {omitida.motivo}
            </li>
          ))}
        </ul>
      )}
    </Aviso>
  )
}

const LEYENDA = [
  { tono: 'nueva', texto: 'Selección para abrir' },
  { tono: 'libre', texto: 'Libre' },
  { tono: 'reservada', texto: 'Reservada · sin confirmar' },
  { tono: 'confirmada', texto: 'Asistencia confirmada' },
  { tono: 'asistio', texto: 'Reunión registrada' },
  { tono: 'cerrada', texto: 'Cerrada' },
]

/** Detalle de una franja del calendario con sus acciones. */
function DetalleFranja({ franja, hoy, onCerrar, onEliminar, onAsistencia, onSalir }) {
  const { tono, texto } = aspectoFranja(franja)
  const reserva = franja.reserva
  const puedeAsistencia = reserva && !reserva.asistencia && franja.fecha <= hoy
  return (
    <Panel
      titulo={`${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`}
      descripcion={etiquetaDiaLarga(franja.fecha)}
      icono={CalendarDays}
      tono="violeta"
      className="agenda__detalle"
      acciones={
        <Boton variante="enlace" icono={X} aria-label="Cerrar el detalle" onClick={onSalir}>
          Cerrar
        </Boton>
      }
    >
      <p>
        <span className={`agenda__chip agenda__chip--${tono}`}>{texto}</span>
      </p>
      {reserva ? (
        <>
          <p className="agenda__detalle-docente">
            <strong>{reserva.nombreDocente}</strong>
            <br />
            {reserva.confirmacion ? ETIQUETA_CONFIRMACION[reserva.confirmacion] : 'Aún no confirma su asistencia'}
          </p>
          <Link to={`/docentes/${reserva.idDocente}`}>Ver expediente y proceso</Link>
          {puedeAsistencia && (
            <ControlAsistencia cita={reserva} nombreDocente={reserva.nombreDocente} onRegistrada={onAsistencia} />
          )}
          {reserva && !reserva.asistencia && franja.fecha > hoy && (
            <p className="campo__ayuda">La asistencia se registra el día de la cita.</p>
          )}
        </>
      ) : (
        <div className="acciones-fila">
          {franja.estado === 'ABIERTA' && (
            <Boton variante="contorno" icono={X} onClick={onCerrar}>
              Cerrar franja
            </Boton>
          )}
          <Boton variante="contorno" icono={Trash2} onClick={onEliminar}>
            Eliminar
          </Boton>
        </div>
      )}
    </Panel>
  )
}

/**
 * Calendario semanal de la coordinación. Arrastrar sobre horas vacías (o clic y Mayús+clic) marca un
 * bloque para abrir franjas de 30 minutos; clic en una franja muestra quién la reservó, si confirmó y
 * permite registrar la asistencia el día de la cita.
 */
function CalendarioFranjas() {
  const hoy = hoyEnColombia()
  const [lunes, setLunes] = useState(() => lunesDe(hoy))
  const [ancla, setAncla] = useState(null)
  const [foco, setFoco] = useState(null)
  const [idDetalle, setIdDetalle] = useState(null)
  const [error, setError] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const arrastrando = useRef(false)
  const vieneDePuntero = useRef(false)

  const franjas = useFetch(listarFranjas)
  const dias = useMemo(() => diasDeSemana(lunes), [lunes])
  const lista = useMemo(() => franjas.datos ?? [], [franjas.datos])

  const porCelda = useMemo(() => {
    const mapa = new Map()
    for (const franja of lista) mapa.set(`${franja.fecha}|${formatearHora(franja.horaInicio)}`, franja)
    return mapa
  }, [lista])

  const horas = useMemo(() => {
    let desde = PRIMERA_HORA
    let hasta = ULTIMA_HORA
    for (const franja of lista) {
      if (!dias.includes(franja.fecha)) continue
      desde = Math.min(desde, aMinutos(formatearHora(franja.horaInicio)))
      hasta = Math.max(hasta, aMinutos(formatearHora(franja.horaFin)))
    }
    const filas = []
    for (let m = desde; m + DURACION_FRANJA_MIN <= hasta; m += DURACION_FRANJA_MIN) filas.push(m)
    return filas
  }, [lista, dias])

  const detalle = lista.find((f) => f.idFranja === idDetalle) ?? null

  // Rectángulo seleccionado: días e intervalos entre el ancla y el foco.
  const seleccion = useMemo(() => {
    if (!ancla || !foco) return null
    const d1 = Math.min(dias.indexOf(ancla.dia), dias.indexOf(foco.dia))
    const d2 = Math.max(dias.indexOf(ancla.dia), dias.indexOf(foco.dia))
    if (d1 < 0) return null
    return {
      dias: dias.slice(d1, d2 + 1),
      desde: Math.min(ancla.min, foco.min),
      hasta: Math.max(ancla.min, foco.min),
    }
  }, [ancla, foco, dias])

  const enSeleccion = (dia, min) =>
    Boolean(seleccion && seleccion.dias.includes(dia) && min >= seleccion.desde && min <= seleccion.hasta)

  const { nuevas, libres } = useMemo(() => {
    const resultadoSeleccion = { nuevas: [], libres: [] }
    if (!seleccion) return resultadoSeleccion
    for (const dia of seleccion.dias) {
      for (let m = seleccion.desde; m <= seleccion.hasta; m += DURACION_FRANJA_MIN) {
        const franja = porCelda.get(`${dia}|${aHora(m)}`)
        if (!franja && dia >= hoy) {
          resultadoSeleccion.nuevas.push({ fecha: dia, horaInicio: aHora(m), horaFin: aHora(m + DURACION_FRANJA_MIN) })
        } else if (franja && franja.estado === 'ABIERTA' && !franja.reserva) {
          resultadoSeleccion.libres.push(franja.idFranja)
        }
      }
    }
    return resultadoSeleccion
  }, [seleccion, porCelda, hoy])

  useEffect(() => {
    const soltar = () => {
      arrastrando.current = false
    }
    window.addEventListener('pointerup', soltar)
    return () => window.removeEventListener('pointerup', soltar)
  }, [])

  function limpiarSeleccion() {
    setAncla(null)
    setFoco(null)
  }

  function iniciarArrastre(evento, dia, min) {
    if (evento.button !== undefined && evento.button !== 0) return
    vieneDePuntero.current = true
    arrastrando.current = true
    setIdDetalle(null)
    if (evento.shiftKey && ancla) {
      setFoco({ dia, min })
    } else {
      setAncla({ dia, min })
      setFoco({ dia, min })
    }
  }

  // Sirve igual con mouse y con el dedo: la celda bajo el puntero se busca por coordenadas.
  function moverArrastre(evento) {
    if (!arrastrando.current) return
    const celda = document.elementFromPoint?.(evento.clientX, evento.clientY)?.closest?.('[data-celda-vacia]')
    if (!celda) return
    const dia = celda.getAttribute('data-dia')
    const min = Number(celda.getAttribute('data-min'))
    setFoco((actual) => (actual?.dia === dia && actual?.min === min ? actual : { dia, min }))
  }

  // Teclado (Enter o Espacio): clic fija el inicio, Mayús+clic extiende el bloque.
  function elegirConTeclado(evento, dia, min) {
    if (vieneDePuntero.current) {
      vieneDePuntero.current = false
      return
    }
    setIdDetalle(null)
    if (evento.shiftKey && ancla) {
      setFoco({ dia, min })
    } else {
      setAncla({ dia, min })
      setFoco({ dia, min })
    }
  }

  async function ejecutar(accion, mensaje) {
    setEnviando(true)
    setError(null)
    setReporte(null)
    setResultado(null)
    try {
      const respuesta = await accion()
      if (respuesta?.aplicadas) setReporte(respuesta)
      else if (mensaje) setResultado(mensaje)
      limpiarSeleccion()
      franjas.recargar()
      return true
    } catch (errorApi) {
      setError(errorApi)
      return false
    } finally {
      setEnviando(false)
    }
  }

  function eliminar(franja) {
    if (!window.confirm(`¿Eliminar la franja del ${describirFranja(franja)}?`)) return
    ejecutar(() => eliminarFranja(franja.idFranja), 'Franja eliminada.').then((hecho) => hecho && setIdDetalle(null))
  }

  const semana = lista.filter((f) => dias.includes(f.fecha))
  const conteo = {
    libres: semana.filter((f) => f.estado === 'ABIERTA' && !f.reserva).length,
    reservadas: semana.filter((f) => f.reserva && !f.reserva.confirmacion && !f.reserva.asistencia).length,
    confirmadas: semana.filter((f) => f.reserva?.confirmacion === 'CONFIRMADA' && !f.reserva.asistencia).length,
    reuniones: semana.filter((f) => f.reserva?.asistencia).length,
  }

  return (
    <div className="agenda">
      <Panel
        titulo="Calendario de citas"
        descripcion="Arrastra sobre las horas vacías para abrir franjas de 30 minutos. Con teclado: Enter marca el inicio y Mayús+Enter el final."
        icono={MousePointerClick}
        tono="turquesa"
        acciones={
          <div className="agenda__navegacion">
            <Boton variante="contorno" icono={ChevronLeft} aria-label="Semana anterior" onClick={() => setLunes((l) => sumarDias(l, -7))} />
            <Boton variante="contorno" onClick={() => setLunes(lunesDe(hoy))}>
              Hoy
            </Boton>
            <Boton variante="contorno" icono={ChevronRight} aria-label="Semana siguiente" onClick={() => setLunes((l) => sumarDias(l, 7))} />
          </div>
        }
      >
        <div className="agenda__resumen">
          <strong className="agenda__semana">{etiquetaSemana(dias)}</strong>
          <span className="agenda__contador agenda__contador--libre">{conteo.libres} libres</span>
          <span className="agenda__contador agenda__contador--reservada">{conteo.reservadas} sin confirmar</span>
          <span className="agenda__contador agenda__contador--confirmada">{conteo.confirmadas} confirmadas</span>
          <span className="agenda__contador agenda__contador--asistio">{conteo.reuniones} reuniones</span>
        </div>

        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <ErrorApi error={error} />
        {reporte && <ReporteLote reporte={reporte} />}

        {seleccion && (nuevas.length > 0 || libres.length > 0) && (
          <div className="agenda__barra" role="status">
            <span>
              <strong>
                {aHora(seleccion.desde)}–{aHora(seleccion.hasta + DURACION_FRANJA_MIN)}
              </strong>{' '}
              ·{' '}
              {seleccion.dias.length === 1
                ? etiquetaDiaLarga(seleccion.dias[0])
                : `${etiquetaDia(seleccion.dias[0])} a ${etiquetaDia(seleccion.dias[seleccion.dias.length - 1])}`}
            </span>
            <div className="acciones-fila">
              {nuevas.length > 0 && (
                <Boton disabled={enviando} onClick={() => ejecutar(() => abrirFranjasEnLote(nuevas))}>
                  {enviando ? 'Abriendo…' : `Abrir ${nuevas.length} ${nuevas.length === 1 ? 'franja' : 'franjas'}`}
                </Boton>
              )}
              {libres.length > 0 && (
                <Boton variante="contorno" disabled={enviando} onClick={() => ejecutar(() => cerrarFranjasEnLote(libres))}>
                  Cerrar {libres.length} libres
                </Boton>
              )}
              <Boton variante="enlace" onClick={limpiarSeleccion}>
                Cancelar
              </Boton>
            </div>
          </div>
        )}

        {franjas.cargando && !franjas.datos && <Cargando texto="Cargando franjas" bloques={1} />}
        {franjas.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar las franjas" mensaje={franjas.error.message}>
            <Boton icono={RefreshCw} onClick={franjas.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}

        {franjas.datos && (
          <div className="agenda__contenedor">
            <div
              className="agenda__rejilla"
              style={{ gridTemplateColumns: `64px repeat(${dias.length}, minmax(104px, 1fr))` }}
              onPointerMove={moverArrastre}
            >
              <div className="agenda__esquina" aria-hidden="true" />
              {dias.map((dia) => (
                <div key={dia} className={`agenda__cabecera${dia === hoy ? ' agenda__cabecera--hoy' : ''}`}>
                  {etiquetaDia(dia)}
                </div>
              ))}
              {horas.map((min) => (
                <FilaHora
                  key={min}
                  min={min}
                  dias={dias}
                  hoy={hoy}
                  porCelda={porCelda}
                  enSeleccion={enSeleccion}
                  idDetalle={idDetalle}
                  onIniciar={iniciarArrastre}
                  onTeclado={elegirConTeclado}
                  onAbrirDetalle={(franja) => {
                    limpiarSeleccion()
                    setIdDetalle(franja.idFranja)
                  }}
                />
              ))}
            </div>
          </div>
        )}

        <ul className="agenda__leyenda" aria-label="Leyenda">
          {LEYENDA.map(({ tono, texto }) => (
            <li key={tono}>
              <span className={`agenda__muestra agenda__celda--${tono}`} aria-hidden="true" />
              {texto}
            </li>
          ))}
        </ul>
      </Panel>

      {detalle && (
        <DetalleFranja
          franja={detalle}
          hoy={hoy}
          onSalir={() => setIdDetalle(null)}
          onCerrar={() => ejecutar(() => cerrarFranja(detalle.idFranja), 'Franja cerrada.')}
          onEliminar={() => eliminar(detalle)}
          onAsistencia={() => {
            setResultado('Asistencia registrada.')
            franjas.recargar()
          }}
        />
      )}
    </div>
  )
}

function FilaHora({ min, dias, hoy, porCelda, enSeleccion, idDetalle, onIniciar, onTeclado, onAbrirDetalle }) {
  const hora = aHora(min)
  const fin = aHora(min + DURACION_FRANJA_MIN)
  return (
    <>
      <div className="agenda__hora" aria-hidden="true">
        {min % 60 === 0 ? hora : ''}
      </div>
      {dias.map((dia) => {
        const franja = porCelda.get(`${dia}|${hora}`)
        const etiqueta = `${etiquetaDiaLarga(dia)}, ${hora}–${fin}`
        if (franja) {
          const { tono, texto } = aspectoFranja(franja)
          return (
            <button
              key={dia}
              type="button"
              className={`agenda__celda agenda__celda--${tono}${idDetalle === franja.idFranja ? ' agenda__celda--activa' : ''}${enSeleccion(dia, min) ? ' agenda__celda--marcada' : ''}`}
              aria-label={`${etiqueta}: ${franja.reserva ? `${franja.reserva.nombreDocente}, ` : ''}${texto}`}
              onClick={() => onAbrirDetalle(franja)}
            >
              <span className="agenda__celda-texto">{franja.reserva?.nombreDocente ?? texto}</span>
              {franja.reserva && <span className="agenda__celda-sub">{texto}</span>}
            </button>
          )
        }
        const pasada = dia < hoy
        return (
          <button
            key={dia}
            type="button"
            data-celda-vacia=""
            data-dia={dia}
            data-min={min}
            disabled={pasada}
            className={`agenda__celda agenda__celda--vacia${enSeleccion(dia, min) && !pasada ? ' agenda__celda--nueva' : ''}`}
            aria-label={`${etiqueta}: vacía`}
            aria-pressed={enSeleccion(dia, min)}
            onPointerDown={(e) => onIniciar(e, dia, min)}
            onClick={(e) => onTeclado(e, dia, min)}
          />
        )
      })}
    </>
  )
}

export default function Agenda() {
  const ventana = useVentanaEtapa(ETAPAS.AGENDA_CITAS)

  return (
    <>
      <Encabezado
        miga="Coordinación · Agenda de citas"
        titulo="Agenda de citas"
        subtitulo="Abre tus horarios en el calendario, mira quién reservó y si confirmó, y registra la asistencia el día de la reunión."
      />
      <div className="pagina">
        <AvisoEtapaCerrada ventana={ventana} accion="Abrir, cerrar o eliminar franjas" />
        <CalendarioFranjas />
      </div>
    </>
  )
}
