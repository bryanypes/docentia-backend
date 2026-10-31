import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { describirFechasEtapa } from '../etapas.js'

/**
 * Explica que la acción no está disponible porque su etapa está cerrada hoy.
 * Solo se muestra si la ventana se pudo consultar y está cerrada.
 */
export function AvisoEtapaCerrada({ ventana, accion }) {
  if (!ventana || ventana.abierta) return null
  return (
    <Aviso tipo="alerta" titulo={`${ventana.nombreEtapa}: etapa cerrada`}>
      {accion} solo es posible durante esta etapa. {describirFechasEtapa(ventana)}
    </Aviso>
  )
}
