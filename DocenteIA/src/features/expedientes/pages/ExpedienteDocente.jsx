import { ChartColumnBig, FileSignature, History, PenLine, RefreshCw, Sparkles } from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS, FUENTE_DEL_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { iniciales } from '../../../shared/utils/formatters.js'
import { PanelActa } from '../../actas/components/PanelActa.jsx'
import { ResumenAnalisisIA } from '../../analisis/components/ResumenAnalisisIA.jsx'
import { PanelSeguimientoPlan } from '../../actas/components/PanelSeguimientoPlan.jsx'
import { useSesion } from '../../auth/sesionContext.js'
import { PanelCorrecciones } from '../../calificaciones/components/PanelCorrecciones.jsx'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { PanelProceso } from '../../proceso/components/PanelProceso.jsx'
import {
  DatosDocente,
  PanelCompromisos,
  PanelDimensiones,
  PanelFuentes,
  PanelHistorico,
  ValorPromedio,
} from '../components/PanelesExpediente.jsx'
import { obtenerExpedienteDocente } from '../services/expedientesService.js'
import './Expediente.css'

// Pestañas del expediente: cada una reúne lo que antes eran varias tarjetas sueltas.
const PESTANAS = [
  { clave: 'resultados', etiqueta: 'Resultados', corta: 'Resultados', Icono: ChartColumnBig },
  { clave: 'analisis', etiqueta: 'Análisis IA', corta: 'IA', Icono: Sparkles },
  { clave: 'historico', etiqueta: 'Histórico y compromisos', corta: 'Histórico', Icono: History },
  { clave: 'acta', etiqueta: 'Proceso y acta', corta: 'Acta', Icono: FileSignature },
]

export default function ExpedienteDocente() {
  const { idDocente } = useParams()
  const { usuario } = useSesion()
  const navegar = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const pestana = PESTANAS.some((p) => p.clave === parametros.get('pestana')) ? parametros.get('pestana') : 'resultados'
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)
  const expediente = useFetch(() => obtenerExpedienteDocente(idDocente), [idDocente])

  const encabezado = (
    <Encabezado
      volver={{ a: '/docentes', texto: 'Docentes' }}
      titulo="Expediente del docente"
      subtitulo="Resultados, análisis de los comentarios de estudiantes, histórico, compromisos y acta."
    />
  )

  if (expediente.cargando) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <Cargando texto="Cargando expediente" />
        </div>
      </>
    )
  }

  if (expediente.error) {
    const noEncontrado = expediente.error.status === 404
    return (
      <>
        {encabezado}
        <div className="pagina">
          <EstadoVista
            tipo={noEncontrado ? 'vacio' : 'error'}
            titulo={noEncontrado ? 'Docente no encontrado' : 'No fue posible cargar el expediente'}
            mensaje={noEncontrado ? 'El docente no existe o no está dentro de tu alcance.' : expediente.error.message}
          >
            {noEncontrado ? (
              <Link to="/docentes">Volver al listado</Link>
            ) : (
              <Boton icono={RefreshCw} onClick={expediente.recargar}>Intentar de nuevo</Boton>
            )}
          </EstadoVista>
        </div>
      </>
    )
  }

  const { docente, periodo, promedio, notasPorFuente, dimensiones, historico, compromisosVigentes } =
    expediente.datos
  const miFuente = FUENTE_DEL_ROL[usuario.rol]
  const yaCalifique = notasPorFuente.some((f) => f.fuente === miFuente && f.nota !== null)

  return (
    <>
      {encabezado}
      <div className="pagina">

        {!periodo && (
          <Aviso tipo="alerta" titulo="No hay un periodo académico activo">
            La vicerrectoría debe activar uno para registrar calificaciones.
          </Aviso>
        )}

        <Panel className="expediente__cabecera">
          <div className="expediente__perfil">
            <span className="expediente__avatar" aria-hidden="true">
              {iniciales(docente.nombreCompleto)}
            </span>
            <div>
              <h2 className="expediente__nombre">{docente.nombreCompleto}</h2>
              <p className="expediente__correo">{docente.correo}</p>
            </div>
          </div>
          <DatosDocente docente={docente} periodo={periodo} promedio={promedio} />
          <div className="expediente__nota">
            <span className="rotulo">Nota final</span>
            <ValorPromedio promedio={promedio} />
          </div>
          {miFuente && (
            <div className="expediente__calificar">
              <Boton
                icono={PenLine}
                disabled={!periodo || Boolean(recoleccion && !recoleccion.abierta)}
                onClick={() => navegar(`/coevaluaciones/${docente.idDocente}`)}
              >
                {yaCalifique ? 'Ver mi coevaluación' : 'Responder coevaluación'}
              </Boton>
              <AvisoEtapaCerrada ventana={recoleccion} accion="Calificar" />
            </div>
          )}
        </Panel>

        <section className="expediente-pestanas">
          <div className="expediente-pestanas__barra" role="tablist" aria-label="Secciones del expediente">
            {PESTANAS.map(({ clave, etiqueta, corta, Icono }) => (
              <button
                key={clave}
                id={`pestana-${clave}`}
                type="button"
                role="tab"
                aria-selected={pestana === clave}
                aria-controls="expediente-contenido"
                className={`expediente-pestanas__pestana${pestana === clave ? ' expediente-pestanas__pestana--activa' : ''}`}
                onClick={() => {
                  const nuevos = new URLSearchParams(parametros)
                  if (clave === 'resultados') nuevos.delete('pestana')
                  else nuevos.set('pestana', clave)
                  setParametros(nuevos, { replace: true, state: { conservarFoco: true } })
                }}
              >
                <Icono size={16} aria-hidden="true" />
                <span className="expediente-pestanas__larga">{etiqueta}</span>
                <span className="expediente-pestanas__corta" aria-hidden="true">
                  {corta}
                </span>
              </button>
            ))}
          </div>

          <div id="expediente-contenido" className="expediente-pestanas__contenido" role="tabpanel" aria-labelledby={`pestana-${pestana}`}>
            {pestana === 'resultados' && (
              <>
                <div className="expediente-pestanas__columnas">
                  <PanelFuentes
                    notasPorFuente={notasPorFuente}
                    promedio={promedio}
                    descripcion="Estudiantes llega por su encuesta o CSV; coevaluación y autoevaluación, por sus formularios."
                  />
                  <PanelDimensiones dimensiones={dimensiones} />
                </div>
                <PanelCorrecciones idDocente={docente.idDocente} />
              </>
            )}
            {pestana === 'analisis' && <ResumenAnalisisIA
                idDocente={docente.idDocente}
                periodoId={periodo?.idPeriodo}
                esCoordinador={usuario.rol === ROLES.COORDINADOR}
              />}
            {pestana === 'historico' && (
              <div className="expediente-pestanas__columnas">
                <PanelHistorico historico={historico} />
                <PanelCompromisos compromisos={compromisosVigentes} />
              </div>
            )}
            {pestana === 'acta' && (
              <>
                {periodo && <PanelProceso idDocente={docente.idDocente} />}
                <PanelActa idDocente={docente.idDocente} />
                <PanelSeguimientoPlan idDocente={docente.idDocente} puedeMarcar={usuario.rol === ROLES.COORDINADOR} />
              </>
            )}
          </div>
        </section>
      </div>
    </>
  )
}
