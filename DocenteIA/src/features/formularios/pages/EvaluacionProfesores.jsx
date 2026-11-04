import { ClipboardCheck, RefreshCw, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { CifraPortada, Portada } from '../../../shared/components/ui/Portada/Portada.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarEvaluaciones } from '../../estudiante/services/estudianteService.js'
import '../components/FormularioInstrumento.css'

const ESTADO = {
  PENDIENTE: { etiqueta: 'Pendiente', clase: 'info' },
  ENVIADA: { etiqueta: 'Enviada', clase: 'exito' },
}

export default function EvaluacionProfesores() {
  const { usuario } = useSesion()
  const { datos, error, cargando, recargar } = useFetch(listarEvaluaciones)

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
        {datos?.evaluaciones.length > 0 && (
          <Portada
            id="portada-estudiante"
            icono={ShieldCheck}
            rotulo={`Periodo ${datos.periodo.nombre} · tus respuestas son anónimas`}
            titulo={`Hola, ${usuario.nombreCompleto.split(' ')[0]}`}
            detalle={
              datos.pendientes
                ? `Te ${datos.pendientes === 1 ? 'falta 1 profesor' : `faltan ${datos.pendientes} profesores`} por evaluar.`
                : 'Ya evaluaste a todos tus profesores. ¡Gracias!'
            }
            robot={datos.pendientes ? robotSenala : robotAprobada}
          >
            <div className="portada__cifras">
              <CifraPortada
                rotulo="Enviadas"
                valor={`${datos.evaluaciones.length - datos.pendientes} de ${datos.evaluaciones.length}`}
                nota="Evaluaciones de este periodo"
              />
              <CifraPortada rotulo="Pendientes" valor={datos.pendientes} nota="Elige un profesor abajo" />
            </div>
            <span className="portada-estudiante__riel" aria-hidden="true">
              <span
                style={{
                  width: `${((datos.evaluaciones.length - datos.pendientes) / datos.evaluaciones.length) * 100}%`,
                }}
              />
            </span>
          </Portada>
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
                  {e.estado === 'PENDIENTE' && (
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
