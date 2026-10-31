import { ArrowRight } from 'lucide-react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_FUENTE } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora, formatearNota } from '../../../shared/utils/formatters.js'
import { listarCorrecciones } from '../services/calificacionesService.js'
import './PanelCorrecciones.css'

/** El promedio del historial solo trae valor si estaba completo. */
function textoPromedio(promedio) {
  return promedio?.estado === 'COMPLETO' ? formatearNota(promedio.valor) : 'Pendiente'
}

/**
 * Historial de correcciones de notas del docente (todas las fuentes y periodos).
 * Solo para coordinación y vicerrectoría: el docente nunca ve notas por fuente.
 */
export function PanelCorrecciones({ idDocente }) {
  const correcciones = useFetch(() => listarCorrecciones(idDocente), [idDocente])

  return (
    <Panel
      titulo="Historial de correcciones"
      descripcion="Cada cambio de una nota ya registrada, con el promedio de antes y de después."
    >
      {correcciones.cargando && <Cargando texto="Cargando historial de correcciones" bloques={1} />}

      {correcciones.error && (
        <div className="correcciones__error" role="alert">
          <p>{correcciones.error.message}</p>
          <Boton variante="secundario" onClick={correcciones.recargar}>
            Intentar de nuevo
          </Boton>
        </div>
      )}

      {correcciones.datos?.length === 0 && (
        <p className="expediente__vacio">Ninguna nota de este docente ha sido corregida.</p>
      )}

      {correcciones.datos?.length > 0 && (
        <ol className="correcciones">
          {correcciones.datos.map((c) => (
            <li key={c.idCorreccion} className="correcciones__item">
              <div className="correcciones__cabecera">
                <span className="pildora pildora--info">{ETIQUETA_FUENTE[c.fuente] ?? c.fuente}</span>
                <span className="correcciones__periodo">Periodo {c.nombrePeriodo}</span>
                <time className="correcciones__fecha" dateTime={c.fechaCorreccion}>
                  {formatearFechaHora(c.fechaCorreccion)}
                </time>
              </div>

              <dl className="correcciones__cambios">
                <div>
                  <dt className="rotulo">Nota de la fuente</dt>
                  <dd>
                    {formatearNota(c.notaAnterior)} <ArrowRight size={14} aria-hidden="true" className="correcciones__flecha" />
                    <span className="solo-lectores">pasó a</span>{' '}
                    <strong>{formatearNota(c.notaNueva)}</strong>
                  </dd>
                </div>
                <div>
                  <dt className="rotulo">Promedio ponderado</dt>
                  <dd>
                    {textoPromedio(c.promedioAnterior)} <ArrowRight size={14} aria-hidden="true" className="correcciones__flecha" />
                    <span className="solo-lectores">pasó a</span>{' '}
                    <strong>{textoPromedio(c.promedioNuevo)}</strong>
                  </dd>
                </div>
              </dl>

              <p className="correcciones__motivo">{c.motivo || 'Sin motivo registrado.'}</p>
              <p className="correcciones__autor">{c.nombreUsuario ?? 'Proceso del sistema'}</p>
            </li>
          ))}
        </ol>
      )}
    </Panel>
  )
}
