import { RefreshCw } from 'lucide-react'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota, listarFuentes, textoFormula } from '../../../shared/utils/formatters.js'
import { PanelSeguimientoPlan } from '../../actas/components/PanelSeguimientoPlan.jsx'
import { PanelHallazgosPropios } from '../../analisis/components/PanelHallazgosPropios.jsx'
import { PanelMiAutoevaluacion } from '../../formularios/components/PanelMiAutoevaluacion.jsx'
import { obtenerConfiguracionEvaluacion } from '../../kpis/services/configuracionService.js'
import { LineaTiempo } from '../../periodos/components/LineaTiempo.jsx'
import { PanelProcesoPropio } from '../../proceso/components/PanelProcesoPropio.jsx'
import { PanelCompromisos } from '../components/PanelesExpediente.jsx'
import { obtenerExpedientePropio } from '../services/expedientesService.js'
import './MiEvaluacion.css'

/**
 * Vista del docente. Usa solo ExpedientePropioResponse: no recibe ni muestra las notas
 * individuales de cada fuente ni un promedio parcial.
 */
export default function MiEvaluacion() {
  const expediente = useFetch(obtenerExpedientePropio)
  const periodoNombre = expediente.datos?.periodo?.nombre
  const configuracion = useFetch(obtenerConfiguracionEvaluacion)

  const encabezado = (
    <Encabezado
      miga="Docente · Mi evaluación"
      titulo={periodoNombre ? `Mi evaluación ${periodoNombre}` : 'Mi evaluación'}
      subtitulo="Tu resultado del periodo y los compromisos de mejora acordados."
    />
  )

  if (expediente.cargando) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <Cargando texto="Cargando tu evaluación" />
        </div>
      </>
    )
  }

  if (expediente.error) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <EstadoVista tipo="error" titulo="No fue posible cargar tu evaluación" mensaje={expediente.error.message}>
            <Boton icono={RefreshCw} onClick={expediente.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        </div>
      </>
    )
  }

  const { periodo, promedio, compromisosVigentes } = expediente.datos
  // Sin el campo (backend anterior) se comporta como antes.
  const habilitado = expediente.datos.resultadoHabilitado !== false
  const completo = promedio?.estado === 'COMPLETO' && habilitado
  const esperaReunion = promedio?.estado === 'COMPLETO' && !habilitado

  return (
    <>
      {encabezado}
      <div className="pagina">
        <MensajeNavegacion />
        {!periodo ? (
          <EstadoVista
            titulo="Aún no hay un periodo de evaluación activo"
            mensaje="Cuando la vicerrectoría abra el periodo, tu resultado aparecerá aquí."
          />
        ) : (
          <>
          <LineaTiempo />
          <PanelProcesoPropio />
          <div className="rejilla mi-evaluacion__rejilla">
            <section className="mi-evaluacion__resumen">
              <div className="mi-evaluacion__cabecera">
                <span className="mi-evaluacion__ilustracion">
                  <img src={robotSaludo} alt="" aria-hidden="true" />
                </span>
                <div>
                  <div className="mi-evaluacion__rotulo">Tu promedio {periodo.nombre}</div>
                  <strong className="mi-evaluacion__valor">
                    {completo ? formatearNota(promedio.valor) : esperaReunion ? 'En tu cita' : 'Pendiente'}
                  </strong>
                </div>
              </div>

              {esperaReunion ? (
                <span className="pildora pildora--turquesa">Lista para tu reunión</span>
              ) : (
                <EtiquetaNivel promedio={promedio} />
              )}

              {completo && promedio.categoria && (
                <p className="mi-evaluacion__texto">
                  <strong>Acción institucional:</strong> {promedio.categoria.accionInstitucional}
                </p>
              )}

              <p className="mi-evaluacion__texto">
                {esperaReunion
                  ? 'Tus tres evaluaciones están completas. Tu nota, tus compromisos y el acta se te muestran en la reunión de retroalimentación, cuando la coordinación registre tu asistencia.'
                  : completo
                  ? 'Promedio ponderado del periodo. Incluye la evaluación de tus estudiantes y las evaluaciones institucionales, que se reportan únicamente dentro de este promedio.'
                  : `Tu promedio estará disponible cuando se registre la calificación de ${listarFuentes(promedio.fuentesPendientes)}.`}
              </p>

              <div className="mi-evaluacion__fuentes">
                <div>
                  <strong>Estudiantes, coordinación y autoevaluación</strong>
                  <span>{configuracion.datos ? textoFormula(configuracion.datos) : 'Las tres fuentes se combinan según la ponderación del periodo.'}</span>
                </div>
                <span className="mi-evaluacion__chip">Solo en el promedio</span>
              </div>
            </section>

            <PanelMiAutoevaluacion />
            <PanelCompromisos
              titulo="Mis compromisos"
              compromisos={compromisosVigentes}
              className="mi-evaluacion__compromisos"
            />
            <PanelHallazgosPropios />
          </div>
          <PanelSeguimientoPlan />
          </>
        )}
      </div>
    </>
  )
}
