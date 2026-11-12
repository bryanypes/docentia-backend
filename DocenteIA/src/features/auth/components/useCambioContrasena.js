import { useRef, useState } from 'react'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotError from '../../../assets/ilustraciones/robot-error.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { validarCambioContrasena } from '../../../shared/utils/validators.js'
import { cambiarContrasena } from '../services/authService.js'

const CAMPOS = ['contrasenaActual', 'contrasenaNueva', 'confirmacion']

/**
 * Estado del cambio de contraseña, compartido por la pantalla obligatoria y el modal de Mi perfil.
 * `avatar` es la ilustración de DocenteIA que acompaña cada momento: señala el formulario, piensa
 * mientras guarda, se preocupa si algo falla y celebra cuando todo está listo.
 *
 * @param onGuardada recibe la sesión nueva cuando el backend acepta el cambio
 */
export function useCambioContrasena(onGuardada) {
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
  const listo = requisitos.every((r) => r.cumple) && Boolean(contrasenaActual)
  const avatar = enviando
    ? robotPensando
    : errorEnvio || listaErrores.length > 0
      ? robotError
      : listo
        ? robotAprobada
        : robotSenala

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
      onGuardada(sesion)
    } catch (error) {
      setErrorEnvio(error)
      setEnviando(false)
      if (error.errores?.contrasenaActual) {
        setFormulario((f) => ({ ...f, contrasenaActual: '' }))
        document.getElementById('contrasenaActual')?.focus()
      }
    }
  }

  return {
    formulario,
    ver,
    setVer,
    enviando,
    resumen,
    errorDe,
    listaErrores,
    errorGeneral,
    requisitos,
    listo,
    avatar,
    cambiar,
    guardar,
  }
}
