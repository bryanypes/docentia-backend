import { useEffect, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'

export const TAMANOS = [10, 20, 50]

export function useFiltrosUrl(predeterminados) {
  const [parametros, setParametros] = useSearchParams()
  const valores = Object.fromEntries(
    Object.entries(predeterminados).map(([clave, predeterminado]) => [
      clave,
      parametros.get(clave) ?? predeterminado,
    ]),
  )

  function actualizar(cambios) {
    setParametros(
      (actuales) => {
        const nuevos = new URLSearchParams(actuales)
        let cambioFiltro = false
        for (const [clave, valor] of Object.entries(cambios)) {
          const predeterminado = predeterminados[clave]
          const anterior = nuevos.get(clave) ?? predeterminado
          const siguiente = valor == null ? predeterminado : String(valor)
          if (anterior !== siguiente) cambioFiltro = true
          if (siguiente === String(predeterminado)) nuevos.delete(clave)
          else nuevos.set(clave, siguiente)
        }
        if (cambioFiltro) {
          for (const clavePagina of [...nuevos.keys()].filter((clave) => clave.startsWith('pagina'))) {
            nuevos.delete(clavePagina)
          }
        }
        return nuevos
      },
      { replace: true },
    )
  }

  return [valores, actualizar]
}

/**
 * Paginación en el cliente con la página y el tamaño en la URL (?pagina=2&tamano=20). Cuando cambia
 * algo de `filtros` (búsqueda, filtros, orden) vuelve a la página 1.
 */
export function usePaginacion(filas, { clavePagina = 'pagina', claveTamano = 'tamano' } = {}) {
  const [parametros, setParametros] = useSearchParams()
  const tamanoParam = Number(parametros.get(claveTamano))
  const tamano = TAMANOS.includes(tamanoParam) ? tamanoParam : TAMANOS[0]
  const total = filas.length
  const paginas = Math.max(1, Math.ceil(total / tamano))
  const pedida = Math.max(1, Number.parseInt(parametros.get(clavePagina) ?? '1', 10) || 1)
  const pagina = Math.min(pedida, paginas)

  function fijar(cambios) {
    setParametros(
      (actuales) => {
        const nuevos = new URLSearchParams(actuales)
        for (const [clave, valor] of Object.entries(cambios)) {
          if (
            valor === null ||
            (clave === clavePagina && valor === 1) ||
            (clave === claveTamano && valor === TAMANOS[0])
          ) {
            nuevos.delete(clave)
          }
          else nuevos.set(clave, String(valor))
        }
        return nuevos
      },
      { replace: true },
    )
  }

  useEffect(() => {
    const paginaUrl = parametros.get(clavePagina)
    const tamanoUrl = parametros.get(claveTamano)
    const cambios = {}
    if (pagina !== pedida || (paginaUrl && Number(paginaUrl) !== pagina)) cambios[clavePagina] = pagina
    if (
      tamanoUrl &&
      (!TAMANOS.includes(Number(tamanoUrl)) || Number(tamanoUrl) === TAMANOS[0])
    ) {
      cambios[claveTamano] = TAMANOS[0]
    }
    if (Object.keys(cambios).length > 0) fijar(cambios)
    // URL normalization only depends on the requested and valid page size.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, pedida, parametros])

  const visibles = useMemo(() => filas.slice((pagina - 1) * tamano, pagina * tamano), [filas, pagina, tamano])

  return {
    visibles,
    pagina,
    paginas,
    tamano,
    total,
    desde: total === 0 ? 0 : (pagina - 1) * tamano + 1,
    hasta: Math.min(total, pagina * tamano),
    irA: (n) => fijar({ [clavePagina]: Math.min(Math.max(1, n), paginas) }),
    cambiarTamano: (n) => fijar({ [claveTamano]: n, [clavePagina]: null }),
  }
}
