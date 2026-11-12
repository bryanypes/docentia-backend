import { createContext, useContext } from 'react'

/**
 * Sin proveedor (algunas pruebas aisladas) confirma con el diálogo del navegador y no muestra
 * notificaciones, para que ningún componente falle.
 */
const RESPALDO = {
  notificar: () => {},
  confirmar: ({ titulo, mensaje }) => Promise.resolve(window.confirm([titulo, mensaje].filter(Boolean).join('\n'))),
}

export const AvisosContext = createContext(RESPALDO)

/** { notificar({ tipo, titulo, mensaje }), confirmar({ tipo, titulo, mensaje, textoConfirmar }) } */
export function useAvisos() {
  return useContext(AvisosContext)
}
