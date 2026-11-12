import { RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_FUENTE } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota } from '../../../shared/utils/formatters.js'
import { obtenerComparativoDocente, obtenerComparativoPropio } from '../services/expedientesService.js'
import './PanelComparativoPrograma.css'

function anchoBarra(nota) {
  if (nota === null || nota === undefined) return '0%'
  return `${Math.max(0, Math.min(5, Number(nota)) / 5 * 100)}%`
}

function Serie({ nombre, nota, tipo }) {
  return (
    <div className="comparativo-programa__serie">
      <span>{nombre}</span>
      <div className="comparativo-programa__pista" aria-hidden="true">
        {nota !== null && nota !== undefined && (
          <div
            className={`comparativo-programa__barra comparativo-programa__barra--${tipo}`}
            style={{ width: anchoBarra(nota) }}
          />
        )}
      </div>
      <strong>{formatearNota(nota)}</strong>
    </div>
  )
}

export function PanelComparativoPrograma({ idDocente, propio = false }) {
  const cargar = useCallback(() => (
    propio ? obtenerComparativoPropio() : obtenerComparativoDocente(idDocente)
  ), [idDocente, propio])
  const comparativo = useFetch(cargar, [cargar])

  if (comparativo.cargando && !comparativo.datos) {
    return (
      <Panel titulo="Comparativo por fuente">
        <Cargando texto="Cargando el comparativo" bloques={1} />
      </Panel>
    )
  }

  if (comparativo.error) {
    return (
      <Panel titulo="Comparativo por fuente">
        <EstadoVista tipo="error" titulo="No fue posible cargar el comparativo" mensaje={comparativo.error.message}>
          <Boton icono={RefreshCw} onClick={comparativo.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      </Panel>
    )
  }

  const { fuentes, nombrePrograma } = comparativo.datos
  const hayDatos = fuentes.some((fuente) => fuente.notaDocente !== null || fuente.promedioPrograma !== null)

  return (
    <Panel
      titulo="Comparativo por fuente"
      descripcion={`Tus notas frente al promedio real del programa ${nombrePrograma}.`}
    >
      {!hayDatos ? (
        <p className="comparativo-programa__vacio">Aún no hay calificaciones comparables en este periodo.</p>
      ) : (
        <>
          <div className="comparativo-programa__leyenda" aria-hidden="true">
            <span><i className="comparativo-programa__muestra comparativo-programa__muestra--docente" />Docente</span>
            <span><i className="comparativo-programa__muestra comparativo-programa__muestra--programa" />Promedio del programa</span>
          </div>
          <div className="comparativo-programa__grafico">
            {fuentes.map((fuente) => (
              <section key={fuente.fuente} className="comparativo-programa__fuente">
                <h3>{ETIQUETA_FUENTE[fuente.fuente] ?? fuente.fuente}</h3>
                <Serie nombre="Docente" nota={fuente.notaDocente} tipo="docente" />
                <Serie nombre="Programa" nota={fuente.promedioPrograma} tipo="programa" />
              </section>
            ))}
          </div>
          <table className="solo-lectores">
            <caption>Comparativo de notas por fuente en {nombrePrograma}</caption>
            <thead><tr><th>Fuente</th><th>Docente</th><th>Promedio del programa</th></tr></thead>
            <tbody>
              {fuentes.map((fuente) => (
                <tr key={fuente.fuente}>
                  <th scope="row">{ETIQUETA_FUENTE[fuente.fuente] ?? fuente.fuente}</th>
                  <td>{formatearNota(fuente.notaDocente)}</td>
                  <td>{formatearNota(fuente.promedioPrograma)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </Panel>
  )
}
