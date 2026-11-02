import { CalendarCheck, CalendarX, CircleCheck, RefreshCw } from 'lucide-react'
import { useState } from 'react'
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
import { PanelProcesoPropio } from '../../proceso/components/PanelProcesoPropio.jsx'
import { responderCita } from '../../proceso/services/procesoService.js'
import { agruparPorFecha, formatearHora } from '../franjas.js'
import { listarFranjasDisponibles, obtenerMiCita, reservarFranja } from '../services/agendaService.js'
import './Agenda.css'

function describir(franja) {
  return `${formatearFecha(franja.fecha)}, ${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`
}

/** Confirmar o declinar la cita. Declinar la libera y la franja queda para otro docente. */
function ConfirmacionCita({ cita, onRespondida, onError }) {
  const [enviando, setEnviando] = useState(false)

  async function responder(asiste) {
    if (
      !asiste &&
      !window.confirm('Si declinas, tu cita se libera y la franja queda para otro docente. ¿Declinar la cita?')
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
        <Boton variante="contorno" icono={CalendarX} disabled={enviando} onClick={() => responder(false)}>
          No podré asistir
        </Boton>
      </div>
    </div>
  )
}

export default function MiCita() {
  const ventana = useVentanaEtapa(ETAPAS.AGENDA_CITAS)
  const cita = useFetch(obtenerMiCita)
  const disponibles = useFetch(listarFranjasDisponibles)
  const [reservando, setReservando] = useState(null)
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [versionProceso, setVersionProceso] = useState(0)

  async function reservar(franja) {
    // Solo hay una cita activa por periodo: reservar otra libera la anterior.
    if (
      cita.datos &&
      !window.confirm(
        `Tu cita anterior (${describir(cita.datos)}) se liberará y quedará disponible para otros docentes. ¿Reservar ${describir(franja)}?`,
      )
    ) {
      return
    }
    setReservando(franja.idFranja)
    setError(null)
    setResultado(null)
    try {
      const nueva = await reservarFranja(franja.idFranja)
      setResultado(
        `Cita reservada: ${describir(nueva)}.${nueva.idCitaLiberada ? ' Tu cita anterior quedó liberada.' : ''}`,
      )
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setReservando(null)
      // Tanto si se reservó como si otra persona la tomó, la lista cambió.
      cita.recargar()
      disponibles.recargar()
      setVersionProceso((v) => v + 1)
    }
  }

  const lista = disponibles.datos ?? []

  return (
    <>
      <Encabezado
        miga="Docente · Mi cita"
        titulo="Mi cita de retroalimentación"
        subtitulo="Elige una franja de 30 minutos con la coordinación de tu facultad."
      />
      <div className="pagina">
        <AvisoEtapaCerrada ventana={ventana} accion="Reservar una cita" />
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <ErrorApi error={error} />
        {error?.status === 409 && !error.datos?.etapa && (
          <p className="campo__ayuda">La lista de franjas se actualizó; elige otra.</p>
        )}

        {cita.cargando && !cita.datos && <Cargando texto="Consultando tu cita" bloques={1} />}
        {cita.error && (
          <EstadoVista tipo="error" titulo="No fue posible consultar tu cita" mensaje={cita.error.message}>
            <Boton icono={RefreshCw} onClick={cita.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!cita.cargando && !cita.error && !cita.datos && (
          <Aviso tipo="info" titulo="Todavía no tienes cita">
            Reserva una de las franjas disponibles.
          </Aviso>
        )}
        {cita.datos && (
          <Panel titulo="Tu cita" descripcion={`Con ${cita.datos.nombreCoordinador}`} icono={CalendarCheck}>
            <p className="agenda__cita">{describir(cita.datos)}</p>
            <ConfirmacionCita
              cita={cita.datos}
              onError={setError}
              onRespondida={(mensaje) => {
                setResultado(mensaje)
                cita.recargar()
                disponibles.recargar()
                setVersionProceso((v) => v + 1)
              }}
            />
          </Panel>
        )}

        <PanelProcesoPropio version={versionProceso} />

        <Panel titulo="Franjas disponibles" icono={CalendarCheck} tono="violeta">
          {disponibles.cargando && !disponibles.datos && <Cargando texto="Cargando franjas" bloques={1} />}
          {disponibles.error && (
            <EstadoVista tipo="error" titulo="No fue posible cargar las franjas" mensaje={disponibles.error.message}>
              <Boton icono={RefreshCw} onClick={disponibles.recargar}>Intentar de nuevo</Boton>
            </EstadoVista>
          )}
          {!disponibles.cargando && !disponibles.error && lista.length === 0 && (
            <EstadoVista
              titulo="No hay franjas disponibles"
              mensaje="La coordinación aún no ha abierto franjas o ya están todas reservadas."
            />
          )}
          {agruparPorFecha(lista).map(([dia, delDia]) => (
            <div key={dia} className="agenda__dia">
              <h3 className="agenda__fecha">{formatearFecha(dia)}</h3>
              <ul className="agenda__franjas">
                {delDia.map((franja) => (
                  <li key={franja.idFranja}>
                    <span>
                      <strong>
                        {formatearHora(franja.horaInicio)}–{formatearHora(franja.horaFin)}
                      </strong>{' '}
                      con {franja.nombreCoordinador}
                    </span>
                    <Boton
                      variante="contorno"
                      disabled={reservando !== null}
                      aria-label={`Reservar ${describir(franja)}`}
                      onClick={() => reservar(franja)}
                    >
                      {reservando === franja.idFranja ? 'Reservando…' : 'Reservar'}
                    </Boton>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </Panel>
      </div>
    </>
  )
}
