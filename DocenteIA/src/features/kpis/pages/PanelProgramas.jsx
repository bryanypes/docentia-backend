import { ChartColumn, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL, NIVELES, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { obtenerPendientesCoevaluacion } from '../../formularios/services/formulariosService.js'
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
  if (!datos?.programas.length) return null
  return (
    <Panel titulo="Participación en formularios" descripcion="Solo conteos, sin puntajes.">
      <div className="acciones__tabla">
        <table className="tabla">
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
            {datos.programas.map((p) => (
              <tr key={p.programa}>
                <td>{p.programa}</td>
                <td>{p.docentes}</td>
                <td>{p.enviadas}</td>
                <td>{p.borrador}</td>
                <td>{p.sinIniciar}</td>
                <td>{p.autoevaluacionesEnviadas}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Panel>
  )
}

/** Panel I11: solo agregados. A propósito no enlaza a ningún expediente individual. */
export default function PanelProgramas() {
  const { usuario } = useSesion()
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR
  const [periodoId, setPeriodoId] = useState('')
  const [facultadId, setFacultadId] = useState('')
  const periodos = useFetch(listarPeriodos)
  const facultades = useFetch(() => (esVicerrector ? listarFacultades() : Promise.resolve([])), [esVicerrector])
  const cargar = useCallback(() => obtenerPanelProgramas({ periodoId, facultadId }), [periodoId, facultadId])
  const panel = useFetch(cargar, [cargar])

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
              onChange={(e) => setPeriodoId(e.target.value)}
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
                onChange={(e) => setFacultadId(e.target.value)}
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
              {panel.datos.programas.map((fila) => (
                <Panel key={fila.idPrograma} titulo={fila.programa} descripcion={fila.facultad}>
                  <Fila fila={fila} />
                </Panel>
              ))}
            </div>
            {esVicerrector && <Participacion />}
          </>
        )}
      </div>
    </>
  )
}
