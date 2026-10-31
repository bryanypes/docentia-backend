import { CalendarPlus, CalendarRange, RefreshCw, Trash2, X } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { agruparPorFecha, finDeFranja, formatearHora, franjasEntre, hoyEnColombia } from '../franjas.js'
import {
  abrirFranja,
  abrirFranjasEnLote,
  cerrarFranja,
  cerrarFranjasEnLote,
  eliminarFranja,
  listarFranjas,
} from '../services/agendaService.js'
import './Agenda.css'

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

function FormularioFranja({ onAbierta }) {
  const [fecha, setFecha] = useState('')
  const [horaInicio, setHoraInicio] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const horaFin = finDeFranja(horaInicio)

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await abrirFranja({ fecha, horaInicio, horaFin })
      onAbierta(`Franja del ${formatearFecha(fecha)} a las ${horaInicio} abierta.`)
      setHoraInicio('')
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Panel titulo="Abrir una franja" descripcion="Cada franja dura 30 minutos." icono={CalendarPlus}>
      <form className="formulario-simple" onSubmit={enviar}>
        <Campo id="fechaFranja" etiqueta="Fecha">
          <input
            id="fechaFranja"
            type="date"
            className="campo__control"
            min={hoyEnColombia()}
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            required
          />
        </Campo>
        <Campo id="inicioFranja" etiqueta="Hora de inicio" ayuda={`Termina: ${horaFin ?? '—'}`}>
          <input
            id="inicioFranja"
            type="time"
            className="campo__control"
            value={horaInicio}
            aria-describedby="inicioFranja-ayuda"
            onChange={(e) => setHoraInicio(e.target.value)}
            required
          />
        </Campo>
        <Boton type="submit" disabled={enviando || !horaFin}>
          {enviando ? 'Abriendo…' : 'Abrir franja'}
        </Boton>
        <ErrorApi error={error} />
      </form>
    </Panel>
  )
}

function FormularioLote({ onAplicado }) {
  const [fecha, setFecha] = useState('')
  const [desde, setDesde] = useState('08:00')
  const [hasta, setHasta] = useState('10:00')
  const [error, setError] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const franjas = fecha && desde && hasta ? franjasEntre(fecha, desde, hasta) : []

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    setReporte(null)
    try {
      setReporte(await abrirFranjasEnLote(franjas))
      onAplicado()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Panel titulo="Abrir varias franjas seguidas" icono={CalendarRange}>
      <form className="formulario-simple" onSubmit={enviar}>
        <Campo id="fechaLote" etiqueta="Fecha del bloque">
          <input
            id="fechaLote"
            type="date"
            className="campo__control"
            min={hoyEnColombia()}
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            required
          />
        </Campo>
        <Campo id="desdeLote" etiqueta="Desde">
          <input
            id="desdeLote"
            type="time"
            className="campo__control"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            required
          />
        </Campo>
        <Campo id="hastaLote" etiqueta="Hasta">
          <input
            id="hastaLote"
            type="time"
            className="campo__control"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
            required
          />
        </Campo>
        <Boton type="submit" disabled={enviando || franjas.length === 0}>
          {enviando ? 'Abriendo…' : `Abrir ${franjas.length} franjas`}
        </Boton>
        <ErrorApi error={error} />
        {reporte && <ReporteLote reporte={reporte} />}
      </form>
    </Panel>
  )
}

