import { ArrowRight, CircleAlert, Eye, EyeOff, Info, Lock, Mail, ShieldAlert } from 'lucide-react'
import { GoogleLogin } from '@react-oauth/google'
import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import robotError from '../../../assets/ilustraciones/robot-error.svg'
import robotEsperando from '../../../assets/ilustraciones/robot-esperando.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { RUTA_CAMBIO_CONTRASENA, rutaInicioDelRol } from '../../../app/rutasPorRol.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { SolicitarAyuda } from '../../../shared/components/ui/SolicitarAyuda/SolicitarAyuda.jsx'
import { DOMINIO_INSTITUCIONAL, esCorreoExterno } from '../../../shared/constants/ayuda.js'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { validarInicioSesion } from '../../../shared/utils/validators.js'
import { PantallaAcceso } from '../components/PantallaAcceso.jsx'
import { MOTIVO_CIERRE, useSesion } from '../sesionContext.js'
import './Login.css'

const CAMPOS = ['correo', 'contrasena']

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''

// Radio del anillo del cronómetro (viewBox de 120): la circunferencia marca el tiempo restante.
const RADIO_ANILLO = 52
const CIRCUNFERENCIA = 2 * Math.PI * RADIO_ANILLO

// El fin del bloqueo se guarda en el navegador: recargar la página no debe esconder el contador,
// porque el servidor sigue rechazando ese correo hasta que termine la espera.
const CLAVE_BLOQUEO = 'docentia.bloqueo-ingreso'

function leerBloqueoGuardado() {
  try {
    const fin = Number(localStorage.getItem(CLAVE_BLOQUEO))
    const segundos = Math.ceil((fin - Date.now()) / 1000)
    if (segundos > 0) return { segundos }
    localStorage.removeItem(CLAVE_BLOQUEO)
  } catch {
    // Sin almacenamiento (modo privado estricto) el contador vive solo en esta pestaña.
  }
  return null
}

function guardarFinBloqueo(fin) {
  try {
    if (fin) localStorage.setItem(CLAVE_BLOQUEO, String(fin))
    else localStorage.removeItem(CLAVE_BLOQUEO)
  } catch {
    // Igual que al leer: sin almacenamiento no se persiste.
  }
}

/** 9:05 para 545 segundos. */
function formatearEspera(segundos) {
  const minutos = Math.floor(segundos / 60)
  return `${minutos}:${String(segundos % 60).padStart(2, '0')}`
}

/** «Llevas 2 de 5 intentos.» El servidor manda el conteo del correo; sin él no se dice nada. */
function describirIntentos({ intentosUsados, intentosMaximos } = {}) {
  if (!intentosUsados || !intentosMaximos) return null
  if (intentosUsados >= intentosMaximos) {
    return `Llevas ${intentosUsados} de ${intentosMaximos} intentos. El próximo fallo bloqueará el ingreso unos minutos.`
  }
  const restantes = intentosMaximos - intentosUsados
  return `Llevas ${intentosUsados} de ${intentosMaximos} intentos. Te ${restantes === 1 ? 'queda 1' : `quedan ${restantes}`}.`
}

/** Cronómetro centrado con un anillo que se vacía a medida que pasa la espera. */
function Cronometro({ segundos, total }) {
  const fraccion = total > 0 ? segundos / total : 0
  return (
    <div className="login__cronometro">
      <svg viewBox="0 0 120 120" aria-hidden="true">
        <circle className="login__anillo-fondo" cx="60" cy="60" r={RADIO_ANILLO} />
        <circle
          className="login__anillo"
          cx="60"
          cy="60"
          r={RADIO_ANILLO}
          strokeDasharray={CIRCUNFERENCIA}
          strokeDashoffset={CIRCUNFERENCIA * (1 - fraccion)}
        />
      </svg>
      <span className="login__contador" role="timer" aria-live="off" aria-atomic="true">
        {formatearEspera(segundos)}
      </span>
      <span className="login__contador-unidad" aria-hidden="true">
        min : seg
      </span>
    </div>
  )
}

