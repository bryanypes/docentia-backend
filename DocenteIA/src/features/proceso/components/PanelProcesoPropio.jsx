import { Hourglass, Route } from 'lucide-react'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { ESTADOS_PROCESO } from '../estados.js'
import { obtenerProcesoPropio } from '../services/procesoService.js'
import { ChipProceso } from './ChipProceso.jsx'
import { HitosProceso, PasosProceso } from './LineaProceso.jsx'
import './Proceso.css'

/**
 * El proceso visto por el docente. Mientras estudiantes y coordinación no terminen solo sabe que su
 * evaluación está en curso; después ve cada paso hasta el acta firmada.
 */
export function PanelProcesoPropio({ version = 0 }) {
  const proceso = useFetch(obtenerProcesoPropio, [version])
  const datos = proceso.datos
  if (!datos) return null

  if (datos.estado === 'EVALUACIONES_EN_CURSO') {
    return (
      <Panel titulo="Mi proceso de evaluación" icono={Hourglass} tono="cielo">
        <p className="proceso__texto">
          Tu evaluación está en curso. Cuando tus estudiantes y la coordinación terminen, aquí verás cada paso: tu
          autoevaluación, tu cita, la reunión y el acta.
        </p>
      </Panel>
    )
  }

  return (
    <Panel
      titulo="Mi proceso de evaluación"
      descripcion={ESTADOS_PROCESO[datos.estado]?.ayuda}
      icono={Route}
      tono="turquesa"
      acciones={<ChipProceso estado={datos.estado} />}
    >
      <PasosProceso proceso={datos} />
      <h3 className="proceso__subtitulo">Trazabilidad</h3>
      <HitosProceso hitos={datos.hitos} />
    </Panel>
  )
}
