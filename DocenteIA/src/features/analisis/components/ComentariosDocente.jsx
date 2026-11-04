import { Frown, Meh, MessageSquareQuote, Smile, X } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { listarComentariosDocente } from '../services/analisisService.js'
import './ResultadoComentarios.css'

const TAMANO = 10
const CRITERIOS = ['C1', 'C2', 'C3', 'C4', 'C5', 'C6']
const SENTIMIENTO = {
  POSITIVO: { texto: 'Positivo', Icono: Smile },
  NEGATIVO: { texto: 'Negativo', Icono: Frown },
  NEUTRO: { texto: 'Neutro', Icono: Meh },
  MIXTO: { texto: 'Mixto', Icono: Meh },
}

/**
 * Comentarios anonimizados del análisis vigente (GET /comentarios-estudiantes/docentes/{id}). Nunca
 * muestra los excluidos. El criterio puede venir de tocar un tema en el resultado.
 */
export function ComentariosDocente({ idDocente, periodoId, criterio, onCriterio }) {
  const [sentimiento, setSentimiento] = useState('')
  const [pagina, setPagina] = useState(0)
  const cargar = useCallback(
    () => listarComentariosDocente(idDocente, { periodo: periodoId, criterio, sentimiento, pagina, tamano: TAMANO }),
    [idDocente, periodoId, criterio, sentimiento, pagina],
  )
  const lista = useFetch(cargar, [cargar])
  const datos = lista.datos
  const totalPaginas = datos ? Math.max(1, Math.ceil(datos.total / (datos.tamano || TAMANO))) : 1

  function elegirCriterio(valor) {
    setPagina(0)
    onCriterio(valor || null)
  }

  return (
    <Panel
      className="comentarios-docente"
      titulo="Comentarios de los estudiantes"
      descripcion="Anonimizados, tal como se analizaron. Los ofensivos o con órdenes al modelo no aparecen."
      icono={MessageSquareQuote}
    >
      <div className="comentarios-docente__filtros">
        <label>
          <span>Criterio</span>
          <select className="campo__control" value={criterio ?? ''} onChange={(e) => elegirCriterio(e.target.value)}>
            <option value="">Todos</option>
            {CRITERIOS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Sentimiento</span>
          <select
            className="campo__control"
            value={sentimiento}
            onChange={(e) => {
              setPagina(0)
              setSentimiento(e.target.value)
            }}
          >
            <option value="">Todos</option>
            {Object.entries(SENTIMIENTO).map(([clave, { texto }]) => (
              <option key={clave} value={clave}>
                {texto}
              </option>
            ))}
          </select>
        </label>
        {criterio && (
          <button type="button" className="comentarios-docente__quitar" onClick={() => elegirCriterio('')}>
            <X size={14} aria-hidden="true" />
            Quitar {criterio}
          </button>
        )}
        {datos?.disponible && (
          <span className="comentarios-docente__conteo" aria-live="polite">
            {datos.total} {datos.total === 1 ? 'comentario' : 'comentarios'}
          </span>
        )}
      </div>

      {lista.cargando && !datos && <p className="resultado-ia__cargando">Cargando comentarios…</p>}
      {lista.error && <p className="resultado-ia__vacio">No fue posible cargar los comentarios: {lista.error.message}</p>}
      {datos && !datos.disponible && (
        <p className="resultado-ia__vacio">Los comentarios aparecen cuando el análisis está completo.</p>
      )}
      {datos?.disponible && datos.comentarios.length === 0 && (
        <p className="resultado-ia__vacio">Ningún comentario coincide con los filtros.</p>
      )}
      {datos?.disponible && datos.comentarios.length > 0 && (
        <ol className="comentarios-docente__lista">
          {datos.comentarios.map((c) => {
            const s = SENTIMIENTO[c.sentimiento] ?? SENTIMIENTO.NEUTRO
            return (
              <li key={c.indice} className={`comentarios-docente__item comentarios-docente__item--${c.sentimiento.toLowerCase()}`}>
                <span className="comentarios-docente__sentimiento" title={s.texto}>
                  <s.Icono size={18} aria-hidden="true" />
                  <span className="solo-lectores">{s.texto}</span>
                </span>
                <div>
                  <p>{c.texto}</p>
                  {c.etiquetas.length > 0 && (
                    <ul className="comentarios-docente__etiquetas">
                      {c.etiquetas.map((e) => (
                        <li key={`${e.criterio}-${e.descriptor}`} className={`comentarios-docente__etiqueta comentarios-docente__etiqueta--${e.polaridad.toLowerCase()}`}>
                          {e.criterio} · {e.nombre}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </li>
            )
          })}
        </ol>
      )}

      {datos?.disponible && totalPaginas > 1 && (
        <div className="comentarios-docente__paginas">
          <Boton variante="contorno" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
            Anterior
          </Boton>
          <span>
            Página {pagina + 1} de {totalPaginas}
          </span>
          <Boton variante="contorno" disabled={pagina + 1 >= totalPaginas} onClick={() => setPagina((p) => p + 1)}>
            Siguiente
          </Boton>
        </div>
      )}
    </Panel>
  )
}
