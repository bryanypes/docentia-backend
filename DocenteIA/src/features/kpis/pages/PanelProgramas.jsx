import { ChartColumn, RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { ETIQUETA_ROL, NIVELES, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { obtenerPendientesCoevaluacion } from '../../formularios/services/formulariosService.js'
import { TablaCobertura } from '../../heteroevaluacion/pages/Matricula.jsx'
import { obtenerCobertura } from '../../heteroevaluacion/services/matriculaService.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { obtenerPanelProgramas } from '../services/kpisService.js'
import '../components/PanelesSeguimiento.css'
import './PanelProgramas.css'

const CLASE = Object.fromEntries(NIVELES.map(({ codigo, clase }) => [codigo, clase]))

function porcentaje(valor) {
  return `${Number(valor).toLocaleString('es-CO')} %`
}

function Distribucion({ fila }) {
  return (
    <>
      {fila.evaluados > 0 && (
        <div className="distribucion__barra" aria-hidden="true">
          {fila.distribucion
            .filter((c) => c.cantidad > 0)
            .map((c) => (
              <span
                key={c.codigo}
                className={`distribucion__segmento distribucion__segmento--${CLASE[c.codigo]}`}
                style={{ flexGrow: c.cantidad }}
              />
            ))}
        </div>
      )}
      <ul className="panel-programas__conteos">
        {fila.distribucion.map((c) => (
          <li key={c.codigo}>
            {c.etiqueta}: <strong>{c.cantidad}</strong> ({porcentaje(c.porcentaje)})
          </li>
        ))}
      </ul>
    </>
  )
}

function Fila({ fila }) {
  return (
    <>
      <p className="panel-programas__datos">
        Cobertura <strong>{porcentaje(fila.cobertura)}</strong> ({fila.evaluados} de {fila.docentes}) · Promedio{' '}
        <strong>{formatearNota(fila.promedio)}</strong>
      </p>
      <Distribucion fila={fila} />
    </>
  )
}

function Participacion() {
  const { datos } = useFetch(obtenerPendientesCoevaluacion)
  const programas = datos?.programas ?? []
  const paginacion = usePaginacion(programas, {
    clavePagina: 'paginaParticipacion',
    claveTamano: 'tamanoParticipacion',
  })
  if (!programas.length) return null
  return (
    <Panel titulo="Participación en formularios" descripcion="Solo conteos, sin puntajes.">
      <div className="tabla-contenedor">
        <table className="tabla tabla--apilable tabla--cifras">
          <thead>
            <tr>
              <th scope="col">Programa</th>
              <th scope="col">Docentes</th>
              <th scope="col">Coevaluaciones enviadas</th>
              <th scope="col">En borrador</th>
              <th scope="col">Sin iniciar</th>
              <th scope="col">Autoevaluaciones enviadas</th>
            </tr>
          </thead>
          <tbody>
            {paginacion.visibles.map((p) => (
              <tr key={p.programa}>
                <td className="tabla__fuerte">{p.programa}</td>
                <td data-etiqueta="Docentes">{p.docentes}</td>
                <td data-etiqueta="Coevaluaciones enviadas">{p.enviadas}</td>
                <td data-etiqueta="En borrador">{p.borrador}</td>
                <td data-etiqueta="Sin iniciar">{p.sinIniciar}</td>
                <td data-etiqueta="Autoevaluaciones enviadas">{p.autoevaluacionesEnviadas}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginacion {...paginacion} etiqueta="participación por programa" />
    </Panel>
  )
}

function CoberturaEstudiantes() {
  const { datos } = useFetch(() => obtenerCobertura(0))
  const programas = datos?.programas ?? []
  const paginacion = usePaginacion(programas, {
    clavePagina: 'paginaCobertura',
    claveTamano: 'tamanoCobertura',
  })
  if (!programas.length) return null
  return (
    <Panel
      titulo="Cobertura de la evaluación de estudiantes"
      descripcion={`${datos.total.respondieron} de ${datos.total.matriculados} matriculados respondieron.`}
    >
      <TablaCobertura filas={paginacion.visibles} primeraColumna="Programa" />
      <Paginacion {...paginacion} etiqueta="cobertura por programa" />
    </Panel>
  )
}

/** Panel I11: solo agregados. A propósito no enlaza a ningún expediente individual. */
export default function PanelProgramas() {
  const { usuario } = useSesion()
  // Visualización lee el panel de toda la universidad, como vicerrectoría.
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR || usuario.rol === ROLES.VISUALIZADOR
  const [filtros, actualizarFiltros] = useFiltrosUrl({ periodo: '', facultad: '' })
  const periodoId = filtros.periodo
  const facultadId = filtros.facultad
  const periodos = useFetch(listarPeriodos)
  const facultades = useFetch(() => (esVicerrector ? listarFacultades() : Promise.resolve([])), [esVicerrector])
  const cargar = useCallback(() => obtenerPanelProgramas({ periodoId, facultadId }), [periodoId, facultadId])
  const panel = useFetch(cargar, [cargar])
  const paginacionProgramas = usePaginacion(panel.datos?.programas ?? [], {
    clavePagina: 'paginaPanelProgramas',
    claveTamano: 'tamanoPanelProgramas',
  })

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Panel por programa`}
        titulo="Panel por programa"
        subtitulo="Cobertura, promedio y distribución por categoría. Cuenta los promedios completos; no identifica docentes."
      />
      <div className="pagina">
        <div className="panel-programas__filtros">
          <Campo id="periodoPanel" etiqueta="Periodo">
            <select
              id="periodoPanel"
              className="campo__control"
              value={periodoId}
              onChange={(e) => actualizarFiltros({ periodo: e.target.value })}
            >
              <option value="">Periodo activo</option>
              {(periodos.datos ?? []).map((p) => (
                <option key={p.idPeriodo} value={p.idPeriodo}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </Campo>
          {esVicerrector && (
            <Campo id="facultadPanel" etiqueta="Facultad">
              <select
                id="facultadPanel"
                className="campo__control"
                value={facultadId}
                onChange={(e) => actualizarFiltros({ facultad: e.target.value })}
              >
                <option value="">Toda la universidad</option>
                {(facultades.datos ?? []).map((f) => (
                  <option key={f.idFacultad} value={f.idFacultad}>
                    {f.nombre}
                  </option>
                ))}
              </select>
            </Campo>
          )}
        </div>
        {panel.cargando && !panel.datos && <Cargando texto="Cargando el panel" bloques={2} />}
        {panel.error && (
          <EstadoVista
            tipo={panel.error.status === 409 ? 'vacio' : 'error'}
            titulo={panel.error.status === 409 ? 'No hay un periodo activo' : 'No fue posible cargar el panel'}
            mensaje={panel.error.message}
          >
            <Boton icono={RefreshCw} onClick={panel.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {panel.datos && (
          <>
            <Panel titulo={`Total · ${panel.datos.periodo.nombre}`} icono={ChartColumn}>
              <Fila fila={panel.datos.total} />
            </Panel>
            {panel.datos.programas.length === 0 && (
              <EstadoVista titulo="Sin programas" mensaje="No hay docentes activos en este alcance." />
            )}
            <div className="rejilla">
              {paginacionProgramas.visibles.map((fila) => (
                <Panel key={fila.idPrograma} titulo={fila.programa} descripcion={fila.facultad}>
                  <Fila fila={fila} />
                </Panel>
              ))}
            </div>
            <Paginacion {...paginacionProgramas} etiqueta="programas del panel" />
            {esVicerrector && <Participacion />}
            {esVicerrector && <CoberturaEstudiantes />}
          </>
        )}
      </div>
    </>
  )
}
