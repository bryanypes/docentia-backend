import { House, LogIn } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotEsperando from '../../../assets/ilustraciones/robot-esperando.svg'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { SolicitarAyuda } from '../../../shared/components/ui/SolicitarAyuda/SolicitarAyuda.jsx'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { PantallaAcceso } from '../components/PantallaAcceso.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import {
  obtenerDisponibilidadRecuperacion,
  restablecerContrasena,
  solicitarRecuperacion,
} from '../services/authService.js'
import './RecuperarContrasena.css'

/**
 * Pide el correo; la respuesta es siempre la misma, exista o no la cuenta. Si el servidor no tiene
 * correo configurado, no promete un enlace: dice a quién pedir una contraseña nueva.
 */
function Solicitud({ alCambiarEstado }) {
  const cargarDisponibilidad = useCallback(() => obtenerDisponibilidadRecuperacion(), [])
  const disponibilidad = useFetch(cargarDisponibilidad, [cargarDisponibilidad])
  const [correo, setCorreo] = useState('')
  const [mensaje, setMensaje] = useState(null)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      const respuesta = await solicitarRecuperacion(correo.trim())
      setMensaje(respuesta.mensaje)
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  const sinCorreo = Boolean(disponibilidad.datos) && !disponibilidad.datos.porCorreo
  useEffect(() => {
    alCambiarEstado(mensaje ? 'listo' : sinCorreo ? 'espera' : null)
  }, [mensaje, sinCorreo, alCambiarEstado])

  if (sinCorreo) {
    return (
      <Aviso tipo="info" titulo="Pide una nueva contraseña a la Vicerrectoría">
        En este servidor DOCENTIA no envía correos. Escribe a la Vicerrectoría: la administración de DOCENTIA
        te asigna una contraseña temporal que cambias al entrar. Los estudiantes entran con su cuenta de Google.
      </Aviso>
    )
  }

  if (mensaje) {
    return (
      <Aviso tipo="exito" titulo="Revisa tu correo">
        {mensaje}
      </Aviso>
    )
  }
  return (
    <form onSubmit={enviar} className="recuperar__formulario">
      <p>Escribe tu correo institucional y te enviaremos un enlace para crear una contraseña nueva.</p>
      <Campo id="correoRecuperacion" etiqueta="Correo institucional">
        <input
          id="correoRecuperacion"
          type="email"
          className="campo__control"
          autoComplete="email"
          required
          value={correo}
          onChange={(e) => setCorreo(e.target.value)}
        />
      </Campo>
      <ErrorApi error={error} />
      <Boton type="submit" disabled={enviando || !correo.trim()}>
        {enviando ? 'Enviando…' : 'Enviar enlace'}
      </Boton>
    </form>
  )
}

/** Fija la contraseña nueva con el token del enlace. */
function Restablecimiento({ token, alCambiarEstado }) {
  const [clave, setClave] = useState('')
  const [repetida, setRepetida] = useState('')
  const [listo, setListo] = useState(false)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  useEffect(() => {
    alCambiarEstado(listo ? 'listo' : null)
  }, [listo, alCambiarEstado])
  const problema =
    clave && clave.length < 8 ? 'Usa al menos 8 caracteres.' : repetida && repetida !== clave ? 'Las contraseñas no coinciden.' : null

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await restablecerContrasena(token, clave)
      setListo(true)
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  if (listo) {
    return (
      <Aviso tipo="exito" titulo="Contraseña actualizada">
        Ya puedes <Link to="/login">iniciar sesión</Link> con tu contraseña nueva.
      </Aviso>
    )
  }
  return (
    <form onSubmit={enviar} className="recuperar__formulario">
      <Campo id="claveNueva" etiqueta="Contraseña nueva" ayuda="Entre 8 y 72 caracteres.">
        <input
          id="claveNueva"
          type="password"
          className="campo__control"
          autoComplete="new-password"
          maxLength={72}
          required
          value={clave}
          onChange={(e) => setClave(e.target.value)}
        />
      </Campo>
      <Campo id="claveRepetida" etiqueta="Repite la contraseña" error={problema}>
        <input
          id="claveRepetida"
          type="password"
          className="campo__control"
          autoComplete="new-password"
          maxLength={72}
          required
          value={repetida}
          onChange={(e) => setRepetida(e.target.value)}
        />
      </Campo>
      <ErrorApi error={error} />
      <Boton type="submit" disabled={enviando || !clave || clave !== repetida || Boolean(problema)}>
        {enviando ? 'Guardando…' : 'Guardar contraseña'}
      </Boton>
    </form>
  )
}

const AVATAR = { listo: robotAprobada, espera: robotEsperando }

export default function RecuperarContrasena() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token')
  const titulo = token ? 'Crea tu contraseña nueva' : '¿Olvidaste tu contraseña?'
  useTituloDocumento(titulo)
  const [estado, setEstado] = useState(null)
  const alCambiarEstado = useCallback((nuevo) => setEstado(nuevo), [])

  return (
    <PantallaAcceso avatar={AVATAR[estado] ?? robotSaludo}>
      <div className="recuperar__contenido">
        <h1 className="recuperar__titulo">{titulo}</h1>
        {token ? (
          <Restablecimiento token={token} alCambiarEstado={alCambiarEstado} />
        ) : (
          <Solicitud alCambiarEstado={alCambiarEstado} />
        )}
      </div>
      <nav className="recuperar__acciones" aria-label="Otras opciones">
        <Link to="/" className="boton boton--contorno recuperar__accion">
          <House size={16} strokeWidth={2.25} aria-hidden="true" />
          Ir al inicio
        </Link>
        <Link to="/login" className="boton boton--contorno recuperar__accion">
          <LogIn size={16} strokeWidth={2.25} aria-hidden="true" />
          Ir al login
        </Link>
        <SolicitarAyuda compacto />
      </nav>
    </PantallaAcceso>
  )
}
