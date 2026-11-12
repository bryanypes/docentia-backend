import { RefreshCw, ScrollText } from 'lucide-react'
import { useCallback, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../../services/api.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { TAMANOS, useFiltrosUrl } from '../../../shared/hooks/usePaginacion.js'
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

const FILTROS_PREDETERMINADOS = { desde: '', hasta: '', accion: '', usuario: '' }

function consultar({ desde, hasta, accion, usuario, pagina, tamano }) {
  const consulta = new URLSearchParams({ page: pagina, size: tamano })
  if (desde) consulta.set('desde', desde)
  if (hasta) consulta.set('hasta', hasta)
  if (accion) consulta.set('accion', accion)
  if (usuario) consulta.set('usuario', usuario)
  return api.get(`/auditoria?${consulta}`)
}

/** Vista de solo lectura para Vicerrectoría. */
export default function Auditoria() {
  const [filtros, actualizarFiltros] = useFiltrosUrl(FILTROS_PREDETERMINADOS)
  const [parametros, setParametros] = useSearchParams()
  const pagina = Math.max(0, Number.parseInt(parametros.get('pagina') ?? '1', 10) - 1 || 0)
  const tamanoSolicitado = Number(parametros.get('tamano'))
  const tamano = TAMANOS.includes(tamanoSolicitado) ? tamanoSolicitado : TAMANOS[0]
  const filtrosClave = JSON.stringify(filtros)
  const cargar = useCallback(
    () => consultar({ ...JSON.parse(filtrosClave), pagina, tamano }),
    [filtrosClave, pagina, tamano],
  )
  const registros = useFetch(cargar, [cargar])
  const datos = registros.datos
  const paginaActual = Math.min(pagina, Math.max(0, (datos?.totalPaginas ?? 1) - 1))

  const actualizarPagina = useCallback((cambios) => {
    setParametros(
      (actuales) => {
        const nuevos = new URLSearchParams(actuales)
        if ('pagina' in cambios) {
          if (cambios.pagina == null || cambios.pagina === 1) nuevos.delete('pagina')
          else nuevos.set('pagina', String(cambios.pagina))
        }
        if ('tamano' in cambios) {
          if (cambios.tamano == null || cambios.tamano === TAMANOS[0]) nuevos.delete('tamano')
          else nuevos.set('tamano', String(cambios.tamano))
        }
        return nuevos
      },
      { replace: true },
    )
  }, [setParametros])

  useEffect(() => {
    if (!datos) return
    const paginaEnUrl = parametros.get('pagina')
    if (pagina !== paginaActual || (paginaEnUrl && Number(paginaEnUrl) !== paginaActual + 1)) {
      actualizarPagina({ pagina: paginaActual + 1 })
      return
    }
    if (parametros.has('tamano') && !TAMANOS.includes(tamanoSolicitado)) {
      actualizarPagina({ tamano: TAMANOS[0] })
    }
  }, [actualizarPagina, datos, pagina, paginaActual, parametros, tamanoSolicitado])

  function cambiar(campo, valor) {
    actualizarFiltros({ [campo]: valor })
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
            <Paginacion
              pagina={paginaActual + 1}
              paginas={Math.max(datos.totalPaginas, 1)}
              tamano={tamano}
              total={datos.totalElementos}
              desde={datos.totalElementos === 0 ? 0 : paginaActual * tamano + 1}
              hasta={Math.min(datos.totalElementos, (paginaActual + 1) * tamano)}
              irA={(siguiente) => actualizarPagina({ pagina: siguiente })}
              cambiarTamano={(siguiente) => actualizarPagina({ tamano: siguiente, pagina: null })}
              etiqueta="registros de auditoría"
            />
          </Panel>
        )}
      </div>
    </>
  )
}
