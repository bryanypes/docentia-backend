import { ChartColumn, ChartColumnBig, RefreshCw, Search, X } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { ETIQUETA_ROL, NIVELES, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { formatearNota, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { obtenerPendientesCoevaluacion } from '../../formularios/services/formulariosService.js'
import { TablaCobertura } from '../../heteroevaluacion/components/TablaCobertura.jsx'
import { obtenerCobertura } from '../../heteroevaluacion/services/matriculaService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { obtenerPanelProgramas } from '../services/kpisService.js'
import '../components/PanelesSeguimiento.css'
import './PanelProgramas.css'

const CLASE = Object.fromEntries(NIVELES.map(({ codigo, clase }) => [codigo, clase]))

const ORDENES = {
  promedio: (a, b) => Number(b.promedio ?? -1) - Number(a.promedio ?? -1),
  peor: (a, b) => Number(a.promedio ?? 99) - Number(b.promedio ?? 99),
  cobertura: (a, b) => Number(b.cobertura) - Number(a.cobertura),
  nombre: (a, b) => a.programa.localeCompare(b.programa, 'es'),
}

function porcentaje(valor) {
  return `${Number(valor).toLocaleString('es-CO')} %`
}

/** Con `onCategoria`, cada categoría de la leyenda es un botón que filtra los programas. */
function Distribucion({ fila, categoria, onCategoria }) {
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
      <ul className={`panel-programas__conteos${onCategoria ? ' panel-programas__conteos--filtros' : ''}`}>
        {fila.distribucion.map((c) => (
          <li key={c.codigo}>
            {onCategoria ? (
              <button
                type="button"
                className="panel-programas__categoria"
                aria-pressed={categoria === c.codigo}
                onClick={() => onCategoria(categoria === c.codigo ? '' : c.codigo)}
              >
                <span
                  className={`panel-programas__punto distribucion__segmento--${CLASE[c.codigo]}`}
                  aria-hidden="true"
                />
                {c.etiqueta}: <strong>{c.cantidad}</strong> ({porcentaje(c.porcentaje)})
              </button>
            ) : (
              <>
                {c.etiqueta}: <strong>{c.cantidad}</strong> ({porcentaje(c.porcentaje)})
              </>
            )}
          </li>
        ))}
      </ul>
    </>
  )
}

function Fila({ fila, categoria, onCategoria }) {
  return (
    <>
      <p className="panel-programas__datos">
        Cobertura <strong>{porcentaje(fila.cobertura)}</strong> ({fila.evaluados} de {fila.docentes}) · Promedio{' '}
        <strong>{formatearNota(fila.promedio)}</strong>
      </p>
      <Distribucion fila={fila} categoria={categoria} onCategoria={onCategoria} />
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

/**
 * Panel I11: solo agregados. No enlaza a ningún expediente individual; sí a las estadísticas del
 * programa (coordinación y vicerrectoría). Se busca, se ordena y se filtra por categoría tocando la
 * leyenda del total.
 */
export default function PanelProgramas() {
  const { usuario } = useSesion()
  // Visualización lee el panel de toda la universidad, como vicerrectoría.
  const esVisualizador = usuario.rol === ROLES.VISUALIZADOR
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR || esVisualizador
  const [filtros, actualizarFiltros] = useFiltrosUrl({
    periodo: '',
    facultad: '',
    busqueda: '',
    orden: 'promedio',
    categoria: '',
  })
  const { periodo: periodoId, facultad: facultadId, busqueda, orden, categoria } = filtros
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => obtenerPanelProgramas({ periodoId, facultadId }), [periodoId, facultadId])
  const panel = useFetch(cargar, [cargar])

  const programas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    const enCategoria = (fila) => (fila.distribucion.find((c) => c.codigo === categoria)?.cantidad ?? 0) > 0
    return (panel.datos?.programas ?? [])
      .filter((fila) => (!termino || normalizarTexto(fila.programa).includes(termino)) && (!categoria || enCategoria(fila)))
      .sort(ORDENES[orden] ?? ORDENES.promedio)
  }, [panel.datos, busqueda, orden, categoria])
  const etiquetaCategoria = NIVELES.find((n) => n.codigo === categoria)?.etiqueta
  const paginacionProgramas = usePaginacion(programas, {
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
          <Campo id="buscarPrograma" etiqueta="Buscar programa">
            <span className="panel-programas__buscador">
              <Search size={16} aria-hidden="true" />
              <input
                id="buscarPrograma"
                className="campo__control"
                type="search"
                value={busqueda}
                placeholder="Nombre del programa"
                onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
              />
            </span>
          </Campo>
          <Campo id="ordenPanel" etiqueta="Ordenar">
            <select
              id="ordenPanel"
              className="campo__control"
              value={orden}
              onChange={(e) => actualizarFiltros({ orden: e.target.value })}
            >
              <option value="promedio">Mejor promedio primero</option>
              <option value="peor">Menor promedio primero</option>
              <option value="cobertura">Mayor cobertura</option>
              <option value="nombre">Nombre</option>
            </select>
          </Campo>
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
            <Panel
              titulo={`Total · ${panel.datos.periodo.nombre}`}
              descripcion="Toca una categoría para ver solo los programas que tienen docentes en ella."
              icono={ChartColumn}
            >
              <Fila
                fila={panel.datos.total}
                categoria={categoria}
                onCategoria={(codigo) => actualizarFiltros({ categoria: codigo })}
              />
            </Panel>
            {(categoria || busqueda) && (
              <p className="panel-programas__activo" role="status">
                {programas.length} de {panel.datos.programas.length} programas
                {etiquetaCategoria ? ` con docentes en «${etiquetaCategoria}»` : ''}
                {busqueda ? ` que coinciden con «${busqueda}»` : ''}.
                <Boton variante="contorno" icono={X} onClick={() => actualizarFiltros({ categoria: '', busqueda: '' })}>
                  Quitar filtros
                </Boton>
              </p>
            )}
            {panel.datos.programas.length === 0 && (
              <EstadoVista titulo="Sin programas" mensaje="No hay docentes activos en este alcance." />
            )}
            {panel.datos.programas.length > 0 && programas.length === 0 && (
              <EstadoVista titulo="Ningún programa coincide" mensaje="Cambia la búsqueda o quita el filtro de categoría." />
            )}
            <div className="rejilla">
              {paginacionProgramas.visibles.map((fila) => (
                <Panel key={fila.idPrograma} titulo={fila.programa} className="panel-programas__programa">
                  <Fila fila={fila} />
                  {!esVisualizador && (
                    <Link
                      className="panel-programas__ver"
                      to={`/estadisticas?programa=${encodeURIComponent(fila.programa)}`}
                    >
                      <ChartColumnBig size={15} aria-hidden="true" /> Ver estadísticas del programa
                    </Link>
                  )}
                </Panel>
              ))}
            </div>
            <Paginacion {...paginacionProgramas} etiqueta="programas del panel" />
            {/* Visualización solo ve agregados: sin coevaluaciones pendientes ni cobertura detallada. */}
            {esVicerrector && !esVisualizador && <Participacion />}
            {esVicerrector && !esVisualizador && <CoberturaEstudiantes />}
          </>
        )}
      </div>
    </>
  )
}
