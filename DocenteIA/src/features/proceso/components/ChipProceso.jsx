import { ESTADOS_PROCESO } from '../estados.js'

/** Estado del proceso como píldora de color, con su explicación al pasar el cursor. */
export function ChipProceso({ estado }) {
  const info = ESTADOS_PROCESO[estado]
  if (!info) return null
  return (
    <span className={`pildora pildora--${info.tono}`} title={info.ayuda}>
      {info.etiqueta}
    </span>
  )
}
