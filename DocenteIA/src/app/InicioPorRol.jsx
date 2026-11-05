import { useSesion } from '../features/auth/sesionContext.js'
import MiEvaluacion from '../features/expedientes/pages/MiEvaluacion.jsx'
import EvaluacionProfesores from '../features/formularios/pages/EvaluacionProfesores.jsx'
import Inicio from '../features/kpis/pages/Inicio.jsx'
import { panelInicioDelRol } from './rutasPorRol.js'

const PANELES = {
  GESTION: Inicio,
  MI_EVALUACION: MiEvaluacion,
  EVALUACION_PROFESORES: EvaluacionProfesores,
}

/** /inicio es la misma ruta para todos; el contenido cambia según los permisos del rol. */
export default function InicioPorRol() {
  const { usuario } = useSesion()
  const Panel = PANELES[panelInicioDelRol(usuario.rol)]
  return <Panel />
}
