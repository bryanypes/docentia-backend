import { RefreshCw, ScrollText } from 'lucide-react'
import { useCallback, useState } from 'react'
import { api } from '../../../services/api.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import '../../actas/components/PanelActa.css'
import '../../kpis/pages/PanelProgramas.css'
import '../../notificaciones/components/Notificaciones.css'

const ACCIONES = {
  LOGIN: 'Inicio de sesión',
  FIRMA_ACTA: 'Firma de acta',
  CORRECCION_ACTA: 'Corrección de acta',
  LECTURA_ACTA_FIRMADA: 'Lectura de acta firmada',
  CAMBIO_PESOS: 'Cambio de pesos',
  IMPORTACION_CSV: 'Importación CSV',
  PUBLICACION_HALLAZGOS: 'Publicación de hallazgos',
  DESCARTE_HALLAZGO: 'Descarte de hallazgo',
  CAMBIO_VENTANA: 'Cambio de ventana',
  ACTIVACION_PERIODO: 'Activación de periodo',
  DESACTIVACION_PERIODO: 'Desactivación de periodo',
  ENVIO_FORMULARIO: 'Envío de formulario',
  ENVIO_HETEROEVALUACION: 'Envío de heteroevaluación',
  IMPORTACION_MATRICULA: 'Importación de matrícula',
}

function consultar({ desde, hasta, accion, usuario, pagina }) {
  const consulta = new URLSearchParams({ page: pagina, size: 20 })
  if (desde) consulta.set('desde', desde)
  if (hasta) consulta.set('hasta', hasta)
  if (accion) consulta.set('accion', accion)
  if (usuario) consulta.set('usuario', usuario)
  return api.get(`/auditoria?${consulta}`)
}

/** Vista de solo lectura para Vicerrectoría. */
export default function Auditoria() {
  const [filtros, setFiltros] = useState({ desde: '', hasta: '', accion: '', usuario: '' })
  const [pagina, setPagina] = useState(0)
  const cargar = useCallback(() => consultar({ ...filtros, pagina }), [filtros, pagina])
  const registros = useFetch(cargar, [cargar])
  const datos = registros.datos

  function cambiar(campo, valor) {
    setPagina(0)
    setFiltros((actuales) => ({ ...actuales, [campo]: valor }))
  }

  return (
    <>
      <Encabezado miga="Vicerrectoría · Auditoría" titulo="Auditoría" subtitulo="Quién hizo qué y cuándo. Solo lectura." />
      <div className="pagina">
        <div className="panel-programas__filtros">
          <Campo id="auditoriaDesde" etiqueta="Desde">
            <input id="auditoriaDesde" type="date" className="campo__control" value={filtros.desde} onChange={(e) => cambiar('desde', e.target.value)} />
          </Campo>
          <Campo id="auditoriaHasta" etiqueta="Hasta">
            <input id="auditoriaHasta" type="date" className="campo__control" value={filtros.hasta} onChange={(e) => cambiar('hasta', e.target.value)} />
          </Campo>
          <Campo id="auditoriaAccion" etiqueta="Acción">
            <select id="auditoriaAccion" className="campo__control" value={filtros.accion} onChange={(e) => cambiar('accion', e.target.value)}>
              <option value="">Todas</option>
              {Object.entries(ACCIONES).map(([valor, nombre]) => (
                <option key={valor} value={valor}>
                  {nombre}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="auditoriaUsuario" etiqueta="Usuario">
            <input id="auditoriaUsuario" className="campo__control" maxLength={150} value={filtros.usuario} onChange={(e) => cambiar('usuario', e.target.value)} />
          </Campo>
        </div>
        {registros.cargando && !datos && <Cargando texto="Cargando auditoría" bloques={1} />}
        {registros.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar la auditoría" mensaje={registros.error.message}>
            <Boton icono={RefreshCw} onClick={registros.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {datos?.contenido.length === 0 && <EstadoVista titulo="Sin registros" mensaje="Ningún registro coincide con los filtros." />}
        {datos?.contenido.length > 0 && (
          <Panel titulo={`${datos.totalElementos} registros`} icono={ScrollText}>
            <div className="acciones__tabla">
              <table className="tabla tabla--apilable">
                <thead>
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Usuario</th>
                    <th scope="col">Acción</th>
                    <th scope="col">Entidad</th>
                    <th scope="col">IP</th>
                    <th scope="col">Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.contenido.map((r) => (
                    <tr key={r.id}>
                      <td>{formatearFechaHora(r.fecha)}</td>
                      <td data-etiqueta="Usuario">{r.correo}</td>
                      <td data-etiqueta="Acción">{ACCIONES[r.accion] ?? r.accion}</td>
                      <td data-etiqueta="Entidad">
                        {r.entidad} {r.entidadId}
                      </td>
                      <td data-etiqueta="IP">{r.ip}</td>
                      <td data-etiqueta="Resultado">{r.resultado === 'EXITO' ? 'Éxito' : 'Fallo'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="notificaciones__paginas">
              <Boton variante="contorno" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                Anterior
              </Boton>
              <span>
                Página {pagina + 1} de {Math.max(datos.totalPaginas, 1)}
              </span>
              <Boton variante="contorno" disabled={pagina + 1 >= datos.totalPaginas} onClick={() => setPagina((p) => p + 1)}>
                Siguiente
              </Boton>
            </div>
          </Panel>
        )}
      </div>
    </>
  )
}
