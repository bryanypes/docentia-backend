import { Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { NOMBRE_CAMPO } from '../mapeo.js'
import { eliminarPlantilla } from '../services/importacionesService.js'

/** Plantillas de mapeo del coordinador. `plantillas` es el resultado de useFetch. */
export function PanelPlantillas({ plantillas }) {
  const [eliminando, setEliminando] = useState(null)
  const [error, setError] = useState(null)

  async function eliminar(plantilla) {
    if (!window.confirm(`¿Eliminar la plantilla "${plantilla.nombre}"?`)) return
    setEliminando(plantilla.idPlantilla)
    setError(null)
    try {
      await eliminarPlantilla(plantilla.idPlantilla)
      plantillas.recargar()
    } catch (e) {
      setError(e)
    } finally {
      setEliminando(null)
    }
  }

  return (
    <Panel
      titulo="Plantillas de mapeo"
      descripcion="Si un archivo trae exactamente las mismas columnas que una plantilla, se aplica sola al validar."
    >
      {error && <Aviso tipo="error">{error.message}</Aviso>}
      {plantillas.error && <Aviso tipo="error">{plantillas.error.message}</Aviso>}

      {plantillas.datos?.length === 0 && (
        <p className="importacion__vacio">
          Aún no tienes plantillas. Después de validar un archivo puedes guardar su mapeo con un nombre.
        </p>
      )}

      {plantillas.datos?.length > 0 && (
        <ul className="plantillas">
          {plantillas.datos.map((p) => (
            <li key={p.idPlantilla} className="plantillas__item">
              <div className="plantillas__textos">
                <strong>{p.nombre}</strong>
                <span className="tabla__tenue">
                  {p.columnas.length} columnas · actualizada {formatearFechaHora(p.fechaActualizacion)}
                </span>
                <details className="plantillas__detalle">
                  <summary>Ver mapeo</summary>
                  <ul>
                    {Object.entries(p.mapeo).map(([columna, campo]) => (
                      <li key={columna}>
                        <code>{columna}</code> → {NOMBRE_CAMPO[campo] ?? campo}
                      </li>
                    ))}
                  </ul>
                </details>
              </div>
              <Boton
                variante="secundario"
                icono={Trash2}
                onClick={() => eliminar(p)}
                disabled={eliminando === p.idPlantilla}
                aria-label={`Eliminar la plantilla ${p.nombre}`}
              >
                {eliminando === p.idPlantilla ? 'Eliminando…' : 'Eliminar'}
              </Boton>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}
