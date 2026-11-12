import {
  BookOpen,
  CalendarDays,
  CalendarRange,
  ChartColumn,
  ChartColumnBig,
  ClipboardCheck,
  ClipboardList,
  FileSignature,
  GraduationCap,
  LayoutDashboard,
  Lightbulb,
  LogOut,
  Menu,
  MessagesSquare,
  PenLine,
  Scale,
  Settings,
  UserCog,
  Users,
  X,
} from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import { useSesion } from '../../../../features/auth/sesionContext.js'
import '../../../../features/formularios/components/FormularioInstrumento.css'
import { obtenerPendientesCoevaluacion } from '../../../../features/formularios/services/formulariosService.js'
import { useFetch } from '../../../hooks/useFetch.js'
import { ETIQUETA_ROL, ROLES } from '../../../constants/dominio.js'
import { iniciales } from '../../../utils/formatters.js'
import './Sidebar.css'

const NAVEGACION = {
  [ROLES.ESTUDIANTE]: [{ ruta: '/inicio', etiqueta: 'Mis evaluaciones', Icono: ClipboardCheck }],
  [ROLES.DOCENTE]: [
    { ruta: '/inicio', etiqueta: 'Mi evaluación', Icono: ClipboardCheck },
    { ruta: '/mi-autoevaluacion', etiqueta: 'Mi autoevaluación', Icono: PenLine },
    { ruta: '/mi-cita', etiqueta: 'Mi cita', Icono: CalendarDays },
    { ruta: '/mi-acta', etiqueta: 'Mi acta', Icono: FileSignature },
  ],
  [ROLES.COORDINADOR]: [
    { ruta: '/inicio', etiqueta: 'Inicio', Icono: LayoutDashboard },
    { ruta: '/estadisticas', etiqueta: 'Estadísticas', Icono: ChartColumnBig },
    { ruta: '/docentes', etiqueta: 'Docentes', Icono: Users },
    { ruta: '/coevaluaciones', etiqueta: 'Coevaluaciones', Icono: ClipboardList },
    { ruta: '/analisis', etiqueta: 'Análisis de comentarios', Icono: MessagesSquare },
    { ruta: '/agenda', etiqueta: 'Agenda de citas', Icono: CalendarDays },
    { ruta: '/acciones-sugeridas', etiqueta: 'Acciones sugeridas', Icono: Lightbulb },
    { ruta: '/panel-programas', etiqueta: 'Panel por programa', Icono: ChartColumn },
  ],
  [ROLES.VICERRECTOR]: [
    { ruta: '/inicio', etiqueta: 'Inicio', Icono: LayoutDashboard },
    { ruta: '/estadisticas', etiqueta: 'Estadísticas', Icono: ChartColumnBig },
    { ruta: '/docentes', etiqueta: 'Docentes', Icono: Users },
    { ruta: '/analisis', etiqueta: 'Análisis de comentarios', Icono: MessagesSquare },
    { ruta: '/acciones-sugeridas', etiqueta: 'Acciones sugeridas', Icono: Lightbulb },
    { ruta: '/panel-programas', etiqueta: 'Panel por programa', Icono: ChartColumn },
    { ruta: '/periodos', etiqueta: 'Periodos', Icono: CalendarRange },
    { ruta: '/ponderaciones', etiqueta: 'Ponderaciones', Icono: Scale },
    { ruta: '/programas', etiqueta: 'Programas', Icono: BookOpen },
    { ruta: '/usuarios', etiqueta: 'Usuarios', Icono: UserCog },
  ],
}

function ContadorCoevaluaciones() {
  const { datos } = useFetch(obtenerPendientesCoevaluacion)
  if (!datos?.pendientes) return null
  return (
    <span className="menu__contador">
      {datos.pendientes}
      <span className="solo-lectores"> pendientes</span>
    </span>
  )
}

/**
 * Menú principal. En escritorio es la barra lateral y `onCerrar` la oculta del todo; en el celular
 * (`movil`) es un panel sobre la página que se cierra al elegir un destino (`onNavegar`).
 */
export function Sidebar({ id, movil = false, onCerrar, onNavegar }) {
  const { usuario, cerrarSesion } = useSesion()
  const enlaces = NAVEGACION[usuario.rol] ?? []

  return (
    <aside id={id} className="sidebar" aria-label="Menú principal">
      <div className="sidebar__marca">
        <span className="sidebar__logo" aria-hidden="true">
          <GraduationCap size={20} strokeWidth={2.25} />
        </span>
        <span className="sidebar__nombre">
          <strong>DOCENTIA</strong>
          <span>Evaluación docente</span>
        </span>
        <button
          id="cerrar-menu"
          type="button"
          className="sidebar__cerrar"
          onClick={onCerrar}
          aria-controls={id}
          aria-expanded="true"
          aria-label="Cerrar menú"
          title="Cerrar menú"
        >
          {movil ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
        </button>
      </div>

      <nav className="sidebar__nav" aria-label="Navegación principal">
        <span className="sidebar__seccion">{ETIQUETA_ROL[usuario.rol]}</span>
        {enlaces.map(({ ruta, etiqueta, Icono }) => (
          <NavLink key={ruta} to={ruta} className="sidebar__enlace" onClick={onNavegar}>
            <Icono size={18} strokeWidth={1.75} aria-hidden="true" className="sidebar__icono" />
            {etiqueta}
            {ruta === '/coevaluaciones' && <ContadorCoevaluaciones />}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar__usuario">
        <span className="sidebar__avatar" aria-hidden="true">
          {iniciales(usuario.nombreCompleto)}
        </span>
        <span className="sidebar__datos">
          <span className="sidebar__usuario-nombre">{usuario.nombreCompleto}</span>
          <span className="sidebar__usuario-meta">{usuario.correo}</span>
        </span>
        <Link to="/perfil" className="sidebar__icono-boton" title="Mi perfil y configuración" onClick={onNavegar}>
          <Settings size={17} aria-hidden="true" />
          <span className="solo-lectores">Mi perfil y configuración</span>
        </Link>
        <button type="button" className="sidebar__salir" onClick={() => cerrarSesion()}>
          <LogOut size={16} strokeWidth={2} aria-hidden="true" />
          Salir
        </button>
      </div>
    </aside>
  )
}
