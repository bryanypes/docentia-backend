import { ClipboardList, RefreshCw, Search, SlidersHorizontal, X } from 'lucide-react'
import { useMemo } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { normalizarTexto } from '../../../shared/utils/formatters.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import '../components/FormularioInstrumento.css'

const TODOS = ''
const FILTROS_PREDETERMINADOS = { busqueda: '', programa: '', estado: '' }

const ESTADO = {
  SIN_INICIAR: { etiqueta: 'Sin iniciar', clase: 'neutro', accion: 'editar', texto: 'Responder' },
  BORRADOR: { etiqueta: 'En borrador', clase: 'info', accion: 'editar', texto: 'Continuar' },
  ENVIADA: { etiqueta: 'Realizada', clase: 'exito', accion: 'ver', texto: 'Ver coevaluación' },
}

export default function Coevaluaciones() {
  const { datos, error, cargando, recargar } = useFetch(obtenerPendientesCoevaluacion)
  const [filtros, actualizarFiltros] = useFiltrosUrl(FILTROS_PREDETERMINADOS)
  const { busqueda, programa, estado } = filtros

  const docentes = useMemo(() => datos?.docentes ?? [], [datos])
  const programas = useMemo(() => [...new Set(docentes.map((d) => d.programa))].sort(), [docentes])
  const filas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return docentes.filter(
      (d) =>
        (!programa || d.programa === programa) &&
        (!estado || d.estado === estado) &&
        (!termino || normalizarTexto(`${d.nombreDocente} ${d.programa}`).includes(termino)),
    )
  }, [docentes, programa, estado, busqueda])
  const paginacion = usePaginacion(filas)
  const hayFiltros = Boolean(busqueda || programa || estado)

  function limpiarFiltros() {
    actualizarFiltros({ busqueda: TODOS, programa: TODOS, estado: TODOS })
  }

  return (
    <>
      <Encabezado
        miga="Coordinación · Coevaluaciones"
        titulo="Coevaluaciones"
        subtitulo={datos ? `Periodo ${datos.periodo.nombre}. ${datos.pendientes} pendientes.` : 'Docentes de tu facultad.'}
      />
      <div className="pagina">
        {cargando && !datos && <Cargando texto="Cargando coevaluaciones" bloques={1} />}
        {error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar las coevaluaciones" mensaje={error.message}>
            <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!cargando && !error && !datos && (
          <EstadoVista titulo="No hay un periodo activo" mensaje="La vicerrectoría debe activar uno." />
        )}
        {datos?.docentes.length === 0 && (
          <EstadoVista titulo="Sin docentes" mensaje="Tu facultad no tiene docentes activos." />
        )}
        {datos?.docentes.length > 0 && (
          <Panel titulo="Docentes de la facultad" icono={ClipboardList}>
            <div className="coevaluaciones__filtros" role="search" aria-label="Filtrar coevaluaciones">
              <span className="coevaluaciones__filtros-rotulo">
                <SlidersHorizontal size={16} aria-hidden="true" />
                Filtrar
              </span>
              <label className="coevaluaciones__buscador">
                <Search size={16} aria-hidden="true" />
                <span className="solo-lectores">Buscar docente</span>
                <input
                  type="search"
                  value={busqueda}
                  onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
                  placeholder="Buscar docente o programa"
                />
              </label>
              <label className="coevaluaciones__selector">
                <span>Programa</span>
                <select value={programa} onChange={(e) => actualizarFiltros({ programa: e.target.value })}>
                  <option value={TODOS}>Todos</option>
                  {programas.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </label>
              <label className="coevaluaciones__selector">
                <span>Estado</span>
                <select value={estado} onChange={(e) => actualizarFiltros({ estado: e.target.value })}>
                  <option value={TODOS}>Todos</option>
                  {Object.entries(ESTADO).map(([clave, { etiqueta }]) => (
                    <option key={clave} value={clave}>
                      {etiqueta}
                    </option>
                  ))}
                </select>
              </label>
              {hayFiltros && (
                <Boton variante="enlace" icono={X} onClick={limpiarFiltros}>
                  Limpiar filtros
                </Boton>
              )}
            </div>
            <p className="coevaluaciones__conteo" aria-live="polite">
              <strong>{filas.length}</strong> de {docentes.length} docentes
            </p>
            {filas.length === 0 ? (
              <EstadoVista titulo="Ningún docente coincide con el filtro" mensaje="Prueba con otro filtro o limpia la búsqueda." />
            ) : (
            <div className="coevaluaciones__tabla">
              <table className="tabla tabla--apilable">
                <caption className="solo-lectores">Coevaluaciones por docente</caption>
                <thead>
                  <tr>
                    <th scope="col">Docente</th>
                    <th scope="col">Programa</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Avance</th>
                    <th scope="col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {paginacion.visibles.map((d) => (
                    <tr key={d.idDocente}>
                      <td className="tabla__fuerte">{d.nombreDocente}</td>
                      <td data-etiqueta="Programa">{d.programa}</td>
                      <td data-etiqueta="Estado">
                        <span className={`pildora pildora--${ESTADO[d.estado].clase}`}>{ESTADO[d.estado].etiqueta}</span>
                      </td>
                      <td data-etiqueta="Avance">
                        <span className="coevaluaciones__avance">
                          <span className="coevaluaciones__riel" aria-hidden="true">
                            <span style={{ width: `${(d.respondidos / datos.itemsCoevaluacion) * 100}%` }} />
                          </span>
                          {d.respondidos} de {datos.itemsCoevaluacion}
                        </span>
                      </td>
                      <td data-etiqueta="Acciones">
                        <div className="acciones-fila">
                          <BotonAccion
                            tipo={ESTADO[d.estado].accion}
                            texto={ESTADO[d.estado].texto}
                            nombre={d.nombreDocente}
                            a={`/coevaluaciones/${d.idDocente}`}
                          />
                          <BotonAccion
                            tipo="ver"
                            texto="Ver expediente"
                            nombre={d.nombreDocente}
                            a={`/docentes/${d.idDocente}`}
                          />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            )}
            {filas.length > 0 && <Paginacion {...paginacion} etiqueta="coevaluaciones" />}
          </Panel>
        )}
      </div>
    </>
  )
}
