import { describirFechasEtapa } from '../../../../features/periodos/etapas.js'
import { Aviso } from '../Aviso/Aviso.jsx'

/**
 * Muestra un ApiError como lo explica el backend. Si la etapa está cerrada (409) nombra la etapa y
 * sus fechas; si hay errores por campo, los lista con `nombresCampos`.
 */
export function ErrorApi({ error, nombresCampos = {} }) {
  if (!error) return null
  if (error.datos?.etapa) {
    return (
      <Aviso tipo="alerta" titulo={`${error.datos.nombreEtapa}: etapa cerrada`}>
        {describirFechasEtapa(error.datos)}
      </Aviso>
    )
  }
  const campos = Object.entries(error.errores ?? {})
  return (
    <Aviso tipo="error" titulo={error.message}>
      {campos.length > 0 && (
        <ul>
          {campos.map(([campo, motivo]) => (
            <li key={campo}>
              {nombresCampos[campo] ?? campo}: {motivo}
            </li>
          ))}
        </ul>
      )}
    </Aviso>
  )
}
