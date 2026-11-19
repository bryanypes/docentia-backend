import { ChevronLeft, PenLine, Printer, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
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
import { PanelSeguimientoPlan } from '../../actas/components/PanelSeguimientoPlan.jsx'
import { useSesion } from '../../auth/sesionContext.js'
import { PanelCorrecciones } from '../../calificaciones/components/PanelCorrecciones.jsx'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { PanelComparativoPrograma } from '../components/PanelComparativoPrograma.jsx'
import { ResumenImprimible } from '../components/ResumenImprimible.jsx'
import { PanelProceso } from '../../proceso/components/PanelProceso.jsx'
import {
  DatosDocente,
  PanelCompromisos,
  PanelDimensiones,
  PanelFuentes,
  PanelHistorico,
} from '../components/PanelesExpediente.jsx'
import { obtenerExpedienteDocente } from '../services/expedientesService.js'
import './Expediente.css'

export default function ExpedienteDocente() {
  const { idDocente } = useParams()
  const { usuario } = useSesion()
  const navegar = useNavigate()
  const [procesoCargado, setProcesoCargado] = useState(null)
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)
  const expediente = useFetch(() => obtenerExpedienteDocente(idDocente), [idDocente])
  const puedeImprimir = [ROLES.COORDINADOR, ROLES.VICERRECTOR].includes(usuario.rol)
  const datosProceso = procesoCargado?.idDocente === idDocente ? procesoCargado.datos : null
  const guardarProceso = useCallback(
    (datos) => setProcesoCargado({ idDocente, datos }),
    [idDocente],
  )

  const encabezado = (
    <Encabezado
      miga="Docentes · Expediente"
      titulo="Expediente del docente"
      subtitulo="Calificación por evaluador, resultado por dimensión, histórico y compromisos vigentes."
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
        <Link to="/docentes" className="enlace-volver">
          <ChevronLeft size={16} aria-hidden="true" />
          Volver al listado
        </Link>
        {puedeImprimir && (
          <Boton
            className="expediente__imprimir"
            icono={Printer}
            onClick={() => window.print()}
          >
            Imprimir resumen
          </Boton>
        )}

        {!periodo && (
          <Aviso tipo="alerta" titulo="No hay un periodo académico activo">
            La vicerrectoría debe activar uno para registrar calificaciones.
          </Aviso>
        )}

        <div className="rejilla">
          <Panel>
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
          </Panel>

          {periodo && puedeImprimir && (
            <PanelComparativoPrograma idDocente={docente.idDocente} evaluadoEsCoordinador={docente.esCoordinador} />
          )}
          <PanelFuentes
            notasPorFuente={notasPorFuente}
            promedio={promedio}
            descripcion="Las tres notas llegan por sus formularios: heteroevaluación, coevaluación y autoevaluación."
            evaluadoEsCoordinador={docente.esCoordinador}
          >
            {miFuente && (
              <>
                <Boton
                  bloque
                  className="expediente__calificar"
                  icono={PenLine}
                  disabled={!periodo || Boolean(recoleccion && !recoleccion.abierta)}
                  onClick={() => navegar(`/coevaluaciones/${docente.idDocente}`)}
                >
                  {yaCalifique ? 'Ver mi coevaluación' : 'Responder coevaluación'}
                </Boton>
                <div className="expediente__etapa">
                  <AvisoEtapaCerrada ventana={recoleccion} accion="Calificar" />
                </div>
              </>
            )}
          </PanelFuentes>

          <PanelDimensiones dimensiones={dimensiones} />
        </div>

        <div className="rejilla">
          <PanelHistorico historico={historico} />
          <PanelCompromisos compromisos={compromisosVigentes} />
        </div>

        {periodo && <PanelProceso idDocente={docente.idDocente} onDatos={guardarProceso} />}
        <PanelActa idDocente={docente.idDocente} evaluadoEsCoordinador={docente.esCoordinador} />
        <PanelSeguimientoPlan idDocente={docente.idDocente} puedeMarcar={usuario.rol === ROLES.COORDINADOR} />
        <PanelCorrecciones idDocente={docente.idDocente} />

        {puedeImprimir && (
          <ResumenImprimible
            docente={docente}
            periodo={periodo}
            promedio={promedio}
            notasPorFuente={notasPorFuente}
            compromisos={compromisosVigentes}
            proceso={datosProceso}
          />
        )}
      </div>
    </>
  )
}
