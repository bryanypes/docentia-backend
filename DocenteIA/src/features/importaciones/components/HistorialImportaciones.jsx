import { RefreshCw } from 'lucide-react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { formatearEntero, formatearFechaHora } from '../../../shared/utils/formatters.js'

/** Tabla de cargas del CSV, de la más reciente a la más antigua. `importaciones` es el resultado de useFetch. */
export function HistorialImportaciones({ importaciones, descripcion, mostrarUsuario }) {
  return (
    <Panel titulo="Historial de cargas" descripcion={descripcion}>
      {importaciones.cargando && !importaciones.datos && <Cargando texto="Cargando historial" bloques={1} />}

      {importaciones.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar el historial" mensaje={importaciones.error.message}>
          <Boton icono={RefreshCw} onClick={importaciones.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      )}

      {importaciones.datos?.length === 0 && (
        <p className="importacion__vacio">Todavía no se ha cargado ningún archivo de evaluación de estudiantes.</p>
      )}

      {importaciones.datos?.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla importacion__historial">
            <caption className="solo-lectores">Historial de cargas del CSV</caption>
            <thead>
              <tr>
                <th scope="col">Carga</th>
                {mostrarUsuario && <th scope="col">Cargado por</th>}
                <th scope="col">Resultado</th>
                <th scope="col" className="tabla__numero">
                  Filas
                </th>
                <th scope="col" className="tabla__numero">
                  Docentes
                </th>
                <th scope="col">Notas</th>
                <th scope="col">Comentarios</th>
              </tr>
            </thead>
            <tbody>
              {importaciones.datos.map((i) => (
                <tr key={i.idImportacion}>
                  <td>
                    <span className="tabla__fuerte">
                      #{i.idImportacion} · {i.nombreArchivo}
                    </span>
                    <span className="tabla__tenue">
                      {formatearFechaHora(i.fechaImportacion)} · periodo {i.nombrePeriodo}
                    </span>
                  </td>
                  {mostrarUsuario && <td>{i.nombreUsuario}</td>}
                  <td>
                    {i.resultado === 'EXITOSA' ? (
                      <span className="pildora pildora--exito">Importada</span>
                    ) : (
                      <span className="pildora pildora--error">Rechazada</span>
                    )}
                  </td>
                  <td className="tabla__numero">
                    {formatearEntero(i.filasProcesadas)}
                    {i.filasConError > 0 && (
                      <span className="tabla__tenue">{formatearEntero(i.filasConError)} con error</span>
                    )}
                  </td>
                  <td className="tabla__numero">{formatearEntero(i.docentesAfectados)}</td>
                  <td>
                    {i.resultado === 'EXITOSA'
                      ? `${i.notasNuevas} nuevas · ${i.notasCorregidas} corregidas · ${i.notasSinCambios} sin cambios`
                      : '—'}
                  </td>
                  <td>
                    {i.resultado === 'EXITOSA' ? `${i.comentariosNuevos} nuevos · ${i.comentariosRepetidos} repetidos` : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  )
}
