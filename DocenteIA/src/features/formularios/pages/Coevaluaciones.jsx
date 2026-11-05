import { ClipboardList, RefreshCw } from 'lucide-react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import '../components/FormularioInstrumento.css'

const ESTADO = {
  SIN_INICIAR: { etiqueta: 'Sin iniciar', clase: 'neutro', accion: 'editar', texto: 'Responder' },
  BORRADOR: { etiqueta: 'En borrador', clase: 'info', accion: 'editar', texto: 'Continuar' },
  ENVIADA: { etiqueta: 'Enviada', clase: 'exito', accion: 'ver', texto: 'Ver coevaluación' },
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
              <table className="tabla tabla--apilable">
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
                  {datos.docentes.map((d) => (
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
          </Panel>
        )}
      </div>
    </>
  )
}
