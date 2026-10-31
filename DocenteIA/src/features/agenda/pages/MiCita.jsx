import { CalendarCheck, RefreshCw } from 'lucide-react'
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
import { agruparPorFecha, formatearHora } from '../franjas.js'
import { listarFranjasDisponibles, obtenerMiCita, reservarFranja } from '../services/agendaService.js'
import './Agenda.css'

function describir(franja) {
  return `${formatearFecha(franja.fecha)}, ${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`
}

export default function MiCita() {
  const ventana = useVentanaEtapa(ETAPAS.AGENDA_CITAS)
  const cita = useFetch(obtenerMiCita)
  const disponibles = useFetch(listarFranjasDisponibles)
  const [reservando, setReservando] = useState(null)
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)

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
          </Panel>
        )}

        <Panel titulo="Franjas disponibles">
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
