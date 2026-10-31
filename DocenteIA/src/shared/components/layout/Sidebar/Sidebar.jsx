import {
  BookOpen,
  Building2,
  CalendarDays,
  CalendarRange,
  ClipboardCheck,
  FileUp,
  GraduationCap,
  KeyRound,
  LayoutDashboard,
  LogOut,
  MessagesSquare,
  Scale,
  UserCog,
  Users,
} from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { RUTA_CAMBIO_CONTRASENA } from '../../../../app/rutasPorRol.js'
import { useSesion } from '../../../../features/auth/sesionContext.js'
import { ETIQUETA_ROL, ROLES } from '../../../constants/dominio.js'
import { iniciales } from '../../../utils/formatters.js'
import './Sidebar.css'

const NAVEGACION = {
  [ROLES.DOCENTE]: [
    { ruta: '/mi-evaluacion', etiqueta: 'Mi evaluación', Icono: ClipboardCheck },
    { ruta: '/mi-cita', etiqueta: 'Mi cita', Icono: CalendarDays },
  ],
  [ROLES.COORDINADOR]: [
    { ruta: '/inicio', etiqueta: 'Inicio', Icono: LayoutDashboard },
    { ruta: '/docentes', etiqueta: 'Docentes', Icono: Users },
    { ruta: '/importaciones', etiqueta: 'Evaluación de estudiantes', Icono: FileUp },
    { ruta: '/analisis', etiqueta: 'Análisis de comentarios', Icono: MessagesSquare },
    { ruta: '/agenda', etiqueta: 'Agenda de citas', Icono: CalendarDays },
  ],
  [ROLES.VICERRECTOR]: [
    { ruta: '/inicio', etiqueta: 'Inicio', Icono: LayoutDashboard },
    { ruta: '/docentes', etiqueta: 'Docentes', Icono: Users },
    { ruta: '/importaciones', etiqueta: 'Cargas de estudiantes', Icono: FileUp },
    { ruta: '/analisis', etiqueta: 'Análisis de comentarios', Icono: MessagesSquare },
    { ruta: '/periodos', etiqueta: 'Periodos', Icono: CalendarRange },
    { ruta: '/ponderaciones', etiqueta: 'Ponderaciones', Icono: Scale },
    { ruta: '/facultades', etiqueta: 'Facultades', Icono: Building2 },
    { ruta: '/programas', etiqueta: 'Programas', Icono: BookOpen },
    { ruta: '/usuarios', etiqueta: 'Usuarios', Icono: UserCog },
  ],
}

export function Sidebar() {
  const { usuario, cerrarSesion } = useSesion()
  const enlaces = NAVEGACION[usuario.rol] ?? []
  const { pathname } = useLocation()
  const nav = useRef(null)

  // En móvil el menú es una fila con desplazamiento: se lleva el enlace activo a la vista.
  useEffect(() => {
    const activo = nav.current?.querySelector('.active')
    if (activo && nav.current.scrollWidth > nav.current.clientWidth) {
      nav.current.scrollTo({ left: activo.offsetLeft - nav.current.offsetLeft - 12 })
    }
  }, [pathname])

  return (
    <aside className="sidebar">
      <div className="sidebar__marca">
        <span className="sidebar__logo" aria-hidden="true">
          <GraduationCap size={20} strokeWidth={2.25} />
        </span>
        <span className="sidebar__nombre">
          <strong>DOCENTIA</strong>
          <span>Evaluación docente</span>
        </span>
      </div>

      <nav ref={nav} className="sidebar__nav" aria-label="Navegación principal">
        <span className="sidebar__seccion">{ETIQUETA_ROL[usuario.rol]}</span>
        {enlaces.map(({ ruta, etiqueta, Icono }) => (
          <NavLink key={ruta} to={ruta} className="sidebar__enlace">
            <Icono size={18} strokeWidth={1.75} aria-hidden="true" className="sidebar__icono" />
            {etiqueta}
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
        <Link to={RUTA_CAMBIO_CONTRASENA} className="sidebar__icono-boton" title="Cambiar contraseña">
          <KeyRound size={17} aria-hidden="true" />
          <span className="solo-lectores">Cambiar contraseña</span>
        </Link>
        <button type="button" className="sidebar__salir" onClick={() => cerrarSesion()}>
          <LogOut size={16} strokeWidth={2} aria-hidden="true" />
          Salir
        </button>
      </div>
    </aside>
  )
}
