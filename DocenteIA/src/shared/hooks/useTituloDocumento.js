import { useEffect } from 'react'

/** Pone "<título> · DOCENTIA" en la pestaña del navegador; los lectores de pantalla lo anuncian al navegar. */
export function useTituloDocumento(titulo) {
  useEffect(() => {
    document.title = titulo ? `${titulo} · DOCENTIA` : 'DOCENTIA'
  }, [titulo])
}
