import { ArrowRight, CircleAlert, Eye, EyeOff, Info, Lock, Mail } from 'lucide-react'
import { GoogleLogin } from '@react-oauth/google'
import { useRef, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import robotError from '../../../assets/ilustraciones/robot-error.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { RUTA_CAMBIO_CONTRASENA, rutaInicioDelRol } from '../../../app/rutasPorRol.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { validarInicioSesion } from '../../../shared/utils/validators.js'
import { PantallaAcceso } from '../components/PantallaAcceso.jsx'
import { MOTIVO_CIERRE, useSesion } from '../sesionContext.js'
import { CONTRASENA_DESARROLLO, USUARIOS_DESARROLLO } from '../usuariosDesarrollo.js'
import './Login.css'

const CAMPOS = ['correo', 'contrasena']

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''


export default function Login() {
  useTituloDocumento('Iniciar sesión')
  const { usuario, validando, motivoCierre, entrar, entrarConGoogle } = useSesion()
  const ubicacion = useLocation()

  const [formulario, setFormulario] = useState({ correo: '', contrasena: '' })
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [verContrasena, setVerContrasena] = useState(false)
  const [mayusculas, setMayusculas] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)
  const refCorreo = useRef(null)
  const refContrasena = useRef(null)

  // Con un token guardado se espera a validarlo: así el formulario no aparece y desaparece.
  if (validando) return null
  // Al abrirse la sesión, vuelve a la ruta protegida de origen o al inicio del rol. Con contraseña
  // temporal, primero hay que cambiarla.
  if (usuario) {
    const destino = usuario.debeCambiarContrasena
      ? RUTA_CAMBIO_CONTRASENA
      : (ubicacion.state?.desde ?? rutaInicioDelRol(usuario.rol))
    return <Navigate to={destino} replace />
  }

  const erroresLocales = validarInicioSesion(formulario)
  const erroresServidor = errorEnvio?.errores ?? {}
  const errorDe = (campo) => erroresServidor[campo] ?? (intentoEnviar ? erroresLocales[campo] : null)
  const errorGeneral = errorEnvio && !Object.keys(erroresServidor).length ? errorEnvio : null

  function cambiar(campo, valor) {
    setFormulario((f) => ({ ...f, [campo]: valor }))
    setErrorEnvio(null)
  }

  function enfocar(campo) {
    const ref = campo === 'correo' ? refCorreo : refContrasena
    ref.current?.focus()
  }

  async function manejarGoogle(credentialResponse) {
    const idToken = credentialResponse?.credential
    if (!idToken) {
      setErrorEnvio({ message: 'No se pudo completar el inicio de sesión con Google.' })
      return
    }
    setEnviando(true)
    setErrorEnvio(null)
    try {
      await entrarConGoogle(idToken)
    } catch (fallo) {
      setErrorEnvio(fallo)
      setEnviando(false)
    }
  }

  function manejarErrorGoogle() {
    setErrorEnvio({ message: 'No se pudo iniciar sesión con Google. Inténtalo de nuevo.' })
    setEnviando(false)
  }

  async function manejarEnvio(evento) {
    evento.preventDefault()
    setIntentoEnviar(true)
    const primeroInvalido = CAMPOS.find((campo) => erroresLocales[campo])
    if (primeroInvalido) {
      enfocar(primeroInvalido)
      return
    }

    setEnviando(true)
    setErrorEnvio(null)
    try {
      await entrar(formulario.correo.trim(), formulario.contrasena)
    } catch (fallo) {
      setErrorEnvio(fallo)
      setEnviando(false)
      const campoServidor = CAMPOS.find((campo) => fallo.errores?.[campo])
      if (campoServidor) {
        enfocar(campoServidor)
      } else if (fallo.status === 401) {
        // Credenciales incorrectas: se borra la contraseña para reescribirla sin seleccionar el texto.
        // No se marca el campo vacío como error: el aviso de arriba ya explica qué pasó.
        setFormulario((f) => ({ ...f, contrasena: '' }))
        setIntentoEnviar(false)
        enfocar('contrasena')
      }
    }
  }

  function usarCuentaPrueba(correo) {
    setFormulario({ correo, contrasena: CONTRASENA_DESARROLLO })
    setErrorEnvio(null)
    setIntentoEnviar(false)
  }

  // El avatar reacciona: saluda, piensa mientras se valida y se preocupa si algo falla.
  const hayError = Boolean(errorEnvio) || (intentoEnviar && Object.keys(erroresLocales).length > 0)
  const avatar = enviando ? robotPensando : hayError ? robotError : robotSaludo

  const describir = (campo, ...extra) => {
    const ids = [...extra, errorDe(campo) && `${campo}-error`].filter(Boolean)
    return ids.length ? ids.join(' ') : undefined
  }

  return (
    <PantallaAcceso avatar={avatar} estadoAvatar={enviando ? 'Validando tus datos' : null}>
      <form className="login__formulario" onSubmit={manejarEnvio} noValidate>
        <h1 className="login__formulario-titulo">Iniciar sesión</h1>

        {motivoCierre === MOTIVO_CIERRE.EXPIRADA && !errorGeneral && (
          <div className="login__aviso login__aviso--info" role="status">
            <Info size={18} aria-hidden="true" />
            <span>Tu sesión expiró por seguridad. Vuelve a ingresar para continuar donde ibas.</span>
          </div>
        )}

        {errorGeneral && (
          <div className="login__aviso login__aviso--error" role="alert">
            <CircleAlert size={18} aria-hidden="true" />
            <span>{errorGeneral.message}</span>
          </div>
        )}

        <div className="login__campo">
          <label htmlFor="correo">Correo institucional</label>
          <div className="login__control">
            <Mail size={18} aria-hidden="true" className="login__icono-campo" />
            <input
              ref={refCorreo}
              id="correo"
              type="email"
              inputMode="email"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              value={formulario.correo}
              onChange={(e) => cambiar('correo', e.target.value)}
              aria-invalid={Boolean(errorDe('correo'))}
              aria-describedby={describir('correo')}
              placeholder="nombre@uniautonoma.edu.co"
              required
            />
          </div>
          {errorDe('correo') && (
            <span id="correo-error" className="login__error-campo">
              {errorDe('correo')}
            </span>
          )}
        </div>

        <div className="login__campo">
          <label htmlFor="contrasena">Contraseña</label>
          <div className="login__control">
            <Lock size={18} aria-hidden="true" className="login__icono-campo" />
            <input
              ref={refContrasena}
              id="contrasena"
              type={verContrasena ? 'text' : 'password'}
              autoComplete="current-password"
              value={formulario.contrasena}
              onChange={(e) => cambiar('contrasena', e.target.value)}
              onKeyUp={(e) => setMayusculas(e.getModifierState('CapsLock'))}
              onBlur={() => setMayusculas(false)}
              aria-invalid={Boolean(errorDe('contrasena'))}
              aria-describedby={describir('contrasena', mayusculas && 'contrasena-mayusculas')}
              required
            />
            <button
              type="button"
              className="login__ver"
              onClick={() => setVerContrasena((v) => !v)}
              aria-pressed={verContrasena}
              aria-label="Mostrar contraseña"
              aria-controls="contrasena"
            >
              {verContrasena ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
            </button>
          </div>
          {mayusculas && (
            <span id="contrasena-mayusculas" className="login__ayuda-campo">
              Bloq Mayús está activado.
            </span>
          )}
          {errorDe('contrasena') && (
            <span id="contrasena-error" className="login__error-campo">
              {errorDe('contrasena')}
            </span>
          )}
        </div>

        <Boton type="submit" bloque className="login__ingresar" disabled={enviando} aria-busy={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
          {!enviando && <ArrowRight size={18} strokeWidth={2.25} aria-hidden="true" className="login__flecha" />}
        </Boton>

        {googleClientId && (
          <>
            <p className="login__separador" role="presentation">
              <span>o</span>
            </p>
            <div className="login__google" aria-busy={enviando}>
              <GoogleLogin
                onSuccess={manejarGoogle}
                onError={manejarErrorGoogle}
                text="continue_with"
                locale="es"
                width="100%"
              />
            </div>
            <p className="login__google-ayuda">
              Solo cuentas <strong>@uniautonoma.edu.co</strong> registradas en DOCENTIA.
            </p>
          </>
        )}

        <p className="login__olvido">
          <strong>¿Olvidaste tu contraseña?</strong> Pide una nueva a la Vicerrectoría académica.
        </p>

        {import.meta.env.DEV && <UsuariosPrueba alElegir={usarCuentaPrueba} />}
      </form>
    </PantallaAcceso>
  )
}

/** Atajo para las demos: rellena correo y contraseña con un usuario del seed de desarrollo. */
function UsuariosPrueba({ alElegir }) {
  return (
    <details className="login__prueba">
      <summary>Usuarios de prueba (solo en desarrollo)</summary>
      <ul>
        {USUARIOS_DESARROLLO.map((u) => (
          <li key={u.correo}>
            <button type="button" onClick={() => alElegir(u.correo)}>
              <strong>{u.rol}</strong>
              <span>{u.nombre}</span>
            </button>
          </li>
        ))}
      </ul>
    </details>
  )
}
