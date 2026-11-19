import { Lightbulb, RefreshCw } from 'lucide-react'
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
import { formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarAccionesSugeridas } from '../services/actasService.js'
import '../components/PanelActa.css'

/** Solo informa lo que indica la Resolución 074: no notifica, no bloquea y no cambia nada. */
export default function AccionesSugeridas() {
  const { usuario } = useSesion()
  const [filtros, actualizarFiltros] = useFiltrosUrl({ periodo: '', orden: 'mejor' })
  const { periodo, orden } = filtros
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => listarAccionesSugeridas(periodo), [periodo])
  const acciones = useFetch(cargar, [cargar])
  // Del mejor al peor promedio por defecto; se puede invertir.
  const ordenadas = useMemo(() => {
    const signo = orden === 'peor' ? 1 : -1
    return [...(acciones.datos ?? [])].sort(
      (a, b) => signo * (Number(a.promedio ?? 0) - Number(b.promedio ?? 0)) || a.nombreDocente.localeCompare(b.nombreDocente, 'es'),
    )
  }, [acciones.datos, orden])
  const paginacion = usePaginacion(ordenadas)

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Acciones sugeridas`}
        titulo="Acciones sugeridas"
        subtitulo="Lo que indica la Resolución 074 para cada categoría. Es informativo: la decisión es de la institución."
      />
      <div className="pagina">
        <div className="acciones__filtros">
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
          <Panel titulo="Por docente" icono={Lightbulb}>
            <div className="acciones__tabla">
              <table className="tabla">
                <thead>
                  <tr>
                    <th scope="col">Docente</th>
                    <th scope="col">Programa</th>
                    <th scope="col" className="tabla__numero" aria-sort={orden === 'peor' ? 'ascending' : 'descending'}>
                      Promedio
                    </th>
                    <th scope="col">Categoría</th>
                    <th scope="col">Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {paginacion.visibles.map((a) => (
                    <tr key={a.idDocente}>
                      <td>
                        <Link to={`/docentes/${a.idDocente}`}>{a.nombreDocente}</Link>
                      </td>
                      <td>{a.programa}</td>
                      <td className="tabla__numero tabla__fuerte">{formatearNota(a.promedio)}</td>
                      <td>
                        <ChipCategoria
                          categoria={a.categoria}
                          sufijo={a.estadoPromedio === 'PARCIAL' ? ' · parcial' : ''}
                        />
                      </td>
                      <td>{a.accion}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Paginacion {...paginacion} etiqueta="acciones sugeridas" />
          </Panel>
        )}
      </div>
    </>
  )
}
