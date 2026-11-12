import { ClipboardCheck, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { mensajeVentanaCerrada } from '../../estudiante/estadoVentana.js'
import { listarEvaluaciones } from '../../estudiante/services/estudianteService.js'
import '../components/FormularioInstrumento.css'

const ESTADO = {
  PENDIENTE: { etiqueta: 'Pendiente', clase: 'info' },
  ENVIADA: { etiqueta: 'Enviada', clase: 'exito' },
}

export default function EvaluacionProfesores() {
  const { datos, error, cargando, recargar } = useFetch(listarEvaluaciones)
  const cerrada = mensajeVentanaCerrada(useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES))
  const terminadas = datos?.evaluaciones.length > 0 && datos.pendientes === 0

  return (
    <>
      <Encabezado
        miga="Estudiante · Mis evaluaciones"
        titulo="Mis evaluaciones"
        subtitulo={
          datos
            ? `Periodo ${datos.periodo.nombre}. ${datos.pendientes} pendientes. Tus respuestas son anónimas.`
            : 'Califica a cada profesor de tus cursos. Tus respuestas son anónimas.'
        }
      />
      <div className="pagina">
        {cargando && !datos && <Cargando texto="Cargando tus evaluaciones" bloques={1} />}
        {error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar tus evaluaciones" mensaje={error.message}>
            <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!cargando && !error && !datos && (
          <EstadoVista titulo="No hay un periodo activo" mensaje="Cuando abra la evaluación verás aquí a tus profesores." />
        )}
        {datos?.evaluaciones.length === 0 && (
          <EstadoVista titulo="No tienes evaluaciones" mensaje="Cuando te matriculen en un curso verás aquí a cada profesor." />
        )}
        {terminadas && (
          <Aviso tipo="exito" titulo="Ya completaste todas tus evaluaciones. ¡Gracias por participar!">
            Tus respuestas quedaron guardadas de forma anónima.
          </Aviso>
        )}
        {cerrada && !terminadas && datos?.evaluaciones.length > 0 && (
          <Aviso tipo="alerta" titulo={cerrada.titulo}>
            {cerrada.texto}
          </Aviso>
        )}
        {datos?.evaluaciones.length > 0 && (
          <Panel titulo="Tus profesores" icono={ClipboardCheck}>
            <ul className="evaluaciones-estudiante">
              {datos.evaluaciones.map((e) => (
                <li key={e.idAsignacion} className="evaluaciones-estudiante__item">
                  <div>
                    <strong>{e.docente}</strong>
                    <span>
                      {e.curso} · Grupo {e.grupo}
                    </span>
                  </div>
                  <span className={`pildora pildora--${ESTADO[e.estado].clase}`}>{ESTADO[e.estado].etiqueta}</span>
                  {e.estado === 'PENDIENTE' && !cerrada && (
                    <Link
                      className="boton boton--primario"
                      to={`/evaluacion-profesores/${e.idAsignacion}`}
                      aria-label={`Evaluar a ${e.docente}`}
                    >
                      Evaluar
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          </Panel>
        )}
      </div>
    </>
  )
}
