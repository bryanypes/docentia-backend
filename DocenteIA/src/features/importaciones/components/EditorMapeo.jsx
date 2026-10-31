import { ArrowRight } from 'lucide-react'
import { CAMPOS_IMPORTACION } from '../../../shared/constants/dominio.js'
import { NOMBRE_CAMPO, revisarMapeo } from '../mapeo.js'

/**
 * Una fila por columna del archivo con un selector del campo que contiene.
 * Las columnas sin campo se ignoran al importar.
 */
export function EditorMapeo({ columnas, mapeo, onCambiar, deshabilitado }) {
  const { faltantes, repetidos } = revisarMapeo(mapeo)

  return (
    <div className="mapeo">
      <ul className="mapeo__lista">
        {columnas.map((columna, i) => {
          const campo = mapeo[columna] ?? ''
          const repetido = campo && repetidos.includes(campo)
          const id = `mapeo-${i}`
          return (
            <li key={columna} className="mapeo__fila">
              <label htmlFor={id} className="mapeo__columna">
                <span className="rotulo">Columna del archivo</span>
                <strong>{columna}</strong>
              </label>
              <ArrowRight size={18} className="mapeo__flecha" aria-hidden="true" />
              <div className="mapeo__selector">
                <select
                  id={id}
                  className="campo__control"
                  value={campo}
                  disabled={deshabilitado}
                  onChange={(e) => onCambiar(columna, e.target.value)}
                  aria-invalid={Boolean(repetido)}
                  aria-describedby={repetido ? `${id}-error` : undefined}
                >
                  <option value="">Ignorar esta columna</option>
                  {CAMPOS_IMPORTACION.map((c) => (
                    <option key={c.campo} value={c.campo}>
                      {c.nombre}
                      {c.obligatorio ? '' : ' (opcional)'}
                    </option>
                  ))}
                </select>
                {repetido && (
                  <span id={`${id}-error`} className="campo__error">
                    Este campo ya está asignado a otra columna
                  </span>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      {faltantes.length > 0 && (
        <p className="mapeo__faltantes" role="status">
          <strong>Campos obligatorios sin columna:</strong> {faltantes.map((c) => NOMBRE_CAMPO[c]).join(', ')}.
        </p>
      )}
    </div>
  )
}
