import { createContext, useContext } from 'react'

export const PeriodoContext = createContext({ periodo: null, ventanas: null, cargando: false })

export function usePeriodoActivo() {
  return useContext(PeriodoContext)
}

/**
 * Ventana de una etapa del periodo activo: { etapa, nombreEtapa, fechaApertura, fechaCierre, abierta }.
 * Devuelve null mientras carga o si no se pudo consultar; en ese caso la UI no bloquea nada.
 */
export function useVentanaEtapa(etapa) {
  const { ventanas } = useContext(PeriodoContext)
  if (!ventanas) return null
  return ventanas.find((v) => v.etapa === etapa) ?? null
}
