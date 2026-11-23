import { AlertTriangle, Lightbulb, RefreshCw, Search } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { ChipCategoria } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { formatearNota, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { textoUmbral, useUmbralRevision } from '../../kpis/hooks/useUmbralRevision.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarAccionesSugeridas } from '../services/actasService.js'
import '../components/PanelActa.css'


/** Solo informa lo que indica la Resolución 074: no notifica, no bloquea y no cambia nada. */
export default function AccionesSugeridas() {
  const { usuario } = useSesion()
  // Por debajo del umbral de revisión (4,0 por defecto) el desempeño queda «Para revisar».
  const umbral = useUmbralRevision()
  const umbralTexto = textoUmbral(umbral)
  const esBajo = useCallback((a) => a.promedio != null && Number(a.promedio) < umbral, [umbral])
  const [filtros, actualizarFiltros] = useFiltrosUrl({
    periodo: '',
    orden: 'mejor',
    busqueda: '',
    programa: '',
    categoria: '',
    desempeno: '',
  })
  const { periodo, orden, busqueda, programa, categoria, desempeno } = filtros
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => listarAccionesSugeridas(periodo), [periodo])
  const acciones = useFetch(cargar, [cargar])
  const todas = useMemo(() => acciones.datos ?? [], [acciones.datos])

  const programas = useMemo(
    () => [...new Set(todas.map((a) => a.programa).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'es')),
    [todas],
  )
  const categorias = useMemo(() => {
    const vistas = new Map()
    todas.forEach((a) => a.categoria && vistas.set(a.categoria.codigo, a.categoria.etiqueta))
    return [...vistas.entries()]
  }, [todas])
  const bajos = todas.filter(esBajo).length

  // Filtra y ordena: del mejor al peor promedio por defecto; se puede invertir.
  const ordenadas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    const signo = orden === 'peor' ? 1 : -1
    return todas
      .filter(
        (a) =>
          (!termino || normalizarTexto(a.nombreDocente).includes(termino)) &&
          (!programa || a.programa === programa) &&
          (!categoria || a.categoria?.codigo === categoria) &&
          (!desempeno || (desempeno === 'bajo' ? esBajo(a) : !esBajo(a))),
      )
      .sort(
        (a, b) =>
          signo * (Number(a.promedio ?? 0) - Number(b.promedio ?? 0)) ||
          a.nombreDocente.localeCompare(b.nombreDocente, 'es'),
      )
  }, [todas, busqueda, programa, categoria, desempeno, orden, esBajo])
  const paginacion = usePaginacion(ordenadas)
  const hayFiltros = Boolean(busqueda || programa || categoria || desempeno)

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Acciones sugeridas`}
        titulo="Acciones sugeridas"
        subtitulo="Lo que indica la Resolución 074 para cada categoría. Es informativo: la decisión es de la institución."
      />
      <div className="pagina">
        {bajos > 0 && (
          <button
            type="button"
            className={`acciones__alerta${desempeno === 'bajo' ? ' acciones__alerta--activa' : ''}`}
            aria-pressed={desempeno === 'bajo'}
            onClick={() => actualizarFiltros({ desempeno: desempeno === 'bajo' ? '' : 'bajo', orden: 'peor' })}
          >
            <AlertTriangle size={20} aria-hidden="true" />
            <span>
              <strong>
                {bajos} {bajos === 1 ? 'docente tiene' : 'docentes tienen'} un promedio por debajo de {umbralTexto}
              </strong>
              {desempeno === 'bajo' ? ' · Mostrando solo ellos. Pulsa para ver a todos.' : ' · Pulsa para revisarlos.'}
            </span>
          </button>
        )}

        <Panel titulo="Filtros" className="acciones__panel-filtros">
          <div className="acciones__filtros">
            <label className="acciones__buscador">
              <Search size={16} aria-hidden="true" />
              <span className="solo-lectores">Buscar docente</span>
              <input
                type="search"
                className="campo__control"
                placeholder="Buscar docente por nombre"
                value={busqueda}
                onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
              />
            </label>
            <Campo id="periodoAcciones" etiqueta="Periodo">
              <select
                id="periodoAcciones"
                className="campo__control"
                value={periodo}
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
            <Campo id="programaAcciones" etiqueta="Programa">
              <select
                id="programaAcciones"
                className="campo__control"
                value={programa}
                onChange={(e) => actualizarFiltros({ programa: e.target.value })}
              >
                <option value="">Todos</option>
                {programas.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="categoriaAcciones" etiqueta="Categoría">
              <select
                id="categoriaAcciones"
                className="campo__control"
                value={categoria}
                onChange={(e) => actualizarFiltros({ categoria: e.target.value })}
              >
                <option value="">Todas</option>
                {categorias.map(([codigo, etiqueta]) => (
                  <option key={codigo} value={codigo}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="desempenoAcciones" etiqueta="Desempeño">
              <select
                id="desempenoAcciones"
                className="campo__control"
                value={desempeno}
                onChange={(e) => actualizarFiltros({ desempeno: e.target.value })}
              >
                <option value="">Todos</option>
                <option value="bajo">Para revisar: menos de {umbralTexto}</option>
                <option value="aceptable">{umbralTexto} o más</option>
              </select>
            </Campo>
            <Campo id="ordenAcciones" etiqueta="Ordenar por promedio">
              <select
                id="ordenAcciones"
                className="campo__control"
                value={orden}
                onChange={(e) => actualizarFiltros({ orden: e.target.value })}
              >
                <option value="mejor">Del mejor al peor</option>
                <option value="peor">Del peor al mejor</option>
              </select>
            </Campo>
          </div>
          {hayFiltros && (
            <Boton
              variante="enlace"
              onClick={() => actualizarFiltros({ busqueda: '', programa: '', categoria: '', desempeno: '' })}
            >
              Quitar filtros
            </Boton>
          )}
        </Panel>

        {acciones.cargando && !acciones.datos && <Cargando texto="Cargando acciones" bloques={1} />}
        {acciones.error && (
          <EstadoVista
            tipo={acciones.error.status === 409 ? 'vacio' : 'error'}
            titulo={acciones.error.status === 409 ? 'No hay un periodo activo' : 'No fue posible cargar las acciones'}
            mensaje={acciones.error.message}
          >
            <Boton icono={RefreshCw} onClick={acciones.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {acciones.datos?.length === 0 && (
          <EstadoVista titulo="Sin docentes clasificados" mensaje="Aún no hay promedios en este periodo." />
        )}
        {acciones.datos?.length > 0 && (
          <Panel
            titulo="Por docente"
            descripcion={`${ordenadas.length} de ${todas.length} docentes`}
            icono={Lightbulb}
          >
            {ordenadas.length === 0 ? (
              <p className="campo__ayuda">Ningún docente coincide con los filtros.</p>
            ) : (
              <>
                <div className="acciones__tabla">
                  <table className="tabla tabla--apilable acciones__lista">
                    <thead>
                      <tr>
                        <th scope="col">Docente</th>
                        <th scope="col">Programa</th>
                        <th
                          scope="col"
                          className="tabla__numero"
                          aria-sort={orden === 'peor' ? 'ascending' : 'descending'}
                        >
                          Promedio
                        </th>
                        <th scope="col">Categoría</th>
                        <th scope="col">Acción</th>
                      </tr>
                    </thead>
                    <tbody>
                      {paginacion.visibles.map((a) => (
                        <tr key={a.idDocente} className={esBajo(a) ? 'acciones__fila--baja' : undefined}>
                          <td className="acciones__docente">
                            <Link to={`/docentes/${a.idDocente}`}>{a.nombreDocente}</Link>
                          </td>
                          <td data-etiqueta="Programa">{a.programa}</td>
                          <td data-etiqueta="Promedio" className="tabla__numero tabla__fuerte">
                            {formatearNota(a.promedio)}
                            {esBajo(a) && <span className="acciones__revisar">Para revisar</span>}
                          </td>
                          <td data-etiqueta="Categoría">
                            <ChipCategoria
                              categoria={a.categoria}
                              sufijo={a.estadoPromedio === 'PARCIAL' ? ' · parcial' : ''}
                            />
                          </td>
                          <td data-etiqueta="Acción" className="acciones__accion">
                            {a.accion}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <Paginacion {...paginacion} etiqueta="acciones sugeridas" />
              </>
            )}
          </Panel>
        )}
      </div>
    </>
  )
}
