import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError, alCerrarSesionPorExpiracion, guardarToken, leerToken } from '../../services/api.js'
import { iniciarSesion, iniciarSesionConGoogle, obtenerSesionActual } from './services/authService.js'
import { MOTIVO_CIERRE, SesionContext } from './sesionContext.js'

export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [validando, setValidando] = useState(() => Boolean(leerToken()))
  const [motivoCierre, setMotivoCierre] = useState(null)
  // Solo si la sesión venció se vuelve a la pantalla de antes, y solo para la misma persona.
  const [idSesionVencida, setIdSesionVencida] = useState(null)

  const cerrarSesion = useCallback((motivo = null) => {
    guardarToken(null)
    setUsuario((actual) => {
      setIdSesionVencida(motivo === MOTIVO_CIERRE.EXPIRADA ? (actual?.idUsuario ?? null) : null)
      return null
    })
    setMotivoCierre(motivo)
  }, [])

  useEffect(() => {
    alCerrarSesionPorExpiracion(() => cerrarSesion(MOTIVO_CIERRE.EXPIRADA))
  }, [cerrarSesion])

  useEffect(() => {
    if (!leerToken()) return
    let vigente = true
    // Solo un 401 significa que la sesión ya no vale. Un corte de red, un 5xx o la validación
    // abortada al recargar la página no la cierran: se reintenta y, si sigue fallando, se conserva
    // el token para que la próxima carga vuelva a validarlo.
    async function validar() {
      for (let intento = 0; intento < 3 && vigente; intento += 1) {
        try {
          const sesion = await obtenerSesionActual()
          if (vigente) setUsuario(sesion)
          return
        } catch (error) {
          if (!vigente) return
          if (error instanceof ApiError && error.status === 401) {
            cerrarSesion(MOTIVO_CIERRE.EXPIRADA)
            return
          }
          await new Promise((listo) => setTimeout(listo, 500 * (intento + 1)))
        }
      }
    }
    validar().finally(() => vigente && setValidando(false))
    return () => {
      vigente = false
    }
  }, [cerrarSesion])

  const entrar = useCallback(async (correo, contrasena) => {
    const { token, usuario: sesion } = await iniciarSesion(correo, contrasena)
    guardarToken(token)
    setMotivoCierre(null)
    setUsuario(sesion)
    return sesion
  }, [])

  const entrarConGoogle = useCallback(async (idToken) => {
    const { token, usuario: sesion } = await iniciarSesionConGoogle(idToken)
    guardarToken(token)
    setMotivoCierre(null)
    setUsuario(sesion)
    return sesion
  }, [])

  /** Cambia a la sesión nueva que devuelve el backend, por ejemplo tras cambiar la contraseña. */
  const reemplazarSesion = useCallback(({ token, usuario: sesion }) => {
    guardarToken(token)
    setUsuario(sesion)
  }, [])

  const valor = useMemo(
    () => ({
      usuario,
      validando,
      motivoCierre,
      idSesionVencida,
      entrar,
      entrarConGoogle,
      cerrarSesion,
      reemplazarSesion,
    }),
    [usuario, validando, motivoCierre, idSesionVencida, entrar, entrarConGoogle, cerrarSesion, reemplazarSesion],
  )

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}
