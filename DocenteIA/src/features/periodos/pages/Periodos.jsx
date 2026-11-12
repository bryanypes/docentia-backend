import { CalendarPlus, Pencil, Power, PowerOff, RefreshCw } from 'lucide-react'
import { useRef, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { usePeriodoActivo } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { ETAPAS_PERIODO, validarFechasPeriodo } from '../etapas.js'
import {
  activarPeriodo,
  actualizarPeriodo,
  crearPeriodo,
  desactivarPeriodo,
  listarPeriodos,
} from '../services/periodosService.js'
import './Periodos.css'

const NOMBRES_CAMPOS = { nombre: 'Nombre', fechaInicio: 'Inicio', fechaFin: 'Fin', etapas: 'Etapas' }

function fechasDe(periodo) {
  return Object.fromEntries(
    ETAPAS_PERIODO.map(({ etapa }) => {
      const guardada = periodo?.etapas.find((e) => e.tipoEtapa === etapa)
      return [etapa, { fechaApertura: guardada?.fechaApertura ?? '', fechaCierre: guardada?.fechaCierre ?? '' }]
    }),
  )
}

export function FormularioPeriodo({ guardado, onGuardar, onCancelar }) {
  const [nombre, setNombre] = useState(guardado?.nombre ?? '')
  const [fechaInicio, setFechaInicio] = useState(guardado?.fechaInicio ?? '')
  const [fechaFin, setFechaFin] = useState(guardado?.fechaFin ?? '')
  const [fechas, setFechas] = useState(() => fechasDe(guardado))
  const [erroresFechas, setErroresFechas] = useState([])
  const [errorEnvio, setErrorEnvio] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const resumen = useRef(null)

  function cambiarFecha(etapa, campo, valor) {
    setFechas((f) => ({ ...f, [etapa]: { ...f[etapa], [campo]: valor } }))
  }

  async function enviar(evento) {
    evento.preventDefault()
    // Una etapa sin ninguna fecha no se programa: cuenta como cerrada.
    const etapas = ETAPAS_PERIODO.map(({ etapa }) => ({ tipoEtapa: etapa, ...fechas[etapa] })).filter(
      (e) => e.fechaApertura || e.fechaCierre,
    )
    const periodo = { nombre: nombre.trim(), fechaInicio, fechaFin, etapas }
    const errores = validarFechasPeriodo(periodo)
    setErroresFechas(errores)
    setErrorEnvio(null)
    if (errores.length > 0) {
      requestAnimationFrame(() => resumen.current?.focus())
      return
    }
    setEnviando(true)
    try {
      await onGuardar(periodo)
    } catch (error) {
      setErrorEnvio(error)
      setEnviando(false)
    }
  }

  return (
    <Panel
      titulo={guardado ? `Editar periodo ${guardado.nombre}` : 'Nuevo periodo'}
      descripcion="Cada acción solo se permite mientras su etapa está abierta. Una etapa sin fechas queda cerrada."
      icono={CalendarPlus}
    >
      <form className="periodos__formulario" onSubmit={enviar}>
        <div className="periodos__datos">
          <Campo id="nombrePeriodo" etiqueta="Nombre">
            <input
              id="nombrePeriodo"
              className="campo__control"
              value={nombre}
              maxLength={20}
              placeholder="2027-1"
              onChange={(e) => setNombre(e.target.value)}
              required
            />
          </Campo>
          <Campo id="fechaInicioPeriodo" etiqueta="Inicio">
            <input
              id="fechaInicioPeriodo"
              type="date"
              className="campo__control"
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              required
            />
          </Campo>
          <Campo id="fechaFinPeriodo" etiqueta="Fin">
            <input
              id="fechaFinPeriodo"
              type="date"
              className="campo__control"
              value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              required
            />
          </Campo>
        </div>

        <fieldset className="periodos__etapas">
          <legend className="rotulo">Etapas</legend>
          {ETAPAS_PERIODO.map(({ etapa, nombre: nombreEtapa, habilita }) => (
            <div key={etapa} className="periodos__etapa">
              <div>
                <strong>{nombreEtapa}</strong>
                <span className="campo__ayuda">{habilita}</span>
              </div>
              <input
                type="date"
                className="campo__control"
                aria-label={`Apertura de ${nombreEtapa}`}
                value={fechas[etapa].fechaApertura}
                onChange={(e) => cambiarFecha(etapa, 'fechaApertura', e.target.value)}
              />
              <input
                type="date"
                className="campo__control"
                aria-label={`Cierre de ${nombreEtapa}`}
                value={fechas[etapa].fechaCierre}
                onChange={(e) => cambiarFecha(etapa, 'fechaCierre', e.target.value)}
              />
            </div>
          ))}
        </fieldset>

        {erroresFechas.length > 0 && (
          <div ref={resumen} tabIndex={-1}>
            <Aviso tipo="error" titulo="Revisa las fechas">
              <ul>
                {erroresFechas.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            </Aviso>
          </div>
        )}
        <ErrorApi error={errorEnvio} nombresCampos={NOMBRES_CAMPOS} />

        <div className="periodos__acciones">
          <Boton type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : guardado ? 'Guardar cambios' : 'Crear periodo'}
          </Boton>
          {guardado && (
            <Boton variante="contorno" onClick={onCancelar}>
              Cancelar
            </Boton>
          )}
        </div>
      </form>
    </Panel>
  )
}

function EtapasDe({ periodo, ventanas }) {
  return (
    <ul className="periodos__lista-etapas">
      {ETAPAS_PERIODO.map(({ etapa, nombre }) => {
        const ventana = periodo.esActivo ? ventanas?.find((v) => v.etapa === etapa) : null
        const fechas = periodo.etapas.find((e) => e.tipoEtapa === etapa)
        return (
          <li key={etapa}>
            <strong>{nombre}</strong>
            <span>
              {fechas
                ? `${formatearFecha(fechas.fechaApertura)} – ${formatearFecha(fechas.fechaCierre)}`
                : 'Sin programar'}
            </span>
            {ventana && (
              <span className={`pildora pildora--${ventana.abierta ? 'exito' : 'neutro'}`}>
                {ventana.abierta ? 'Abierta hoy' : 'Cerrada hoy'}
              </span>
            )}
          </li>
        )
      })}
    </ul>
  )
}

export default function Periodos() {
  const periodos = useFetch(listarPeriodos)
  const { ventanas, recargar: recargarActivo } = usePeriodoActivo()
  const [enEdicion, setEnEdicion] = useState(null)
  const [version, setVersion] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [errorAccion, setErrorAccion] = useState(null)

  function recargar() {
    periodos.recargar()
    recargarActivo?.()
  }

  async function guardar(periodo) {
    await (enEdicion ? actualizarPeriodo(enEdicion.idPeriodo, periodo) : crearPeriodo(periodo))
    setResultado(`Periodo ${periodo.nombre} ${enEdicion ? 'actualizado' : 'creado'}.`)
    setEnEdicion(null)
    setVersion((v) => v + 1)
    recargar()
  }

  async function cambiarActivacion(periodo, activo) {
    if (
      !periodo.esActivo &&
      activo &&
      !window.confirm(
        `Solo puede haber un periodo activo. Activar ${periodo.nombre} desactivará ${activo.nombre}. ¿Continuar?`,
      )
    ) {
      return
    }
    setErrorAccion(null)
    setResultado(null)
    try {
      await (periodo.esActivo ? desactivarPeriodo(periodo.idPeriodo) : activarPeriodo(periodo.idPeriodo))
      setResultado(`Periodo ${periodo.nombre} ${periodo.esActivo ? 'desactivado' : 'activado'}.`)
      recargar()
    } catch (error) {
      setErrorAccion(error)
    }
  }

  const lista = periodos.datos ?? []
  const activo = lista.find((p) => p.esActivo)

  return (
    <>
      <Encabezado
        miga="Vicerrectoría · Periodos"
        titulo="Periodos académicos"
        subtitulo="Crea los periodos, define las ventanas de cada etapa y activa el periodo en curso."
      />
      <div className="pagina">
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <ErrorApi error={errorAccion} />
        <FormularioPeriodo
          key={`${enEdicion?.idPeriodo ?? 'nuevo'}-${version}`}
          guardado={enEdicion}
          onGuardar={guardar}
          onCancelar={() => setEnEdicion(null)}
        />

        {periodos.cargando && !periodos.datos && <Cargando texto="Cargando periodos" bloques={2} />}
        {periodos.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar los periodos" mensaje={periodos.error.message}>
            <Boton icono={RefreshCw} onClick={periodos.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!periodos.cargando && !periodos.error && lista.length === 0 && (
          <EstadoVista titulo="Aún no hay periodos" mensaje="Crea el primero con el formulario de arriba." />
        )}

        {lista.map((periodo) => (
          <Panel
            key={periodo.idPeriodo}
            titulo={periodo.nombre}
            descripcion={`${formatearFecha(periodo.fechaInicio)} – ${formatearFecha(periodo.fechaFin)}`}
            acciones={
              <div className="periodos__acciones">
                {periodo.esActivo && <span className="pildora pildora--exito">Activo</span>}
                <Boton variante="contorno" icono={Pencil} onClick={() => setEnEdicion(periodo)}>
                  Editar
                </Boton>
                <Boton
                  variante={periodo.esActivo ? 'contorno' : 'primario'}
                  icono={periodo.esActivo ? PowerOff : Power}
                  onClick={() => cambiarActivacion(periodo, activo)}
                >
                  {periodo.esActivo ? 'Desactivar' : 'Activar'}
                </Boton>
              </div>
            }
          >
            <EtapasDe periodo={periodo} ventanas={ventanas} />
          </Panel>
        ))}
      </div>
    </>
  )
}
