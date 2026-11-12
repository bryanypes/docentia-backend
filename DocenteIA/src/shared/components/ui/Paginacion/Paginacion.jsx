import { ChevronLeft, ChevronRight } from 'lucide-react'
import { TAMANOS } from '../../../hooks/usePaginacion.js'
import './Paginacion.css'

/** Pie de una lista paginada: «Mostrando X–Y de Z», tamaño de página y Anterior/Siguiente. */
export function Paginacion({ pagina, paginas, tamano, total, desde, hasta, irA, cambiarTamano, etiqueta = 'resultados' }) {
  if (total === 0) return null
  return (
    <nav className="paginacion" aria-label={`Paginación de ${etiqueta}`}>
      <span className="paginacion__contador" aria-live="polite">
        Mostrando {desde}–{hasta} de {total}
      </span>
      <label className="paginacion__tamano">
        <span>Por página</span>
        <select value={tamano} onChange={(e) => cambiarTamano(Number(e.target.value))}>
          {TAMANOS.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
      </label>
      <div className="paginacion__botones">
        <button type="button" className="boton boton--contorno" disabled={pagina <= 1} onClick={() => irA(pagina - 1)}>
          <ChevronLeft size={16} aria-hidden="true" />
          Anterior
        </button>
        <span className="paginacion__pagina">
          Página {pagina} de {paginas}
        </span>
        <button type="button" className="boton boton--contorno" disabled={pagina >= paginas} onClick={() => irA(pagina + 1)}>
          Siguiente
          <ChevronRight size={16} aria-hidden="true" />
        </button>
      </div>
    </nav>
  )
}
