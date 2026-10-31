import { CalendarCheck, CalendarClock, CalendarX } from 'lucide-react'
import { usePeriodoActivo } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import './PanelEtapas.css'

function estadoEtapa(ventana) {
  if (!ventana.fechaApertura || !ventana.fechaCierre) {
    return { texto: 'Sin programar', clase: 'neutro', Icono: CalendarClock }
  }
  return ventana.abierta
    ? { texto: 'Abierta hoy', clase: 'exito', Icono: CalendarCheck }
    : { texto: 'Cerrada hoy', clase: 'alerta', Icono: CalendarX }
}

/** Las cuatro etapas del periodo activo con sus fechas y si hoy están abiertas. */
export function PanelEtapas() {
  const { periodo, ventanas } = usePeriodoActivo()
  if (!periodo || !ventanas?.length) return null

  return (
    <Panel
      titulo={`Etapas del periodo ${periodo.nombre}`}
      descripcion="Cada acción solo se permite mientras su etapa está abierta. Las fechas son inclusivas."
    >
      <ol className="etapas">
        {ventanas.map((v) => {
          const estado = estadoEtapa(v)
          return (
            <li key={v.etapa} className={`etapas__item${v.abierta ? ' etapas__item--abierta' : ''}`}>
              <strong className="etapas__nombre">{v.nombreEtapa}</strong>
              <span className="etapas__fechas">
                {v.fechaApertura && v.fechaCierre
                  ? `${formatearFecha(v.fechaApertura)} – ${formatearFecha(v.fechaCierre)}`
                  : 'Fechas por definir'}
              </span>
              <span className={`pildora pildora--${estado.clase} pildora--con-icono`}>
                <estado.Icono size={13} aria-hidden="true" />
                {estado.texto}
              </span>
            </li>
          )
        })}
      </ol>
    </Panel>
  )
}
