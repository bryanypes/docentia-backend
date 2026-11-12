import { ChevronLeft, PenLine, Printer, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS, ETIQUETA_FUENTE, FUENTE_DEL_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, formatearNota, formatearPeso, iniciales } from '../../../shared/utils/formatters.js'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { PanelActa } from '../../actas/components/PanelActa.jsx'
import { PanelSeguimientoPlan } from '../../actas/components/PanelSeguimientoPlan.jsx'
import { useSesion } from '../../auth/sesionContext.js'
import { PanelCorrecciones } from '../../calificaciones/components/PanelCorrecciones.jsx'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { PanelComparativoPrograma } from '../components/PanelComparativoPrograma.jsx'
import { ESTADOS_PROCESO } from '../../proceso/estados.js'
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
            disabled={!datosProceso}
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
            <PanelComparativoPrograma idDocente={docente.idDocente} />
          )}
          <PanelFuentes
            notasPorFuente={notasPorFuente}
            promedio={promedio}
            descripcion="Las tres notas llegan por sus formularios: heteroevaluación, coevaluación y autoevaluación."
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
        <PanelActa idDocente={docente.idDocente} />
        <PanelSeguimientoPlan idDocente={docente.idDocente} puedeMarcar={usuario.rol === ROLES.COORDINADOR} />
        <PanelCorrecciones idDocente={docente.idDocente} />

        {puedeImprimir && datosProceso && (
          <article className="expediente-impresion" aria-label="Resumen imprimible del docente">
            <header>
              <p>DOCENTIA</p>
              <h1>Resumen de evaluación docente</h1>
            </header>
            <dl>
              <div><dt>Docente</dt><dd>{docente.nombreCompleto}</dd></div>
              <div><dt>Programa</dt><dd>{docente.nombrePrograma}</dd></div>
              <div><dt>Facultad</dt><dd>{docente.nombreFacultad}</dd></div>
              <div><dt>Periodo</dt><dd>{periodo?.nombre ?? 'Sin periodo activo'}</dd></div>
              <div><dt>Nota final</dt><dd>{formatearNota(promedio?.valor)}</dd></div>
              <div><dt>Clasificación</dt><dd><EtiquetaNivel promedio={promedio} /></dd></div>
              <div>
                <dt>Estado del proceso</dt>
                <dd>{ESTADOS_PROCESO[datosProceso.estado]?.etiqueta ?? 'No disponible'}</dd>
              </div>
            </dl>
            <h2>Evaluadores</h2>
            <table>
              <thead><tr><th>Evaluador</th><th>Nota</th><th>Peso</th></tr></thead>
              <tbody>
                {notasPorFuente.map((fuente) => (
                  <tr key={fuente.fuente}>
                    <th scope="row">{ETIQUETA_FUENTE[fuente.fuente] ?? fuente.fuente}</th>
                    <td>{fuente.nota === null ? 'Sin registrar' : formatearNota(fuente.nota)}</td>
                    <td>{formatearPeso(fuente.peso)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <h2>Compromisos vigentes</h2>
            {compromisosVigentes.length ? (
              <ul>
                {compromisosVigentes.map((compromiso) => (
                  <li key={compromiso.idCompromiso}>
                    {compromiso.descripcion} · Vence {formatearFecha(compromiso.fechaLimite)}
                  </li>
                ))}
              </ul>
            ) : <p>No hay compromisos de mejora vigentes.</p>}
          </article>
        )}
      </div>
    </>
  )
}