function ListaFranjas({ version }) {
  const [fecha, setFecha] = useState('')
  const [estado, setEstado] = useState('')
  const [seleccion, setSeleccion] = useState([])
  const [error, setError] = useState(null)
  const [reporte, setReporte] = useState(null)
  const [resultado, setResultado] = useState(null)
  const cargar = useCallback(() => listarFranjas({ fecha, estado }), [fecha, estado])
  const franjas = useFetch(cargar, [cargar, version])

  function alternar(idFranja) {
    setSeleccion((actual) =>
      actual.includes(idFranja) ? actual.filter((id) => id !== idFranja) : [...actual, idFranja],
    )
  }

  async function ejecutar(accion, mensaje) {
    setError(null)
    setReporte(null)
    setResultado(null)
    try {
      const respuesta = await accion()
      if (respuesta?.aplicadas) setReporte(respuesta)
      else setResultado(mensaje)
      setSeleccion([])
      franjas.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  function eliminar(franja) {
    if (!window.confirm(`¿Eliminar la franja del ${describirFranja(franja)}?`)) return
    ejecutar(() => eliminarFranja(franja.idFranja), 'Franja eliminada.')
  }

  const lista = franjas.datos ?? []

  return (
    <Panel
      titulo="Mis franjas"
      acciones={
        <Boton
          variante="contorno"
          disabled={seleccion.length === 0}
          onClick={() => ejecutar(() => cerrarFranjasEnLote(seleccion))}
        >
          Cerrar seleccionadas ({seleccion.length})
        </Boton>
      }
    >
      <div className="agenda__filtros">
        <Campo id="filtroFechaFranjas" etiqueta="Filtrar por fecha">
          <input
            id="filtroFechaFranjas"
            type="date"
            className="campo__control"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
        </Campo>
        <Campo id="filtroEstadoFranjas" etiqueta="Estado">
          <select
            id="filtroEstadoFranjas"
            className="campo__control"
            value={estado}
            onChange={(e) => setEstado(e.target.value)}
          >
            <option value="">Todas</option>
            <option value="ABIERTA">Abiertas</option>
            <option value="CERRADA">Cerradas</option>
          </select>
        </Campo>
      </div>

      {resultado && <Aviso tipo="exito" titulo={resultado} />}
      <ErrorApi error={error} />
      {reporte && <ReporteLote reporte={reporte} />}

      {franjas.cargando && !franjas.datos && <Cargando texto="Cargando franjas" bloques={1} />}
      {franjas.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar las franjas" mensaje={franjas.error.message}>
          <Boton icono={RefreshCw} onClick={franjas.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      )}
      {!franjas.cargando && !franjas.error && lista.length === 0 && (
        <EstadoVista titulo="Sin franjas" mensaje="No hay franjas con estos filtros." />
      )}

      {agruparPorFecha(lista).map(([dia, delDia]) => (
        <div key={dia} className="agenda__dia">
          <h3 className="agenda__fecha">{formatearFecha(dia)}</h3>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">
                    <span className="solo-lectores">Seleccionar</span>
                  </th>
                  <th scope="col">Horario</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Reserva</th>
                  <th scope="col">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {delDia.map((franja) => {
                  const horario = `${formatearHora(franja.horaInicio)}–${formatearHora(franja.horaFin)}`
                  const abierta = franja.estado === 'ABIERTA'
                  return (
                    <tr key={franja.idFranja}>
                      <td>
                        {abierta && !franja.reservada && (
                          <input
                            type="checkbox"
                            aria-label={`Seleccionar ${horario}`}
                            checked={seleccion.includes(franja.idFranja)}
                            onChange={() => alternar(franja.idFranja)}
                          />
                        )}
                      </td>
                      <td className="tabla__fuerte">{horario}</td>
                      <td>
                        <span className={`pildora pildora--${abierta ? 'exito' : 'neutro'}`}>
                          {abierta ? 'Abierta' : 'Cerrada'}
                        </span>
                      </td>
                      <td>{franja.reserva?.nombreDocente ?? '—'}</td>
                      <td>
                        <div className="acciones-fila">
                          {abierta && (
                            <Boton
                              variante="enlace"
                              icono={X}
                              aria-label={`Cerrar la franja ${horario}`}
                              onClick={() => ejecutar(() => cerrarFranja(franja.idFranja), 'Franja cerrada.')}
                            >
                              Cerrar
                            </Boton>
                          )}
                          <Boton
                            variante="enlace"
                            icono={Trash2}
                            aria-label={`Eliminar la franja ${horario}`}
                            onClick={() => eliminar(franja)}
                          >
                            Eliminar
                          </Boton>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </Panel>
  )
}

export default function Agenda() {
  const ventana = useVentanaEtapa(ETAPAS.AGENDA_CITAS)
  const [version, setVersion] = useState(0)
  const [resultado, setResultado] = useState(null)
  const recargar = () => setVersion((v) => v + 1)

  return (
    <>
      <Encabezado
        miga="Coordinación · Agenda de citas"
        titulo="Agenda de citas"
        subtitulo="Franjas de 30 minutos para las citas de retroalimentación con los docentes de tu facultad."
      />
      <div className="pagina">
        <AvisoEtapaCerrada ventana={ventana} accion="Abrir, cerrar o eliminar franjas" />
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <FormularioFranja
          onAbierta={(mensaje) => {
            setResultado(mensaje)
            recargar()
          }}
        />
        <FormularioLote onAplicado={recargar} />
        <ListaFranjas version={version} />
      </div>
    </>
  )
}
