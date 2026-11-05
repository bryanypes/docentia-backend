import { GraduationCap, Menu } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Campana } from '../../../../features/notificaciones/components/Campana.jsx'
import {
  obtenerPeriodoActivo,
  obtenerVentanasActivas,
} from '../../../../features/periodos/services/periodosService.js'
import { useConsultaMedia } from '../../../hooks/useConsultaMedia.js'
import { useFetch } from '../../../hooks/useFetch.js'
import { LimiteError } from '../../ui/LimiteError/LimiteError.jsx'
import { Sidebar } from '../Sidebar/Sidebar.jsx'
import { PeriodoContext } from './periodoContext.js'
import './AppLayout.css'

const CONSULTA_MOVIL = '(max-width: 900px)'
const CLAVE_MENU = 'docentia.menu-abierto'

function leerPreferencia() {
  try {
    return localStorage.getItem(CLAVE_MENU) !== 'no'
  } catch {
    return true
  }
}

function guardarPreferencia(abierto) {
  try {
    localStorage.setItem(CLAVE_MENU, abierto ? 'si' : 'no')
  } catch {
    // Sin almacenamiento: la preferencia dura lo que la pestaña.
  }
}

// Si las ventanas no se pueden consultar, las pantallas no bloquean nada: el backend decide igual.
const cargarVentanas = () => obtenerVentanasActivas().catch(() => null)

/** Estructura de las páginas autenticadas: menú, encabezado (en cada página) y contenido. */
export function AppLayout() {
  const periodo = useFetch(obtenerPeriodoActivo)
  const ventanas = useFetch(cargarVentanas)
  const { recargar: recargarPeriodo } = periodo
  const { recargar: recargarVentanas } = ventanas
  const valor = useMemo(
    () => ({
      periodo: periodo.datos,
      ventanas: ventanas.datos,
      cargando: periodo.cargando,
      recargar: () => {
        recargarPeriodo()
        recargarVentanas()
      },
    }),
    [periodo.datos, ventanas.datos, periodo.cargando, recargarPeriodo, recargarVentanas],
  )

  // Al navegar, el foco pasa al contenido nuevo para que teclado y lectores de pantalla no se queden
  // en el enlace del menú. En la primera carga no se mueve.
  const { pathname, state } = useLocation()
  const principal = useRef(null)
  const primeraRuta = useRef(pathname)
  useEffect(() => {
    if (pathname === primeraRuta.current) return
    primeraRuta.current = null
    // Abrir o cerrar un modal cambia la URL pero no la página: el foco lo maneja el diálogo.
    if (state?.conservarFoco) return
    principal.current?.focus({ preventScroll: true })
    window.scrollTo(0, 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo reacciona al cambio de ruta
  }, [pathname])

  // Menú: en escritorio se abre o se cierra del todo (y se recuerda); en el celular es un panel que
  // se abre sobre la página y se cierra al elegir a dónde ir.
  const esMovil = useConsultaMedia(CONSULTA_MOVIL)
  const [abiertoEscritorio, setAbiertoEscritorio] = useState(leerPreferencia)
  const [abiertoMovil, setAbiertoMovil] = useState(false)
  const botonMovil = useRef(null)
  const botonEscritorio = useRef(null)
  const cerroAlNavegar = useRef(false)
  const panelAbierto = esMovil && abiertoMovil

  const cerrarPanel = useCallback(() => setAbiertoMovil(false), [])
  const alNavegar = useCallback(() => {
    // El foco lo toma el contenido nuevo, no el botón del menú.
    cerroAlNavegar.current = true
    setAbiertoMovil(false)
  }, [])

  function alternarEscritorio() {
    const nuevo = !abiertoEscritorio
    guardarPreferencia(nuevo)
    setAbiertoEscritorio(nuevo)
    // Al cerrar, el foco va al botón que lo vuelve a abrir; al abrir, al botón de cerrar del menú.
    requestAnimationFrame(() =>
      (nuevo ? document.getElementById('cerrar-menu') : botonEscritorio.current)?.focus(),
    )
  }

  // Con el panel abierto en el celular: foco dentro, Esc lo cierra y la página de atrás no se mueve.
  const estuvoAbierto = useRef(false)
  useEffect(() => {
    if (!panelAbierto) {
      if (estuvoAbierto.current && !cerroAlNavegar.current) botonMovil.current?.focus()
      estuvoAbierto.current = false
      cerroAlNavegar.current = false
      return undefined
    }
    estuvoAbierto.current = true
    document.getElementById('cerrar-menu')?.focus()
    const alPulsar = (evento) => evento.key === 'Escape' && cerrarPanel()
    const desbordeAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', alPulsar)
    return () => {
      document.body.style.overflow = desbordeAnterior
      document.removeEventListener('keydown', alPulsar)
    }
  }, [panelAbierto, cerrarPanel])

  const clases = ['app-layout', !abiertoEscritorio && 'app-layout--menu-cerrado', panelAbierto && 'app-layout--menu-movil']
    .filter(Boolean)
    .join(' ')

  return (
    <PeriodoContext.Provider value={valor}>
      <a href="#contenido" className="saltar-contenido">
        Saltar al contenido
      </a>
      <div className={clases}>
        {/* Celular: barra fija con las tres líneas, la marca y (por CSS) la campana. */}
        <header className="barra-movil">
          <BotonMenu ref={botonMovil} abierto={panelAbierto} onClick={() => setAbiertoMovil((a) => !a)} />
          <span className="barra-movil__marca">
            <span className="barra-movil__logo" aria-hidden="true">
              <GraduationCap size={18} strokeWidth={2.25} />
            </span>
            DOCENTIA
          </span>
        </header>

        <Sidebar
          id="menu-principal"
          movil={esMovil}
          onCerrar={esMovil ? cerrarPanel : alternarEscritorio}
          onNavegar={esMovil ? alNavegar : undefined}
        />
        {panelAbierto && <div className="app-layout__velo" aria-hidden="true" onClick={cerrarPanel} />}

        <main id="contenido" ref={principal} tabIndex={-1} className="app-layout__principal">
          {!abiertoEscritorio && (
            <BotonMenu
              ref={botonEscritorio}
              abierto={false}
              onClick={alternarEscritorio}
              className="app-layout__abrir-menu"
            />
          )}
          <Campana />
          {/* Si una pantalla falla, el menú sigue y el error queda en el área de contenido. */}
          <LimiteError key={pathname} enLinea>
            <Outlet />
          </LimiteError>
        </main>
      </div>
    </PeriodoContext.Provider>
  )
}

/** Las tres líneas que abren o cierran el menú principal. */
function BotonMenu({ ref, abierto, onClick, className = '' }) {
  return (
    <button
      ref={ref}
      type="button"
      className={`boton-menu ${className}`}
      aria-controls="menu-principal"
      aria-expanded={abierto}
      aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'}
      title={abierto ? 'Cerrar menú' : 'Abrir menú'}
      onClick={onClick}
    >
      <Menu size={22} strokeWidth={2.25} aria-hidden="true" />
    </button>
  )
}
