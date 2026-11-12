import { CalendarClock, CircleCheck, CircleDashed, RefreshCw, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, iniciales } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarEvaluaciones } from '../../estudiante/services/estudianteService.js'
import './EvaluacionProfesores.css'

const RADIO_ANILLO = 30
const CIRCUNFERENCIA = 2 * Math.PI * RADIO_ANILLO

/** Anillo de avance: cuántas evaluaciones van enviadas de las del periodo. */
function AnilloAvance({ enviadas, total }) {
  const fraccion = total ? enviadas / total : 0
  return (
    <div className="mis-evaluaciones__anillo" role="img" aria-label={`${enviadas} de ${total} evaluaciones enviadas`}>
      <svg viewBox="0 0 72 72" aria-hidden="true">
        <circle cx="36" cy="36" r={RADIO_ANILLO} className="mis-evaluaciones__anillo-fondo" />
        <circle
          cx="36"
          cy="36"
          r={RADIO_ANILLO}
          className="mis-evaluaciones__anillo-avance"
          strokeDasharray={CIRCUNFERENCIA}
          strokeDashoffset={CIRCUNFERENCIA * (1 - fraccion)}
        />
      </svg>
      <span aria-hidden="true">
        <strong>{enviadas}</strong>/{total}
      </span>
    </div>
  )
}

/**
 * Inicio del estudiante: una portada corta con el saludo, el avance y el cierre de la evaluación, y
 * una tarjeta por profesor (primero las pendientes) con su acción.
 */
export default function EvaluacionProfesores() {
  const { usuario } = useSesion()
  const { datos, error, cargando, recargar } = useFetch(listarEvaluaciones)
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)

  const evaluaciones = datos?.evaluaciones ?? []
  const total = evaluaciones.length
  const pendientes = datos?.pendientes ?? 0
  const enviadas = total - pendientes
  // Primero lo que falta por hacer; dentro de cada grupo, el orden que trae el servidor.
  const ordenadas = [...evaluaciones].sort((a, b) => (a.estado === 'PENDIENTE' ? 0 : 1) - (b.estado === 'PENDIENTE' ? 0 : 1))

  return (
    <>
      <Encabezado
        miga="Estudiante · Mis evaluaciones"
        titulo="Mis evaluaciones"
        subtitulo="Califica a cada profesor de tus cursos. Tus respuestas son anónimas."
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
        {datos && total === 0 && (
          <EstadoVista titulo="No tienes evaluaciones" mensaje="Cuando te matriculen en un curso verás aquí a cada profesor." />
        )}

        {total > 0 && (
          <>
            <section className="mis-evaluaciones__portada" aria-labelledby="mis-evaluaciones-saludo">
              <img className="mis-evaluaciones__robot" src={pendientes ? robotSenala : robotAprobada} alt="" aria-hidden="true" />
              <div className="mis-evaluaciones__saludo">
                <h2 id="mis-evaluaciones-saludo">Hola, {usuario.nombreCompleto.split(' ')[0]}</h2>
                <p>
                  {pendientes
                    ? `Te ${pendientes === 1 ? 'falta 1 profesor' : `faltan ${pendientes} profesores`} por evaluar.`
                    : 'Ya evaluaste a todos tus profesores. ¡Gracias!'}
                </p>
                <span className="mis-evaluaciones__anonimo">
                  <ShieldCheck size={14} aria-hidden="true" />
                  Periodo {datos.periodo.nombre} · nadie sabe qué respondiste
                </span>
              </div>
              <div className="mis-evaluaciones__resumen">
                <AnilloAvance enviadas={enviadas} total={total} />
                {recoleccion && (
                  <span className="mis-evaluaciones__cierre">
                    <CalendarClock size={15} aria-hidden="true" />
                    {recoleccion.abierta ? `Cierra el ${formatearFecha(recoleccion.fechaCierre)}` : 'La evaluación está cerrada'}
                  </span>
                )}
              </div>
            </section>

            <section aria-labelledby="mis-evaluaciones-profesores">
              <h2 id="mis-evaluaciones-profesores" className="mis-evaluaciones__titulo">
                Tus profesores
                <span>
                  {enviadas} de {total} enviadas
                </span>
              </h2>
              <ul className="mis-evaluaciones__tarjetas">
                {ordenadas.map((e) => {
                  const pendiente = e.estado === 'PENDIENTE'
                  return (
                    <li
                      key={e.idAsignacion}
                      className={`mis-evaluaciones__tarjeta mis-evaluaciones__tarjeta--${pendiente ? 'pendiente' : 'enviada'}`}
                    >
                      <div className="mis-evaluaciones__persona">
                        <span className="mis-evaluaciones__avatar" aria-hidden="true">
                          {iniciales(e.docente)}
                        </span>
                        <div>
                          <strong>{e.docente}</strong>
                          <span>
                            {e.curso} · Grupo {e.grupo}
                          </span>
                        </div>
                      </div>
                      <span className="mis-evaluaciones__estado">
                        {pendiente ? <CircleDashed size={15} aria-hidden="true" /> : <CircleCheck size={15} aria-hidden="true" />}
                        {pendiente ? 'Pendiente' : 'Enviada'}
                      </span>
                      {pendiente ? (
                        <Link
                          className="boton boton--primario mis-evaluaciones__accion"
                          to={`/evaluacion-profesores/${e.idAsignacion}`}
                          aria-label={`Evaluar a ${e.docente}`}
                        >
                          Evaluar
                        </Link>
                      ) : (
                        <BotonAccion
                          tipo="ver"
                          texto="Ver resumen"
                          nombre={`de ${e.docente}`}
                          a={`/evaluacion-profesores/${e.idAsignacion}`}
                        />
                      )}
                    </li>
                  )
                })}
              </ul>
            </section>
          </>
        )}
      </div>
    </>
  )
}
