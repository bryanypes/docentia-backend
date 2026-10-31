import { MessagesSquare, RefreshCw } from 'lucide-react'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota, listarFuentes } from '../../../shared/utils/formatters.js'
import { PanelCompromisos, PanelDimensiones } from '../components/PanelesExpediente.jsx'
import { obtenerExpedientePropio } from '../services/expedientesService.js'
import './MiEvaluacion.css'

/**
 * Vista del docente. Usa solo ExpedientePropioResponse: no recibe ni muestra las notas
 * individuales de coordinación o vicerrectoría.
 */
export default function MiEvaluacion() {
  const expediente = useFetch(obtenerExpedientePropio)
  const periodoNombre = expediente.datos?.periodo?.nombre

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

  const { periodo, promedio, dimensiones, compromisosVigentes } = expediente.datos
  const completo = promedio?.estado === 'COMPLETO'

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
          <div className="rejilla">
            <section className="mi-evaluacion__resumen">
              <div className="mi-evaluacion__cabecera">
                <span className="mi-evaluacion__ilustracion">
                  <img src={robotSaludo} alt="" aria-hidden="true" />
                </span>
                <div>
                  <div className="mi-evaluacion__rotulo">Tu promedio {periodo.nombre}</div>
                  <strong className="mi-evaluacion__valor">
                    {completo ? formatearNota(promedio.valor) : 'Pendiente'}
                  </strong>
                </div>
              </div>

              <EtiquetaNivel promedio={promedio} />

              <p className="mi-evaluacion__texto">
                {completo
                  ? 'Promedio ponderado del periodo. Incluye la evaluación de tus estudiantes y las evaluaciones institucionales, que se reportan únicamente dentro de este promedio.'
                  : `Tu promedio estará disponible cuando se registre la calificación de ${listarFuentes(promedio.fuentesPendientes)}.`}
              </p>

              <div className="mi-evaluacion__fuentes">
                <div>
                  <strong>Estudiantes, coordinación y vicerrectoría</strong>
                  <span>Las tres fuentes se combinan según la ponderación del periodo.</span>
                </div>
                <span className="mi-evaluacion__chip">Solo en el promedio</span>
              </div>
            </section>

            <PanelDimensiones titulo="Mis dimensiones" dimensiones={dimensiones} />
            <PanelCompromisos titulo="Mis compromisos" compromisos={compromisosVigentes} />
            <Panel titulo="Lo que dicen tus estudiantes" icono={MessagesSquare}>
              <p>
                La coordinación revisa los comentarios de tus estudiantes. Verás las fortalezas y
                oportunidades de mejora cuando las valide.
              </p>
            </Panel>
          </div>
        )}
      </div>
    </>
  )
}
