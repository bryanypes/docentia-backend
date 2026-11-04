import { Frown, Meh, MessageSquareQuote, Smile, X } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { listarComentariosDocente } from '../services/analisisService.js'
import './ResultadoComentarios.css'

// El backend entrega hasta 50 por página; se piden todas para agrupar los comentarios por tema.
const TAMANO = 50
const MAXIMO_PAGINAS = 6
const CRITERIOS = ['C1', 'C2', 'C3', 'C4', 'C5', 'C6']
const SENTIMIENTO = {
  POSITIVO: { texto: 'Positivo', Icono: Smile },
  NEGATIVO: { texto: 'Negativo', Icono: Frown },
  NEUTRO: { texto: 'Neutro', Icono: Meh },
  MIXTO: { texto: 'Mixto', Icono: Meh },
}

async function cargarTodos(idDocente, filtros) {
  const primera = await listarComentariosDocente(idDocente, { ...filtros, pagina: 0, tamano: TAMANO })
  if (!primera?.disponible) return primera
  const paginas = Math.min(MAXIMO_PAGINAS, Math.ceil(primera.total / (primera.tamano || TAMANO)))
  const resto = await Promise.all(
    Array.from({ length: Math.max(0, paginas - 1) }, (_, i) =>
      listarComentariosDocente(idDocente, { ...filtros, pagina: i + 1, tamano: TAMANO }),
    ),
  )
  return { ...primera, comentarios: [primera, ...resto].flatMap((p) => p?.comentarios ?? []) }
}

/**
 * Cada comentario va a un solo tema: el primero cuya polaridad coincide con su sentimiento. Así un
 * comentario con cuatro etiquetas no aparece cuatro veces. Sin tema, queda en "Otros comentarios".
 */
function agruparPorTema(comentarios) {
  const grupos = new Map()
  const otros = []
  for (const c of comentarios) {
    const etiqueta =
      c.etiquetas.find((e) => e.polaridad === c.sentimiento && e.polaridad !== 'NEUTRO') ??
      c.etiquetas.find((e) => e.polaridad !== 'NEUTRO')
    if (!etiqueta) {
      otros.push(c)
      continue
    }
    const clave = `${etiqueta.criterio}-${etiqueta.descriptor}-${etiqueta.polaridad}`
    if (!grupos.has(clave)) grupos.set(clave, { clave, ...etiqueta, comentarios: [] })
    grupos.get(clave).comentarios.push(c)
  }
  const ordenados = [...grupos.values()].sort((a, b) => b.comentarios.length - a.comentarios.length)
  return {
    fortalezas: ordenados.filter((g) => g.polaridad === 'POSITIVO'),
    oportunidades: ordenados.filter((g) => g.polaridad === 'NEGATIVO'),
    otros,
  }
}

function FilaComentario({ comentario }) {
  const s = SENTIMIENTO[comentario.sentimiento] ?? SENTIMIENTO.NEUTRO
  return (
    <li className={`comentarios-docente__fila comentarios-docente__fila--${comentario.sentimiento.toLowerCase()}`}>
      <s.Icono size={15} aria-hidden="true" />
      <span className="solo-lectores">{s.texto}: </span>
      <span>{comentario.texto}</span>
    </li>
  )
}

/** Un tema plegado: nombre, cuántos comentarios lo mencionan y, al abrirlo, esos comentarios. */
function GrupoTema({ nombre, etiqueta, tono, comentarios, maximo }) {
  const n = comentarios.length
  return (
    <details className={`comentarios-docente__tema comentarios-docente__tema--${tono}`}>
      <summary>
        <span className="comentarios-docente__tema-nombre">{nombre}</span>
        <span className="comentarios-docente__tema-criterio">{etiqueta}</span>
        <span className="comentarios-docente__tema-barra" aria-hidden="true">
          {maximo && <span style={{ width: `${(n / maximo) * 100}%` }} />}
        </span>
        <strong>
          {n} <span className="solo-lectores">{n === 1 ? 'comentario' : 'comentarios'}</span>
        </strong>
      </summary>
      <ul className="comentarios-docente__filas">
        {comentarios.map((c) => (
          <FilaComentario key={c.indice} comentario={c} />
        ))}
      </ul>
    </details>
  )
}

function Seccion({ titulo, grupos, maximo }) {
  if (grupos.length === 0) return null
  return (
    <section className="comentarios-docente__seccion">
      <h3>{titulo}</h3>
      {grupos.map((g) => (
        <GrupoTema
          key={g.clave}
          nombre={g.nombre}
          etiqueta={g.criterio}
          tono={g.polaridad.toLowerCase()}
          comentarios={g.comentarios}
          maximo={maximo}
        />
      ))}
    </section>
  )
}

/**
 * Comentarios anonimizados del análisis vigente (GET /comentarios-estudiantes/docentes/{id}),
 * agrupados por tema: primero lo que más se repite, y cada tema se abre para leer sus comentarios.
 * Nunca muestra los excluidos. El criterio puede venir de tocar un tema en el resultado.
 */
export function ComentariosDocente({ idDocente, periodoId, criterio, onCriterio }) {
  const [sentimiento, setSentimiento] = useState('')
  const cargar = useCallback(
    () => cargarTodos(idDocente, { periodo: periodoId, criterio, sentimiento }),
    [idDocente, periodoId, criterio, sentimiento],
  )
  const lista = useFetch(cargar, [cargar])
  const datos = lista.datos
  const grupos = useMemo(() => agruparPorTema(datos?.comentarios ?? []), [datos])
  const maximo = Math.max(1, ...[...grupos.fortalezas, ...grupos.oportunidades].map((g) => g.comentarios.length))

  return (
    <Panel
      className="comentarios-docente"
      titulo="Comentarios de los estudiantes"
      descripcion="Agrupados por tema: abre uno para leer lo que escribieron. Anonimizados; los ofensivos o con órdenes al modelo no aparecen."
      icono={MessageSquareQuote}
    >
      <div className="comentarios-docente__filtros">
        <label>
          <span>Criterio</span>
          <select className="campo__control" value={criterio ?? ''} onChange={(e) => onCriterio(e.target.value || null)}>
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
          <select className="campo__control" value={sentimiento} onChange={(e) => setSentimiento(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(SENTIMIENTO).map(([clave, { texto }]) => (
              <option key={clave} value={clave}>
                {texto}
              </option>
            ))}
          </select>
        </label>
        {criterio && (
          <button type="button" className="comentarios-docente__quitar" onClick={() => onCriterio(null)}>
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
        <div className="comentarios-docente__grupos">
          <Seccion titulo="Fortalezas que se repiten" grupos={grupos.fortalezas} maximo={maximo} />
          <Seccion titulo="Oportunidades de mejora que se repiten" grupos={grupos.oportunidades} maximo={maximo} />
          {grupos.otros.length > 0 && (
            <section className="comentarios-docente__seccion">
              <GrupoTema nombre="Otros comentarios" etiqueta="Sin tema" tono="otros" comentarios={grupos.otros} />
            </section>
          )}
        </div>
      )}
    </Panel>
  )
}
