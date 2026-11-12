import { Lightbulb, RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
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
import { useSesion } from '../../auth/sesionContext.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarAccionesSugeridas } from '../services/actasService.js'
import '../components/PanelActa.css'

/** Solo informa lo que indica la Resolución 074: no notifica, no bloquea y no cambia nada. */
export default function AccionesSugeridas() {
  const { usuario } = useSesion()
  const [filtros, actualizarFiltros] = useFiltrosUrl({ periodo: '' })
  const periodo = filtros.periodo
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => listarAccionesSugeridas(periodo), [periodo])
  const acciones = useFetch(cargar, [cargar])
  const paginacion = usePaginacion(acciones.datos ?? [])

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Acciones sugeridas`}
        titulo="Acciones sugeridas"
        subtitulo="Lo que indica la Resolución 074 para cada categoría. Es informativo: la decisión es de la institución."
      />
      <div className="pagina">
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
