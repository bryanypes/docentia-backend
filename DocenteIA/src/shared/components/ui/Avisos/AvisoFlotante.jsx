import { useEffect } from 'react'
import { useAvisos } from './avisosContext.js'

/**
 * Convierte un mensaje de resultado en una notificación flotante, en lugar de un aviso que empuja el
 * contenido. Avisa cada vez que `mensaje` cambia a un texto nuevo.
 */
export function AvisoFlotante({ mensaje, tipo = 'exito', detalle }) {
  const { notificar } = useAvisos()
  useEffect(() => {
    if (mensaje) notificar({ tipo, titulo: mensaje, mensaje: detalle })
    // El detalle acompaña al mensaje; solo un mensaje nuevo dispara el aviso.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mensaje, tipo, notificar])
  return null
}
