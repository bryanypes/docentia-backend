import { ChevronLeft, ChevronRight, CircleHelp, Pause, Play, SkipForward, X } from 'lucide-react'
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import robotAprobada from '../../assets/ilustraciones/robot-aprobada.svg'
import robotSaludo from '../../assets/ilustraciones/robot-saludo.svg'
import robotSenala from '../../assets/ilustraciones/robot-senala.svg'
import { rutaInicioDelRol } from '../../app/rutasPorRol.js'
import { Boton } from '../../shared/components/ui/Boton/Boton.jsx'
import { useSesion } from '../auth/sesionContext.js'
import { PASOS_POR_ROL, SEGUNDOS_POR_PASO } from './pasos.js'
import { EVENTO_VER_TUTORIAL, marcarTutorialVisto, obtenerTutorial } from './tutorialService.js'
import './TutorialGuiado.css'

const TICK_MS = 100
// Lo que dura el globo que señala el "?" al terminar o saltar el tutorial.
const SEGUNDOS_AVISO = 9
const MARGEN_FOCO = 6
// Igual que CONSULTA_MOVIL del AppLayout: por debajo el menú es un panel cerrado.
const ANCHO_MENU_OCULTO = 900

const prefiereMenosMovimiento = () =>
  typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * Rectángulo del enlace del menú que lleva a la página del paso, para resaltarlo. En el celular el
 * menú está oculto: sin enlace visible no se resalta nada y se oscurece toda la pantalla.
 */
function medirEnlace(ruta) {
  if (window.innerWidth <= ANCHO_MENU_OCULTO) return null
  const enlace = document.querySelector(`#menu-principal a[href$="${ruta}"]`)
  if (!enlace) return null
  const r = enlace.getBoundingClientRect()
  const visible = r.width && r.height && r.right > 0 && r.left < window.innerWidth && r.bottom > 0 && r.top < window.innerHeight
  if (!visible) return null
  return { top: r.top - MARGEN_FOCO, left: r.left - MARGEN_FOCO, width: r.width + MARGEN_FOCO * 2, height: r.height + MARGEN_FOCO * 2 }
}

/**
 * Tutorial del primer inicio de sesión. Bloquea la pantalla con el robot de DOCENTIA y recorre las
 * páginas del rol, una por paso, resaltando su enlace en el menú. Avanza solo como un video (se
 * puede pausar) y se puede saltar un paso o todo el tutorial. Al terminar o saltarlo no vuelve a
 * aparecer; desde Mi perfil se puede ver otra vez.
 */
