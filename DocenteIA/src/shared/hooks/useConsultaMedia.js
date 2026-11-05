import { useSyncExternalStore } from 'react'

/** ¿La ventana cumple la consulta de medios? Se actualiza al cambiar el tamaño. */
export function useConsultaMedia(consulta) {
  return useSyncExternalStore(
    (avisar) => {
      const lista = window.matchMedia?.(consulta)
      lista?.addEventListener?.('change', avisar)
      return () => lista?.removeEventListener?.('change', avisar)
    },
    () => Boolean(window.matchMedia?.(consulta).matches),
    () => false,
  )
}
