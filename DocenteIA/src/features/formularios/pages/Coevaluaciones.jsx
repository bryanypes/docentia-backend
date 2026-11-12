import { ClipboardList, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import '../components/FormularioInstrumento.css'

const ESTADO = {
  SIN_INICIAR: { etiqueta: 'Sin iniciar', clase: 'neutro' },
  BORRADOR: { etiqueta: 'En borrador', clase: 'info' },
  ENVIADA: { etiqueta: 'Enviada', clase: 'exito' },
}

export default function Coevaluaciones() {
  const { datos, error, cargando, recargar } = useFetch(obtenerPendientesCoevaluacion)

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
            <div className="coevaluaciones__tabla">
              <table className="tabla">
                <thead>
                  <tr>
                    <th scope="col">Docente</th>
                    <th scope="col">Programa</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Avance</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.docentes.map((d) => (
                    <tr key={d.idDocente}>
                      <td>
                        <Link to={`/coevaluaciones/${d.idDocente}`}>{d.nombreDocente}</Link>
                      </td>
                      <td>{d.programa}</td>
                      <td>
                        <span className={`pildora pildora--${ESTADO[d.estado].clase}`}>{ESTADO[d.estado].etiqueta}</span>
                      </td>
                      <td>
                        {d.respondidos} de {datos.itemsCoevaluacion}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        )}
      </div>
    </>
  )
}