export function TutorialGuiado() {
  const { usuario } = useSesion()
  const navegar = useNavigate()
  const pasos = PASOS_POR_ROL[usuario?.rol] ?? []
  const [indice, setIndice] = useState(null)
  const [progreso, setProgreso] = useState(0)
  const [pausado, setPausado] = useState(prefiereMenosMovimiento)
  const [foco, setFoco] = useState(null)
  const [aviso, setAviso] = useState(false)
  const titulo = useRef(null)
  const avance = useRef(0)
  const activo = indice !== null

  function irA(nuevo) {
    avance.current = 0
    setProgreso(0)
    setIndice(nuevo)
  }
  const paso = activo ? pasos[indice] : null
  const ultimo = activo && indice === pasos.length - 1

  // Primer inicio: el backend dice si ya lo vio. Con contraseña temporal espera a que la cambie.
  useEffect(() => {
    if (!usuario || usuario.debeCambiarContrasena || pasos.length === 0) return undefined
    let vigente = true
    obtenerTutorial()
      .then((estado) => {
        if (vigente && !estado?.visto) irA(0)
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [usuario, pasos.length])

  // Mi perfil puede pedir verlo otra vez.
  useEffect(() => {
    const abrir = () => pasos.length && irA(0)
    window.addEventListener(EVENTO_VER_TUTORIAL, abrir)
    return () => window.removeEventListener(EVENTO_VER_TUTORIAL, abrir)
  }, [pasos.length])

  // Cada paso lleva a su página y pone el foco en el título para los lectores de pantalla.
  useEffect(() => {
    if (!paso) return
    navegar(paso.ruta, { state: { conservarFoco: true } })
    titulo.current?.focus()
  }, [paso, navegar])

  // El resaltado sigue al enlace aunque la página cambie de tamaño.
  useLayoutEffect(() => {
    if (!paso) return undefined
    const medir = () => setFoco(medirEnlace(paso.ruta))
    medir()
    const tardio = setTimeout(medir, 350)
    window.addEventListener('resize', medir)
    return () => {
      clearTimeout(tardio)
      window.removeEventListener('resize', medir)
    }
  }, [paso])

  const terminar = useCallback(() => {
    setIndice(null)
    setAviso(true)
    marcarTutorialVisto().catch(() => {})
    navegar(rutaInicioDelRol(usuario?.rol))
  }, [navegar, usuario?.rol])

  const siguiente = useCallback(() => {
    if (ultimo) terminar()
    else irA(indice + 1)
  }, [ultimo, terminar, indice])

  const anterior = useCallback(() => irA(Math.max(0, indice - 1)), [indice])

  // Avance automático, como un video: la barra se llena y pasa al siguiente paso. En el último se
  // queda llena esperando que el usuario termine.
  useEffect(() => {
    if (!activo || pausado) return undefined
    const incremento = 100 / ((SEGUNDOS_POR_PASO * 1000) / TICK_MS)
    const reloj = setInterval(() => {
      avance.current = Math.min(100, avance.current + incremento)
      if (avance.current >= 100 && !ultimo) siguiente()
      else setProgreso(avance.current)
    }, TICK_MS)
    return () => clearInterval(reloj)
  }, [activo, pausado, ultimo, siguiente])

  // Teclado: flechas para moverse, espacio pausa y Esc salta todo el tutorial.
  useEffect(() => {
    if (!activo) return undefined
    const alPulsar = (e) => {
      if (e.key === 'Escape') terminar()
      else if (e.key === 'ArrowRight') siguiente()
      else if (e.key === 'ArrowLeft') anterior()
    }
    document.addEventListener('keydown', alPulsar)
    const desborde = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', alPulsar)
      document.body.style.overflow = desborde
    }
  }, [activo, siguiente, anterior, terminar])

  // El globo que señala el "?" se va solo.
  useEffect(() => {
    if (!aviso) return undefined
    const reloj = setTimeout(() => setAviso(false), SEGUNDOS_AVISO * 1000)
    return () => clearTimeout(reloj)
  }, [aviso])

  if (pasos.length === 0) return null

  if (!paso) {
    // Terminado o saltado: el tutorial queda guardado en el signo de interrogación.
    return (
      <div className="tutorial-ayuda">
        <button
          type="button"
          className="tutorial-ayuda__boton"
          onClick={() => {
            setAviso(false)
            irA(0)
          }}
          aria-label="Ver el tutorial"
          title="Ver el tutorial"
        >
          <CircleHelp size={20} aria-hidden="true" />
        </button>
        {aviso && (
          <div className="tutorial-ayuda__globo" role="status">
            <img src={robotAprobada} alt="" />
            <p>
              <strong>¡Listo!</strong> El tutorial queda guardado aquí, en el signo de interrogación. Tócalo cuando
              quieras verlo otra vez.
            </p>
            <button
              type="button"
              className="tutorial-ayuda__cerrar"
              onClick={() => setAviso(false)}
              aria-label="Cerrar aviso"
            >
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        )}
      </div>
    )
  }

  const robot = indice === 0 ? robotSaludo : ultimo ? robotAprobada : robotSenala

  return createPortal(
    <div className="tutorial" role="dialog" aria-modal="true" aria-labelledby="tutorial-titulo" aria-describedby="tutorial-texto">
      {foco ? (
        <div className="tutorial__foco" style={foco} aria-hidden="true" />
      ) : (
        <div className="tutorial__velo" aria-hidden="true" />
      )}

      <section className="tutorial__tarjeta" key={indice}>
        <img className="tutorial__robot" src={robot} alt="" />
        <div className="tutorial__cuerpo">
          <p className="tutorial__paso">
            Paso {indice + 1} de {pasos.length}
          </p>
          <h2 id="tutorial-titulo" className="tutorial__titulo" tabIndex={-1} ref={titulo}>
            {paso.titulo}
          </h2>
          <p id="tutorial-texto" className="tutorial__texto">
            {paso.texto}
            {ultimo && ' Puedes ver este tutorial otra vez desde Mi perfil.'}
          </p>

          <div className="tutorial__reproductor">
            <button
              type="button"
              className="tutorial__icono"
              onClick={() => setPausado((p) => !p)}
              aria-label={pausado ? 'Reproducir el tutorial' : 'Pausar el tutorial'}
              title={pausado ? 'Reproducir' : 'Pausar'}
            >
              {pausado ? <Play size={16} aria-hidden="true" /> : <Pause size={16} aria-hidden="true" />}
            </button>
            <ol className="tutorial__pistas" aria-label="Pasos del tutorial">
              {pasos.map((p, i) => (
                <li key={p.ruta + i} className="tutorial__pista">
                  <button
                    type="button"
                    onClick={() => irA(i)}
                    aria-label={`Ir al paso ${i + 1}: ${p.titulo}`}
                    aria-current={i === indice ? 'step' : undefined}
                  >
                    <span style={{ width: `${i < indice ? 100 : i === indice ? progreso : 0}%` }} />
                  </button>
                </li>
              ))}
            </ol>
          </div>

          <div className="tutorial__acciones">
            <button type="button" className="tutorial__enlace" onClick={terminar}>
              <X size={15} aria-hidden="true" /> Saltar todo el tutorial
            </button>
            <div className="tutorial__navegacion">
              {indice > 0 && (
                <Boton variante="contorno" icono={ChevronLeft} onClick={anterior}>
                  Anterior
                </Boton>
              )}
              {!ultimo && (
                <Boton variante="contorno" icono={SkipForward} onClick={siguiente}>
                  Saltar este paso
                </Boton>
              )}
              <Boton icono={ultimo ? undefined : ChevronRight} onClick={siguiente}>
                {ultimo ? 'Terminar' : 'Siguiente'}
              </Boton>
            </div>
          </div>
        </div>
      </section>
    </div>,
    document.body,
  )
}
