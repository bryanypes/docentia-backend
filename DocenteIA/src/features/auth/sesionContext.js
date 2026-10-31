import { createContext, useContext } from 'react'

export const SesionContext = createContext(null)

/** Motivos por los que se cerró la sesión sin que la persona pulsara "Salir"; el login los explica. */
export const MOTIVO_CIERRE = { EXPIRADA: 'EXPIRADA' }

export function useSesion() {
  const sesion = useContext(SesionContext)
  if (!sesion) throw new Error('useSesion debe usarse dentro de SesionProvider')
  return sesion
}
