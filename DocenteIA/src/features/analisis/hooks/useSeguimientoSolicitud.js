import { useCallback, useEffect, useRef, useState } from 'react'
import { estaEnCurso } from '../estados.js'
import { obtenerSolicitud } from '../services/analisisService.js'

export const INTERVALO_SONDEO_MS = 2500
export const MAXIMO_CONSULTAS = 48 // unos dos minutos

/**
 * Consulta la solicitud y, mientras esté en cola o en proceso, vuelve a consultar cada 2,5 s hasta
 * un tope. Al salir de la pantalla o cambiar de solicitud cancela la petición y el temporizador.
 */
export function useSeguimientoSolicitud(idSolicitud) {
  const [estado, setEstado] = useState({ idSolicitud: null, solicitud: null, error: null })
  const [agotado, setAgotado] = useState(false)
  const [intento, setIntento] = useState(0)
  const consultas = useRef(0)

  useEffect(() => {
    if (!idSolicitud) return undefined
    const controlador = new AbortController()
    let temporizador = null
    consultas.current = 0

    async function consultar() {
      consultas.current += 1
      try {
        const solicitud = await obtenerSolicitud(idSolicitud, controlador.signal)
        setEstado({ idSolicitud, solicitud, error: null })
        setAgotado(false)
        if (!estaEnCurso(solicitud)) return
        if (consultas.current >= MAXIMO_CONSULTAS) {
          setAgotado(true)
          return
        }
        temporizador = setTimeout(consultar, INTERVALO_SONDEO_MS)
      } catch (error) {
        if (error?.name === 'AbortError') return
        setEstado((actual) => ({ ...actual, idSolicitud, error }))
      }
    }

    consultar()
    return () => {
      controlador.abort()
      clearTimeout(temporizador)
    }
  }, [idSolicitud, intento])

  const reanudar = useCallback(() => setIntento((actual) => actual + 1), [])
  const esDeEsta = estado.idSolicitud === idSolicitud

  return {
    solicitud: esDeEsta ? estado.solicitud : null,
    error: esDeEsta ? estado.error : null,
    agotado,
    reanudar,
  }
}
