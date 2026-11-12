import { useEffect } from 'react'
import { describirFechasEtapa } from '../../../../features/periodos/etapas.js'
import { Aviso } from '../Aviso/Aviso.jsx'
import { useAvisos } from '../Avisos/avisosContext.js'

function describir(error, nombresCampos) {
  if (error.datos?.etapa) {
    return { tipo: 'alerta', titulo: `${error.datos.nombreEtapa}: etapa cerrada`, detalle: describirFechasEtapa(error.datos) }
  }
  const campos = Object.entries(error.errores ?? {})
  return {
    tipo: 'error',
    titulo: error.message,
    detalle:
      campos.length > 0 ? (
        <ul>
          {campos.map(([campo, motivo]) => (
            <li key={campo}>
              {nombresCampos[campo] ?? campo}: {motivo}
            </li>
          ))}
        </ul>
      ) : null,
  }
}

/**
 * Muestra un ApiError como lo explica el backend. Si la etapa está cerrada (409) nombra la etapa y
 * sus fechas; si hay errores por campo, los lista con `nombresCampos`. Con `flotante` sale como
 * notificación y no empuja el contenido de la página.
 */
export function ErrorApi({ error, nombresCampos = {}, flotante = false }) {
  const { notificar } = useAvisos()
  useEffect(() => {
    if (!flotante || !error) return
    const { tipo, titulo, detalle } = describir(error, nombresCampos)
    notificar({ tipo, titulo, mensaje: detalle })
    // Los nombres de campo son estables; solo un error nuevo dispara el aviso.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error, flotante, notificar])

  if (!error || flotante) return null
  const { tipo, titulo, detalle } = describir(error, nombresCampos)
  return (
    <Aviso tipo={tipo} titulo={titulo}>
      {detalle}
    </Aviso>
  )
}
