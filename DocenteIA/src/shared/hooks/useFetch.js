import { useCallback, useEffect, useState } from 'react'

/**
 * Ejecuta una función asíncrona al montar y cada vez que cambian sus dependencias.
 * Devuelve { datos, error, cargando, recargar }.
 */
export function useFetch(cargar, dependencias = []) {
  const [estado, setEstado] = useState({ datos: null, error: null, cargando: true })
  const [version, setVersion] = useState(0)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const cargarMemo = useCallback(cargar, dependencias)

  useEffect(() => {
    let vigente = true
    setEstado((previo) => ({ ...previo, error: null, cargando: true }))
    cargarMemo()
      .then((datos) => vigente && setEstado({ datos, error: null, cargando: false }))
      .catch((error) => vigente && setEstado({ datos: null, error, cargando: false }))
    return () => {
      vigente = false
    }
  }, [cargarMemo, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  return { ...estado, recargar }
}
