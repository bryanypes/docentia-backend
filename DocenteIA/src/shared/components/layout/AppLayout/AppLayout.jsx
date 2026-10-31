import { useEffect, useMemo, useRef } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import {
  obtenerPeriodoActivo,
  obtenerVentanasActivas,
} from '../../../../features/periodos/services/periodosService.js'
import { useFetch } from '../../../hooks/useFetch.js'
import { Sidebar } from '../Sidebar/Sidebar.jsx'
import { PeriodoContext } from './periodoContext.js'
import './AppLayout.css'

// Si las ventanas no se pueden consultar, las pantallas no bloquean nada: el backend decide igual.
const cargarVentanas = () => obtenerVentanasActivas().catch(() => null)

/** Estructura de las páginas autenticadas: barra lateral, encabezado (en cada página) y contenido. */
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

  return (
    <PeriodoContext.Provider value={valor}>
      <a href="#contenido" className="saltar-contenido">
        Saltar al contenido
      </a>
      <div className="app-layout">
        <Sidebar />
        <main id="contenido" ref={principal} tabIndex={-1} className="app-layout__principal">
          <Outlet />
        </main>
      </div>
    </PeriodoContext.Provider>
  )
}
