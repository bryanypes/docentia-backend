import { ListChecks, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, formatearFechaHora } from '../../../shared/utils/formatters.js'
import { marcarCumplido, obtenerPlanDocente, obtenerPlanPropio } from '../services/actasService.js'
import './PanelActa.css'

const ESTADO_PLAN = {
  PENDIENTE: { etiqueta: 'Pendiente', clase: 'info' },
  CUMPLIDO: { etiqueta: 'Cumplido', clase: 'exito' },
  VENCIDO: { etiqueta: 'Vencido', clase: 'error' },
}

function Compromiso({ compromiso, puedeMarcar, onMarcado }) {
  const [abierto, setAbierto] = useState(false)
  const [observacion, setObservacion] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const estado = ESTADO_PLAN[compromiso.estado]
  const id = `observacion-${compromiso.idCompromiso}`

  async function marcar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await marcarCumplido(compromiso.idCompromiso, observacion.trim())
      onMarcado()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <li>
      <div className="plan__fila">
        <div>
          <strong>{compromiso.descripcion}</strong>
          <div className="plan__meta">Plazo {formatearFecha(compromiso.fechaLimite)}</div>
        </div>
        <span className={`pildora pildora--${estado.clase}`}>{estado.etiqueta}</span>
      </div>
      {compromiso.historial.map((cambio) => (
        <p key={cambio.fecha} className="plan__meta">
          {cambio.nombreUsuario} · {formatearFechaHora(cambio.fecha)}: {cambio.observacion}
        </p>
      ))}
      {puedeMarcar && compromiso.estado !== 'CUMPLIDO' && !abierto && (
        <Boton variante="contorno" onClick={() => setAbierto(true)}>
          Marcar cumplido
        </Boton>
      )}
      {abierto && (
        <form className="acta__formulario" onSubmit={marcar}>
          <Campo id={id} etiqueta="Observación">
            <textarea
              id={id}
              className="campo__control"
              rows={2}
              maxLength={500}
              required
              value={observacion}
              onChange={(e) => setObservacion(e.target.value)}
            />
          </Campo>
          <Boton type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Confirmar cumplimiento'}
          </Boton>
        </form>
      )}
      <ErrorApi error={error} />
    </li>
  )
}

/** Compromisos del periodo anterior con su estado. Sin `idDocente` muestra los propios. */
export function PanelSeguimientoPlan({ idDocente, puedeMarcar = false }) {
  const cargar = useCallback(
    () => (idDocente ? obtenerPlanDocente(idDocente) : obtenerPlanPropio()),
    [idDocente],
  )
  const plan = useFetch(cargar, [cargar])
  const compromisos = plan.datos?.compromisos ?? []
  const anterior = plan.datos?.periodoEvaluado?.nombre

  return (
    <Panel
      titulo="Seguimiento del plan"
      descripcion={anterior ? `Compromisos del periodo ${anterior}.` : 'Compromisos del periodo anterior.'}
      icono={ListChecks}
    >
      {plan.cargando && !plan.datos && <Cargando texto="Cargando el seguimiento" bloques={1} />}
      {plan.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar el seguimiento" mensaje={plan.error.message}>
          <Boton icono={RefreshCw} onClick={plan.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      )}
      {plan.datos && compromisos.length === 0 && <p className="campo__ayuda">No hay compromisos por revisar.</p>}
      {compromisos.length > 0 && (
        <ul className="plan__lista">
          {compromisos.map((compromiso) => (
            <Compromiso
              key={compromiso.idCompromiso}
              compromiso={compromiso}
              puedeMarcar={puedeMarcar}
              onMarcado={plan.recargar}
            />
          ))}
        </ul>
      )}
    </Panel>
  )
}
