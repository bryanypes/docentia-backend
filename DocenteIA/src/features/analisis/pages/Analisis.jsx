import { Check, Info } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { ETAPAS, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarDocentes } from '../../docentes/services/docentesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { AnalisisDocente } from '../components/AnalisisDocente.jsx'
import { MensajeIA } from '../components/DocenteIA.jsx'
import { PanoramaAnalisis } from '../components/PanoramaAnalisis.jsx'
import { MINIMO_ENCUESTAS } from '../estados.js'
import './Analisis.css'

/**
 * Cuándo se puede pedir el análisis, en tres pasos con el estado de cada uno: los estudiantes comentan,
 * la coordinación lo solicita y revisa los hallazgos. Los comentarios ya cargados se analizan durante
 * toda la revisión; solo los del formulario anónimo esperan a que cierre la recolección.
 */
function GuiaAnalisis({ recoleccionAbierta, fechaCierre, solicitado, esCoordinador }) {
  const pasos = [
    {
      titulo: 'Los estudiantes comentan',
      texto: recoleccionAbierta
        ? `Al final de su encuesta escriben un comentario anónimo. Los del formulario se suman al análisis cuando cierra la recolección${fechaCierre ? `, el ${formatearFecha(fechaCierre)}` : ''}.`
        : 'Al final de su encuesta escriben un comentario anónimo. La recolección ya cerró.',
      estado: 'hecho',
    },
    {
      titulo: esCoordinador ? 'Solicitas el análisis' : 'La coordinación lo solicita',
      texto: `Por docente, con al menos ${MINIMO_ENCUESTAS} encuestas con comentario. Se puede pedir de nuevo cuando lleguen más.`,
      estado: solicitado ? 'hecho' : 'actual',
    },
    {
      titulo: esCoordinador ? 'Revisas y publicas' : 'La coordinación revisa',
      texto: 'Cada hallazgo se valida antes de que el docente lo vea.',
      estado: solicitado ? 'actual' : 'pendiente',
    },
  ]
  return (
    <ol className="analisis__guia" aria-label="Cuándo se puede pedir el análisis">
      {pasos.map((paso, i) => (
        <li key={paso.titulo} className={`analisis__guia-paso analisis__guia-paso--${paso.estado}`}>
          <span className="analisis__guia-marca" aria-hidden="true">
            {paso.estado === 'hecho' ? <Check size={14} strokeWidth={3} /> : i + 1}
          </span>
          <div>
            <strong>
              {paso.titulo}
              {paso.estado === 'actual' && <em className="analisis__guia-ahora">Ahora</em>}
            </strong>
            <span>{paso.texto}</span>
          </div>
          <span className="solo-lectores">
            {paso.estado === 'hecho' ? ' (cumplido)' : paso.estado === 'actual' ? ' (paso actual)' : ' (pendiente)'}
          </span>
        </li>
      ))}
    </ol>
  )
}

export default function Analisis() {
  const { usuario } = useSesion()
  const esCoordinador = usuario.rol === ROLES.COORDINADOR
  const [parametros, setParametros] = useSearchParams()
  const docenteId = parametros.get('docente') ?? ''
  const docentes = useFetch(() => listarDocentes(null))
  const periodos = useFetch(listarPeriodos)
  const periodoActivo = periodos.datos?.find((periodo) => periodo.esActivo)
  const periodoId = parametros.get('periodo') ?? (periodoActivo ? String(periodoActivo.idPeriodo) : '')
  // La recolección solo puede estar abierta en el periodo activo; los anteriores ya cerraron.
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)
  const esPeriodoActivo = Boolean(periodoActivo) && String(periodoActivo.idPeriodo) === periodoId
  const recoleccionAbierta = esPeriodoActivo && Boolean(recoleccion?.abierta)
  const [verAyuda, setVerAyuda] = useState(false)

  function cambiarFiltro(nombre, valor) {
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(nombre, valor)
    else nuevos.delete(nombre)
    setParametros(nuevos, { replace: true, state: { conservarFoco: true } })
  }

  return (
    <>
      <Encabezado
        miga={`${esCoordinador ? 'Coordinación' : 'Vicerrectoría'} · Análisis de comentarios`}
        titulo="Análisis de comentarios de estudiantes"
        subtitulo="Lo que los estudiantes escriben al final de su encuesta, resumido en fortalezas y oportunidades con citas textuales."
        ayuda={
          <button
            type="button"
            className="analisis__ayuda"
            onClick={() => setVerAyuda(true)}
            aria-label="Cómo funciona el análisis y cuándo se puede pedir"
            title="Cómo funciona el análisis y cuándo se puede pedir"
          >
            <Info size={18} aria-hidden="true" />
          </button>
        }
      />
      {verAyuda && (
        <Modal
          titulo="Cómo funciona el análisis"
          descripcion="Qué hace DocenteIA con los comentarios y cuándo se puede pedir."
          icono={Info}
          onCerrar={() => setVerAyuda(false)}
        >
          <MensajeIA robot="saludo" titulo="Hola, soy DocenteIA: analizo los comentarios de los estudiantes" tamano="compacto">
            <p>
              Leo los comentarios anónimos que dejan en su encuesta, ya sin los ofensivos, y los agrupo en fortalezas y
              oportunidades de mejora citando sus palabras. Así sabes cuándo puedes pedirme el análisis:
            </p>
          </MensajeIA>
          <GuiaAnalisis
            recoleccionAbierta={recoleccionAbierta}
            fechaCierre={esPeriodoActivo ? recoleccion?.fechaCierre : null}
            solicitado={false}
            esCoordinador={esCoordinador}
          />
        </Modal>
      )}
      <div className="pagina">
        <div className="analisis__filtros">
          <Campo id="docenteAnalisis" etiqueta="Docente">
            <select
              id="docenteAnalisis"
              className="campo__control"
              value={docenteId}
              onChange={(e) => cambiarFiltro('docente', e.target.value)}
            >
              <option value="">Todos · panorama</option>
              {(docentes.datos ?? []).map((docente) => (
                <option key={docente.idDocente} value={docente.idDocente}>
                  {docente.nombreCompleto} · {docente.nombrePrograma}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="periodoAnalisis" etiqueta="Periodo">
            <select
              id="periodoAnalisis"
              className="campo__control"
              value={periodoId}
              onChange={(e) => cambiarFiltro('periodo', e.target.value)}
            >
              <option value="">Elige un periodo</option>
              {(periodos.datos ?? []).map((periodo) => (
                <option key={periodo.idPeriodo} value={periodo.idPeriodo}>
                  {periodo.nombre}
                  {periodo.esActivo ? ' (activo)' : ''}
                </option>
              ))}
            </select>
          </Campo>
        </div>

        {!docenteId || !periodoId ? (
          periodoId ? (
            <PanoramaAnalisis periodoId={periodoId} onElegirDocente={(id) => cambiarFiltro('docente', id)} />
          ) : (
            <MensajeIA robot="esperando" tono="violeta" titulo="Elige un periodo para empezar">
              <p>Te mostraré el panorama de todos los docentes y, si eliges uno, su análisis en detalle.</p>
            </MensajeIA>
          )
        ) : (
          <AnalisisDocente
            key={`${docenteId}-${periodoId}`}
            idDocente={docenteId}
            periodoId={periodoId}
            esCoordinador={esCoordinador}
          />
        )}
      </div>
    </>
  )
}
