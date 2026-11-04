import { useTituloDocumento } from '../../../hooks/useTituloDocumento.js'
import { usePeriodoActivo } from '../AppLayout/periodoContext.js'
import './Encabezado.css'

/**
 * Encabezado de página del maquetado: miga, título, subtítulo y periodo activo. `ayuda` va junto al
 * título (por ejemplo, un botón de información).
 */
export function Encabezado({ miga, titulo, subtitulo, ayuda, children }) {
  useTituloDocumento(titulo)
  const { periodo, cargando } = usePeriodoActivo()
  const nombrePeriodo = cargando ? '…' : (periodo?.nombre ?? 'Sin periodo activo')

  return (
    <header className="encabezado">
      <div className="encabezado__textos">
        {miga && <div className="encabezado__miga">{miga}</div>}
        {ayuda ? (
          <div className="encabezado__fila-titulo">
            <h1 className="encabezado__titulo">{titulo}</h1>
            {ayuda}
          </div>
        ) : (
          <h1 className="encabezado__titulo">{titulo}</h1>
        )}
        {subtitulo && <p className="encabezado__subtitulo">{subtitulo}</p>}
      </div>
      <div className="encabezado__acciones">
        <div className="encabezado__periodo">
          <span>Periodo</span>
          <strong>{nombrePeriodo}</strong>
        </div>
        {children}
      </div>
    </header>
  )
}
