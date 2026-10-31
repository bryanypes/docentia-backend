import { useCallback, useEffect, useMemo, useState } from 'react'
import { alCerrarSesionPorExpiracion, guardarToken, leerToken } from '../../services/api.js'
import { iniciarSesion, iniciarSesionConGoogle, obtenerSesionActual } from './services/authService.js'
import { MOTIVO_CIERRE, SesionContext } from './sesionContext.js'

export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [validando, setValidando] = useState(() => Boolean(leerToken()))
  const [motivoCierre, setMotivoCierre] = useState(null)

  const cerrarSesion = useCallback((motivo = null) => {
    guardarToken(null)
    setUsuario(null)
    setMotivoCierre(motivo)
  }, [])

  useEffect(() => {
    alCerrarSesionPorExpiracion(() => cerrarSesion(MOTIVO_CIERRE.EXPIRADA))
  }, [cerrarSesion])

  useEffect(() => {
    if (!leerToken()) return
    let vigente = true
    obtenerSesionActual()
      .then((sesion) => vigente && setUsuario(sesion))
      .catch(() => vigente && cerrarSesion(MOTIVO_CIERRE.EXPIRADA))
      .finally(() => vigente && setValidando(false))
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
    () => ({ usuario, validando, motivoCierre, entrar, entrarConGoogle, cerrarSesion, reemplazarSesion }),
    [usuario, validando, motivoCierre, entrar, entrarConGoogle, cerrarSesion, reemplazarSesion],
  )

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}
