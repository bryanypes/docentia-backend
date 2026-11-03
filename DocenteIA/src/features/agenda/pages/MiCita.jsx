import { CalendarCheck, CalendarClock, CalendarX, CircleCheck, Clock, RefreshCw, UserRound } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { AvisoFlotante } from '../../../shared/components/ui/Avisos/AvisoFlotante.jsx'
import { useAvisos } from '../../../shared/components/ui/Avisos/avisosContext.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { PanelProcesoPropio } from '../../proceso/components/PanelProcesoPropio.jsx'
import { responderCita } from '../../proceso/services/procesoService.js'
import { CalendarioMes } from '../components/CalendarioMes.jsx'
import { etiquetaDiaLarga, formatearHora, hoyEnColombia, inicioDeMes, sumarMeses } from '../franjas.js'
import { listarFranjasDisponibles, obtenerMiCita, reservarFranja } from '../services/agendaService.js'
import './Agenda.css'
import './MiCita.css'

function describir(franja) {
  return `${formatearFecha(franja.fecha)}, ${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`
}

/** Confirmar o declinar la cita. Declinar la libera y la franja queda para otro docente. */
function ConfirmacionCita({ cita, onRespondida, onError, onCambiar }) {
  const { confirmar } = useAvisos()
  const [enviando, setEnviando] = useState(false)

  async function responder(asiste) {
    if (
      !asiste &&
      !(await confirmar({
        tipo: 'peligro',
        titulo: '¿Declinar la cita?',
        mensaje: 'Tu cita se libera y la franja queda para otro docente. Luego puedes reservar otra.',
        textoConfirmar: 'Declinar cita',
      }))
    ) {
      return
    }
    setEnviando(true)
    onError(null)
    try {
      await responderCita(asiste)
      onRespondida(asiste ? 'Confirmaste tu asistencia. La coordinación ya fue avisada.' : 'Declinaste la cita; reserva otra franja.')
    } catch (errorApi) {
      onError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  if (cita.asistencia === 'ASISTIO') {
    return <span className="pildora pildora--exito">Asististe a tu reunión</span>
  }
  return (
    <div className="agenda__confirmacion">
      {cita.confirmacion === 'CONFIRMADA' ? (
        <span className="pildora pildora--turquesa">Asistencia confirmada</span>
      ) : (
        <span className="pildora pildora--violeta">Falta confirmar tu asistencia</span>
      )}
      <div className="acciones-fila">
        {cita.confirmacion !== 'CONFIRMADA' && (
          <Boton icono={CircleCheck} disabled={enviando} onClick={() => responder(true)}>
            Confirmar asistencia
          </Boton>
        )}
        <Boton variante="contorno" icono={CalendarClock} disabled={enviando} onClick={onCambiar}>
          Cambiar horario
        </Boton>
        <Boton variante="peligro" icono={CalendarX} disabled={enviando} onClick={() => responder(false)}>
          No podré asistir
        </Boton>
      </div>
    </div>
  )
}

/** Confirmación de la reserva, con la fecha, la hora y quién atiende. */
function ModalReserva({ franja, citaActual, enviando, onReservar, onCerrar }) {
  return (
    <Modal
      titulo={citaActual ? 'Cambiar mi cita' : 'Reservar cita'}
      descripcion="Revisa el horario antes de confirmar."
      icono={CalendarCheck}
      ancho="pequeno"
      onCerrar={onCerrar}
    >
      <div className="mi-cita__resumen">
        <div>
          <CalendarCheck size={18} aria-hidden="true" />
          <span>
            <small>Fecha</small>
            <strong>{etiquetaDiaLarga(franja.fecha)}</strong>
          </span>
        </div>
        <div>
          <Clock size={18} aria-hidden="true" />
          <span>
            <small>Hora</small>
            <strong>
              {formatearHora(franja.horaInicio)} – {formatearHora(franja.horaFin)}
            </strong>
          </span>
        </div>
        <div>
          <UserRound size={18} aria-hidden="true" />
          <span>
            <small>Con</small>
            <strong>{franja.nombreCoordinador}</strong>
          </span>
        </div>
      </div>
      {citaActual && (
        <p className="mi-cita__aviso">
          Tu cita anterior ({describir(citaActual)}) se liberará y quedará disponible para otros docentes.
        </p>
      )}
      <div className="mi-cita__acciones">
        <Boton variante="secundario" onClick={onCerrar}>
          Cancelar
        </Boton>
        <Boton icono={CalendarCheck} disabled={enviando} onClick={onReservar}>
          {enviando ? 'Reservando…' : citaActual ? 'Cambiar mi cita' : 'Reservar'}
        </Boton>
      </div>
    </Modal>
  )
}

/**
 * La cita del docente: su estado y, debajo, un calendario con los días que tienen horarios libres.
 * Al elegir un día salen sus horarios; al elegir uno se confirma en un modal.
 */
export default function MiCita() {
  const ventana = useVentanaEtapa(ETAPAS.AGENDA_CITAS)
  const hoy = hoyEnColombia()
  const cita = useFetch(obtenerMiCita)
  const disponibles = useFetch(listarFranjasDisponibles)
  // Nulos hasta que la persona elija: entonces se usa el primer día con horarios libres.
  const [mesElegido, setMes] = useState(null)
  const [diaElegido, setDia] = useState(null)
  const [elegida, setElegida] = useState(null)
  const [reservando, setReservando] = useState(false)
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [versionProceso, setVersionProceso] = useState(0)

  const lista = useMemo(() => disponibles.datos ?? [], [disponibles.datos])
  const porDia = useMemo(() => {
    const mapa = new Map()
    for (const franja of lista) mapa.set(franja.fecha, [...(mapa.get(franja.fecha) ?? []), franja])
    return mapa
  }, [lista])
  const marcas = useMemo(() => {
    const mapa = new Map()
    for (const [fecha, franjas] of porDia) {
      mapa.set(fecha, { tono: 'libre', cantidad: franjas.length, texto: `${franjas.length} horarios libres` })
    }
    if (cita.datos) mapa.set(cita.datos.fecha, { tono: 'cita', cantidad: '★', texto: 'tu cita' })
    return mapa
  }, [porDia, cita.datos])

  const primerDia = [...porDia.keys()].sort()[0] ?? null
  const dia = diaElegido ?? primerDia
  const mes = mesElegido ?? inicioDeMes(dia ?? hoy)

  const delDia = (porDia.get(dia) ?? []).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))

  async function reservar() {
    setReservando(true)
    setError(null)
    setResultado(null)
    try {
      const nueva = await reservarFranja(elegida.idFranja)
      setResultado(`Cita reservada: ${describir(nueva)}.${nueva.idCitaLiberada ? ' Tu cita anterior quedó liberada.' : ''}`)
      setElegida(null)
    } catch (errorApi) {
      setError(errorApi)
      setElegida(null)
    } finally {
      setReservando(false)
      // Tanto si se reservó como si otra persona la tomó, la lista cambió.
      cita.recargar()
      disponibles.recargar()
      setVersionProceso((v) => v + 1)
    }
  }

  function irAlCalendario() {
    document.getElementById('calendario-disponibilidad')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <>
      <Encabezado
        miga="Docente · Mi cita"
        titulo="Mi cita de retroalimentación"
        subtitulo="Elige en el calendario un horario de 30 minutos con la coordinación de tu facultad."
      />
      <div className="pagina">
        <AvisoEtapaCerrada ventana={ventana} accion="Reservar una cita" />
        <AvisoFlotante mensaje={resultado} />
        <ErrorApi error={error} flotante />

        {cita.cargando && !cita.datos && <Cargando texto="Consultando tu cita" bloques={1} />}
        {cita.error && (
          <EstadoVista tipo="error" titulo="No fue posible consultar tu cita" mensaje={cita.error.message}>
            <Boton icono={RefreshCw} onClick={cita.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {cita.datos ? (
          <Panel titulo="Tu cita" descripcion={`Con ${cita.datos.nombreCoordinador}`} icono={CalendarCheck} tono="violeta">
            <p className="agenda__cita">{describir(cita.datos)}</p>
            <ConfirmacionCita
              cita={cita.datos}
              onError={setError}
              onCambiar={irAlCalendario}
              onRespondida={(mensaje) => {
                setResultado(mensaje)
                cita.recargar()
                disponibles.recargar()
                setVersionProceso((v) => v + 1)
              }}
            />
          </Panel>
        ) : (
          !cita.cargando &&
          !cita.error && (
            <div className="mi-cita__vacia">
              <CalendarClock size={22} aria-hidden="true" />
              <div>
                <strong>Todavía no tienes cita</strong>
                <span>Elige un día resaltado en el calendario y luego un horario.</span>
              </div>
            </div>
          )
        )}

        <PanelProcesoPropio version={versionProceso} />

        {/* Con la asistencia registrada la cita ya ocurrió: no hay horario que cambiar. */}
        {cita.datos?.asistencia !== 'ASISTIO' && (
        <section id="calendario-disponibilidad" className="mi-cita panel" aria-label="Horarios disponibles">
          <div className="mi-cita__columna">
            <h2 className="mi-cita__titulo">Elige un día</h2>
            <ul className="mi-cita__leyenda">
              <li>
                <span className="mi-cita__punto mi-cita__punto--libre" aria-hidden="true" /> Con horarios libres
              </li>
              <li>
                <span className="mi-cita__punto mi-cita__punto--cita" aria-hidden="true" /> Tu cita
              </li>
              <li>
                <span className="mi-cita__punto mi-cita__punto--hoy" aria-hidden="true" /> Hoy
              </li>
            </ul>
            {disponibles.cargando && !disponibles.datos ? (
              <Cargando texto="Cargando horarios" bloques={1} />
            ) : (
              <CalendarioMes
                mes={mes}
                onMes={(paso) => setMes(sumarMeses(mes, paso))}
                seleccionado={dia}
                onElegir={setDia}
                marcas={marcas}
                hoy={hoy}
                soloConMarca
                titulo="Días con horarios disponibles"
              />
            )}
          </div>

          <div className="mi-cita__columna">
            <h2 className="mi-cita__titulo">{dia ? etiquetaDiaLarga(dia) : 'Horarios'}</h2>
            {disponibles.error && (
              <EstadoVista tipo="error" titulo="No fue posible cargar los horarios" mensaje={disponibles.error.message}>
                <Boton icono={RefreshCw} onClick={disponibles.recargar}>Intentar de nuevo</Boton>
              </EstadoVista>
            )}
            {!disponibles.cargando && !disponibles.error && lista.length === 0 && (
              <EstadoVista
                titulo="No hay horarios disponibles"
                mensaje="La coordinación aún no ha abierto horarios o ya están todos reservados."
              />
            )}
            {lista.length > 0 && delDia.length === 0 && (
              <p className="campo__ayuda">Elige un día resaltado para ver sus horarios.</p>
            )}
            <ul className="mi-cita__horarios">
              {delDia.map((franja) => (
                <li key={franja.idFranja}>
                  <button
                    type="button"
                    className="mi-cita__horario"
                    aria-label={`Reservar ${describir(franja)}`}
                    onClick={() => setElegida(franja)}
                  >
                    <Clock size={16} aria-hidden="true" />
                    <strong>
                      {formatearHora(franja.horaInicio)} – {formatearHora(franja.horaFin)}
                    </strong>
                    <span>con {franja.nombreCoordinador}</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </section>
        )}
      </div>

      {elegida && (
        <ModalReserva
          franja={elegida}
          citaActual={cita.datos}
          enviando={reservando}
          onReservar={reservar}
          onCerrar={() => setElegida(null)}
        />
      )}
    </>
  )
}
