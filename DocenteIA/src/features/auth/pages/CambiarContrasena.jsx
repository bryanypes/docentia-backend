import { Check, Circle, Eye, EyeOff } from 'lucide-react'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotError from '../../../assets/ilustraciones/robot-error.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { rutaInicioDelRol } from '../../../app/rutasPorRol.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ResumenErrores } from '../../../shared/components/ui/ResumenErrores/ResumenErrores.jsx'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { validarCambioContrasena } from '../../../shared/utils/validators.js'
import { PantallaAcceso } from '../components/PantallaAcceso.jsx'
import { useSesion } from '../sesionContext.js'
import { cambiarContrasena } from '../services/authService.js'
import './CambiarContrasena.css'

const CAMPOS = ['contrasenaActual', 'contrasenaNueva', 'confirmacion']

/**
 * Cambio de la contraseña propia. Es obligatorio mientras la contraseña sea temporal (la asignó la
 * vicerrectoría): en ese caso no hay forma de salir de aquí salvo cerrando sesión.
 */
export default function CambiarContrasena() {
  const { usuario, reemplazarSesion, cerrarSesion } = useSesion()
  const navegar = useNavigate()
  const obligatorio = usuario.debeCambiarContrasena
  useTituloDocumento(obligatorio ? 'Crea tu contraseña' : 'Cambiar contraseña')

  const [formulario, setFormulario] = useState({ contrasenaActual: '', contrasenaNueva: '', confirmacion: '' })
  const [ver, setVer] = useState(false)
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)
  const resumen = useRef(null)

  const erroresLocales = validarCambioContrasena(formulario)
  const erroresServidor = errorEnvio?.errores ?? {}
  const errorDe = (campo) => erroresServidor[campo] ?? (intentoEnviar ? erroresLocales[campo] : null)
  const listaErrores = CAMPOS.filter(errorDe).map((campo) => ({ id: campo, mensaje: errorDe(campo) }))
  const errorGeneral = errorEnvio && Object.keys(erroresServidor).length === 0 ? errorEnvio.message : null

  const { contrasenaActual, contrasenaNueva, confirmacion } = formulario
  const requisitos = [
    { texto: 'Entre 8 y 72 caracteres', cumple: contrasenaNueva.length >= 8 && contrasenaNueva.length <= 72 },
    { texto: 'Distinta de la actual', cumple: Boolean(contrasenaNueva) && contrasenaNueva !== contrasenaActual },
    { texto: 'Las dos coinciden', cumple: Boolean(confirmacion) && confirmacion === contrasenaNueva },
  ]

  function cambiar(campo, valor) {
    setFormulario((f) => ({ ...f, [campo]: valor }))
    setErrorEnvio(null)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setIntentoEnviar(true)
    if (Object.keys(erroresLocales).length > 0) {
      // Tras pintar el resumen, el foco va a él para que se lea qué corregir.
      requestAnimationFrame(() => resumen.current?.focus())
      return
    }
    setEnviando(true)
    setErrorEnvio(null)
    try {
      const sesion = await cambiarContrasena(contrasenaActual, contrasenaNueva)
      reemplazarSesion(sesion)
      navegar(rutaInicioDelRol(sesion.usuario.rol), {
        replace: true,
        state: { mensaje: 'Tu contraseña quedó actualizada.' },
      })
    } catch (error) {
      setErrorEnvio(error)
      setEnviando(false)
      if (error.errores?.contrasenaActual) {
        setFormulario((f) => ({ ...f, contrasenaActual: '' }))
        document.getElementById('contrasenaActual')?.focus()
      }
    }
  }

  const describir = (campo, ...extra) => [...extra, errorDe(campo) && `${campo}-error`].filter(Boolean).join(' ') || undefined
  const tipo = ver ? 'text' : 'password'
  // El avatar señala el formulario, celebra cuando todo está listo y se preocupa si algo falla.
  const listo = requisitos.every((r) => r.cumple) && Boolean(contrasenaActual)
  const avatar = enviando
    ? robotPensando
    : errorEnvio || listaErrores.length > 0
      ? robotError
      : listo
        ? robotAprobada
        : robotSenala

  return (
    <PantallaAcceso avatar={avatar} estadoAvatar={listo && !enviando ? 'Contraseña lista para guardar' : null}>
      <form className="cambio-contrasena__formulario" onSubmit={guardar} noValidate>
        <div className="cambio-contrasena__encabezado">
          <h1>{obligatorio ? 'Crea tu contraseña' : 'Cambiar contraseña'}</h1>
          <p>
            {obligatorio
              ? `Hola, ${usuario.nombreCompleto.split(' ')[0]}. La contraseña con la que ingresaste es temporal: elige una propia para continuar.`
              : 'Escribe tu contraseña actual y la nueva.'}
          </p>
        </div>

        <ResumenErrores ref={resumen} errores={listaErrores} />
        {errorGeneral && (
          <div className="cambio-contrasena__error" role="alert">
            {errorGeneral}
          </div>
        )}

        <Campo id="contrasenaActual" etiqueta={obligatorio ? 'Contraseña temporal' : 'Contraseña actual'} error={errorDe('contrasenaActual')}>
          <input
            id="contrasenaActual"
            type={tipo}
            autoComplete="current-password"
            value={contrasenaActual}
            onChange={(e) => cambiar('contrasenaActual', e.target.value)}
            aria-invalid={Boolean(errorDe('contrasenaActual'))}
            aria-describedby={describir('contrasenaActual')}
          />
        </Campo>

        <Campo id="contrasenaNueva" etiqueta="Nueva contraseña" error={errorDe('contrasenaNueva')}>
          <input
            id="contrasenaNueva"
            type={tipo}
            autoComplete="new-password"
            maxLength={72}
            value={contrasenaNueva}
            onChange={(e) => cambiar('contrasenaNueva', e.target.value)}
            aria-invalid={Boolean(errorDe('contrasenaNueva'))}
            aria-describedby={describir('contrasenaNueva', 'requisitos')}
          />
        </Campo>

        <Campo id="confirmacion" etiqueta="Repite la nueva contraseña" error={errorDe('confirmacion')}>
          <input
            id="confirmacion"
            type={tipo}
            autoComplete="new-password"
            maxLength={72}
            value={confirmacion}
            onChange={(e) => cambiar('confirmacion', e.target.value)}
            aria-invalid={Boolean(errorDe('confirmacion'))}
            aria-describedby={describir('confirmacion')}
          />
        </Campo>

        <ul id="requisitos" className="cambio-contrasena__requisitos" aria-label="Requisitos de la nueva contraseña">
          {requisitos.map(({ texto, cumple }) => (
            <li key={texto} className={cumple ? 'cumple' : undefined}>
              {cumple ? <Check size={16} aria-hidden="true" /> : <Circle size={16} aria-hidden="true" />}
              {texto}
              <span className="solo-lectores">{cumple ? ': cumplido' : ': pendiente'}</span>
            </li>
          ))}
        </ul>

        <label className="cambio-contrasena__ver">
          <input type="checkbox" checked={ver} onChange={(e) => setVer(e.target.checked)} />
          {ver ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
          Mostrar contraseñas
        </label>

        <div className="cambio-contrasena__botones">
          <Boton type="submit" bloque disabled={enviando} aria-busy={enviando}>
            {enviando ? 'Guardando…' : 'Guardar contraseña'}
          </Boton>
          {obligatorio ? (
            <Boton variante="secundario" bloque onClick={() => cerrarSesion()} disabled={enviando}>
              Salir sin cambiarla
            </Boton>
          ) : (
            <Boton variante="secundario" bloque onClick={() => navegar(-1)} disabled={enviando}>
              Cancelar
            </Boton>
          )}
        </div>
      </form>
    </PantallaAcceso>
  )
}

function Campo({ id, etiqueta, error, children }) {
  return (
    <div className="cambio-contrasena__campo">
      <label htmlFor={id}>{etiqueta}</label>
      {children}
      {error && (
        <span id={`${id}-error`} className="cambio-contrasena__error-campo">
          {error}
        </span>
      )}
    </div>
  )
}
