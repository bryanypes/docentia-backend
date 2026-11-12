import { useSesion } from '../features/auth/sesionContext.js'
import MiEvaluacion from '../features/expedientes/pages/MiEvaluacion.jsx'
import EvaluacionProfesores from '../features/formularios/pages/EvaluacionProfesores.jsx'
import Inicio from '../features/kpis/pages/Inicio.jsx'
import { Navigate } from 'react-router-dom'
import { panelInicioDelRol, rutaInicioDelRol } from './rutasPorRol.js'

const PANELES = {
  GESTION: Inicio,
  MI_EVALUACION: MiEvaluacion,
  EVALUACION_PROFESORES: EvaluacionProfesores,
}

/** /inicio es la misma ruta para todos; el contenido cambia según los permisos del rol. */
export default function InicioPorRol() {
  const { usuario } = useSesion()
  const panel = panelInicioDelRol(usuario.rol)
  if (panel === 'REDIRIGIR') return <Navigate to={rutaInicioDelRol(usuario.rol)} replace />
  const Panel = PANELES[panel]
  return <Panel />
}