export default function Login() {
  useTituloDocumento('Iniciar sesión')
  const { usuario, validando, motivoCierre, idSesionVencida, entrar, entrarConGoogle } = useSesion()
  const ubicacion = useLocation()

  const [formulario, setFormulario] = useState({ correo: '', contrasena: '' })
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [verContrasena, setVerContrasena] = useState(false)
  const [mayusculas, setMayusculas] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)
  // Correo con forma válida pero de otro dominio: se avisa sin gastar un intento en el servidor.
  const [correoExterno, setCorreoExterno] = useState(null)
  // Bloqueo por intentos: la espera que dio el servidor y los segundos que faltan, para el contador.
  const [bloqueo, setBloqueo] = useState(leerBloqueoGuardado)
  const [segundosBloqueo, setSegundosBloqueo] = useState(() => bloqueo?.segundos ?? 0)
  const refCorreo = useRef(null)
  const refContrasena = useRef(null)

  useEffect(() => {
    if (!bloqueo) return undefined
    const fin = Date.now() + bloqueo.segundos * 1000
    guardarFinBloqueo(fin)
    const actualizar = () => {
      const restantes = Math.max(0, Math.ceil((fin - Date.now()) / 1000))
      setSegundosBloqueo(restantes)
      if (restantes === 0) {
        guardarFinBloqueo(null)
        setBloqueo(null)
        setErrorEnvio(null)
      }
    }
    actualizar()
    const intervalo = setInterval(actualizar, 1000)
    return () => clearInterval(intervalo)
  }, [bloqueo])

  // Con un token guardado se espera a validarlo: así el formulario no aparece y desaparece.
  if (validando) return null
  // Al abrirse la sesión va al inicio del rol. Solo si la sesión anterior venció y entra la misma
  // persona vuelve a la ruta donde estaba; quien entra después de un "Salir" nunca hereda la ruta
  // del rol anterior. Con contraseña temporal, primero hay que cambiarla.
  if (usuario) {
    const volver = ubicacion.state?.desde && idSesionVencida === usuario.idUsuario
    const destino = usuario.debeCambiarContrasena
      ? RUTA_CAMBIO_CONTRASENA
      : volver
        ? ubicacion.state.desde
        : rutaInicioDelRol(usuario.rol)
    return <Navigate to={destino} replace />
  }

  const erroresLocales = validarInicioSesion(formulario)
  const erroresServidor = errorEnvio?.errores ?? {}
  const errorDe = (campo) => erroresServidor[campo] ?? (intentoEnviar ? erroresLocales[campo] : null)
  const errorGeneral = errorEnvio && !Object.keys(erroresServidor).length ? errorEnvio : null
  const bloqueado = Boolean(bloqueo)
  const datosIntentos = errorGeneral?.datos ?? undefined
  const avisoIntentos = errorGeneral && !bloqueado ? describirIntentos(datosIntentos) : null
  const enElLimite = Boolean(datosIntentos?.intentosMaximos) && datosIntentos.intentosUsados >= datosIntentos.intentosMaximos

  function cambiar(campo, valor) {
    setFormulario((f) => ({ ...f, [campo]: valor }))
    if (campo === 'correo') setCorreoExterno(null)
    // El aviso del bloqueo se queda mientras corre el contador.
    if (!bloqueado) setErrorEnvio(null)
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
    if (esCorreoExterno(formulario.correo)) {
      setCorreoExterno(formulario.correo.trim())
      setErrorEnvio(null)
      enfocar('correo')
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
      const espera = fallo.status === 429 ? Number(fallo.datos?.segundosParaReintentar) : 0
      if (espera > 0) {
        setSegundosBloqueo(espera)
        setBloqueo({ segundos: espera })
      } else if (campoServidor) {
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

  // El avatar reacciona: saluda, piensa mientras se valida, espera durante el bloqueo y se preocupa si algo falla.
  const hayError = Boolean(errorEnvio) || Boolean(correoExterno) || (intentoEnviar && Object.keys(erroresLocales).length > 0)
  const avatar = enviando ? robotPensando : bloqueado ? robotEsperando : hayError ? robotError : robotSaludo
  const estadoAvatar = enviando ? 'Validando tus datos' : null

  const describir = (campo, ...extra) => {
    const ids = [...extra, errorDe(campo) && `${campo}-error`].filter(Boolean)
    return ids.length ? ids.join(' ') : undefined
  }

  if (bloqueado) {
    return (
      <PantallaAcceso avatar={avatar} tono="espera" volverAlInicio>
        <section className="login__bloqueo" role="alert" aria-labelledby="titulo-bloqueo">
          <span className="login__bloqueo-icono" aria-hidden="true">
            <ShieldAlert size={20} />
          </span>
          <h1 id="titulo-bloqueo" className="login__bloqueo-titulo">
            Ingreso pausado por seguridad
          </h1>
          <p className="login__bloqueo-texto">
            Superaste el número de intentos permitidos. Podrás volver a intentarlo cuando el contador llegue a cero.
          </p>
          <Cronometro segundos={segundosBloqueo} total={bloqueo.segundos} />
        </section>
        <SolicitarAyuda>Abre una solicitud en la mesa de ayuda y el equipo de soporte te ayudará a recuperar el acceso.</SolicitarAyuda>
      </PantallaAcceso>
    )
  }

  return (
    <PantallaAcceso avatar={avatar} estadoAvatar={estadoAvatar} tono={hayError ? 'error' : null} volverAlInicio>
      <form className="login__formulario" onSubmit={manejarEnvio} noValidate>
        <h1 className="login__formulario-titulo">Iniciar sesión</h1>

        {motivoCierre === MOTIVO_CIERRE.EXPIRADA && !errorGeneral && (
          <div className="login__aviso login__aviso--info" role="status">
            <Info size={18} aria-hidden="true" />
            <span>Tu sesión venció. Vuelve a ingresar para continuar donde ibas.</span>
          </div>
        )}

        {correoExterno && (
          <section className="login__externo" role="alert" aria-labelledby="titulo-externo">
            <img className="login__externo-robot" src={robotError} alt="" />
            <div>
              <h2 id="titulo-externo" className="login__externo-titulo">
                Este correo no puede ingresar a DOCENTIA
              </h2>
              <p className="login__externo-texto">
                <strong className="login__externo-correo">{correoExterno}</strong> no hace parte de la Corporación
                Universitaria Autónoma del Cauca. Ingresa con tu correo institucional, el que termina en{' '}
                <strong>@{DOMINIO_INSTITUCIONAL}</strong>.
              </p>
            </div>
          </section>
        )}

        {errorGeneral && (
          <div className="login__aviso login__aviso--error" role="alert">
            <CircleAlert size={18} aria-hidden="true" />
            <span>
              {errorGeneral.message}
              {avisoIntentos && <span className="login__intentos">{avisoIntentos}</span>}
            </span>
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
              aria-invalid={Boolean(errorDe('correo')) || Boolean(correoExterno)}
              aria-describedby={describir('correo', correoExterno && 'titulo-externo')}
              placeholder={`nombre@${DOMINIO_INSTITUCIONAL}`}
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
              aria-label={verContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}
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
        <Link to="/recuperar-contrasena" className="login__olvide">
          ¿Olvidaste tu contraseña?
        </Link>

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
              Solo cuentas <strong>@{DOMINIO_INSTITUCIONAL}</strong> registradas en DOCENTIA.
            </p>
          </>
        )}

        {enElLimite || correoExterno ? (
          <SolicitarAyuda>
            {correoExterno
              ? 'Si eres parte de la Corporación y aún no tienes correo institucional, abre una solicitud en la mesa de ayuda.'
              : undefined}
          </SolicitarAyuda>
        ) : (
          <p className="login__problemas">
            ¿Problemas para ingresar? <SolicitarAyuda compacto variante="enlace" />
          </p>
        )}
      </form>
    </PantallaAcceso>
  )
}
