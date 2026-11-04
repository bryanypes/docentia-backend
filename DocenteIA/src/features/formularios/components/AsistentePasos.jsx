import { Check, ChevronLeft, ChevronRight, ClipboardCheck } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import './AsistentePasos.css'

// Alto de la cabecera fija en su versión compacta (rótulo del paso y barra de avance).
const ALTO_CABECERA_COMPACTA = 64

/** Alto de la barra superior fija del celular; en escritorio no hay barra fija. */
function altoBarraFija() {
  const barra = typeof document === 'undefined' ? null : document.querySelector('.barra-movil')
  return barra && getComputedStyle(barra).display !== 'none' ? barra.getBoundingClientRect().height : 0
}

/**
 * Cuestionario por pasos: una barra de pasos arriba (pendiente, actual, completo), el contenido del
 * paso con una transición suave y Anterior / Siguiente abajo. El último paso es la revisión.
 *
 * @param pasos [{ codigo, titulo, respondidas, total, contenido }]
 * @param revision { titulo, contenido } paso final, siempre disponible
 */
export function AsistentePasos({ pasos, revision, etiqueta = 'Pasos del cuestionario' }) {
  const todos = [...pasos, { codigo: '__revision', titulo: revision.titulo, revision: true, contenido: revision.contenido }]
  const [actual, setActual] = useState(0)
  const [direccion, setDireccion] = useState('adelante')
  const cuerpo = useRef(null)
  const pasoAnterior = useRef(0)
  const idTitulo = useId()
  const paso = todos[actual]
  const respondidas = pasos.reduce((a, p) => a + p.respondidas, 0)
  const total = pasos.reduce((a, p) => a + p.total, 0)

  // Al bajar, la cabecera queda fija en versión compacta: un centinela invisible marca cuándo se pegó.
  const centinela = useRef(null)
  const [compacta, setCompacta] = useState(false)
  useEffect(() => {
    const elemento = centinela.current
    if (!elemento || typeof IntersectionObserver === 'undefined') return undefined
    const observador = new IntersectionObserver(([entrada]) => setCompacta(!entrada.isIntersecting), {
      rootMargin: `-${altoBarraFija()}px 0px 0px 0px`,
    })
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])

  // Al cambiar de paso la página sube suave hasta el título del paso nuevo (debajo de las barras
  // fijas) y el foco va al título, para que el lector de pantalla lo anuncie.
  useEffect(() => {
    // Solo cuando el paso cambia de verdad (no al montar, ni en el doble montaje del modo estricto).
    if (pasoAnterior.current === actual) return
    pasoAnterior.current = actual
    const titulo = cuerpo.current?.querySelector('[data-titulo-paso]')
    if (cuerpo.current && typeof window.scrollTo === 'function') {
      const tope = cuerpo.current.getBoundingClientRect().top + window.scrollY
      const reducir = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
      window.scrollTo({
        top: Math.max(0, tope - altoBarraFija() - ALTO_CABECERA_COMPACTA - 12),
        behavior: reducir ? 'auto' : 'smooth',
      })
    }
    titulo?.focus({ preventScroll: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo reacciona al cambio de paso
  }, [actual])

  function ir(indice) {
    if (indice < 0 || indice >= todos.length || indice === actual) return
    setDireccion(indice > actual ? 'adelante' : 'atras')
    setActual(indice)
  }

  return (
    <div className="cuestionario">
      <span ref={centinela} className="cuestionario__centinela" aria-hidden="true" />
      <div className={`cuestionario__cabecera${compacta ? ' cuestionario__cabecera--compacta' : ''}`}>
        <p className="cuestionario__resumen-paso" aria-hidden="true">
          <strong>{paso.revision ? 'Último paso' : `Paso ${actual + 1} de ${pasos.length}`}</strong>
          <span>{paso.titulo}</span>
        </p>
        <div className="cuestionario__avance">
          <span>
            {respondidas} de {total} ítems respondidos
          </span>
          <span className="cuestionario__riel" aria-hidden="true">
            <span className="cuestionario__relleno" style={{ width: `${total ? (respondidas / total) * 100 : 0}%` }} />
          </span>
          <progress className="solo-lectores" value={respondidas} max={total} aria-label="Avance del cuestionario" />
        </div>
        <ol className="cuestionario__pasos" aria-label={etiqueta}>
          {todos.map((p, i) => {
            const completo = p.revision ? false : p.respondidas === p.total
            const estado = i === actual ? 'actual' : completo ? 'completo' : 'pendiente'
            return (
              <li key={p.codigo} className={`cuestionario__paso cuestionario__paso--${estado}`}>
                <button
                  type="button"
                  aria-current={i === actual ? 'step' : undefined}
                  onClick={() => ir(i)}
                  title={p.titulo}
                >
                  <span className="cuestionario__marca" aria-hidden="true">
                    {p.revision ? <ClipboardCheck size={15} /> : completo && i !== actual ? <Check size={15} strokeWidth={3} /> : i + 1}
                  </span>
                  <span className="cuestionario__nombre">{p.titulo}</span>
                  <span className="solo-lectores">
                    {p.revision ? '' : completo ? ' (completo)' : ` (${p.respondidas} de ${p.total})`}
                  </span>
                </button>
              </li>
            )
          })}
        </ol>
      </div>

      <section ref={cuerpo} className="cuestionario__cuerpo" aria-labelledby={idTitulo}>
        <div key={paso.codigo} className={`cuestionario__contenido cuestionario__contenido--${direccion}`}>
          <div className="cuestionario__titulo-paso">
            <span className="cuestionario__contador">
              {paso.revision ? 'Último paso' : `Paso ${actual + 1} de ${pasos.length}`}
            </span>
            <h2 id={idTitulo} tabIndex={-1} data-titulo-paso="">
              {paso.titulo}
            </h2>
            {!paso.revision && (
              <span className={`cuestionario__estado${paso.respondidas === paso.total ? ' cuestionario__estado--completo' : ''}`}>
                {paso.respondidas} de {paso.total} respondidos
              </span>
            )}
          </div>
          {paso.contenido}
        </div>
      </section>

      <div className="cuestionario__navegacion">
        <button type="button" className="boton boton--contorno" disabled={actual === 0} onClick={() => ir(actual - 1)}>
          <ChevronLeft size={16} aria-hidden="true" />
          Anterior
        </button>
        {actual < todos.length - 1 && (
          <button type="button" className="boton boton--primario" onClick={() => ir(actual + 1)}>
            {actual === todos.length - 2 ? 'Revisar' : 'Siguiente'}
            <ChevronRight size={16} aria-hidden="true" />
          </button>
        )}
      </div>
    </div>
  )
}

/** Un ítem del instrumento con la escala como tarjetas; el color cambia con el valor elegido. */
export function PreguntaEscala({ descriptor, escala, valor, editable, onResponder }) {
  return (
    <fieldset className="pregunta">
      <legend>{descriptor.texto}</legend>
      <div className="pregunta__opciones">
        {escala.map((opcion) => (
          <label key={opcion.valor} className={`pregunta__opcion pregunta__opcion--${opcion.valor}`}>
            <input
              type="radio"
              name={descriptor.codigo}
              value={opcion.valor}
              checked={valor === opcion.valor}
              disabled={!editable}
              onChange={() => onResponder(descriptor.codigo, opcion.valor)}
            />
            <span className="pregunta__valor" aria-hidden="true">
              {opcion.valor}
            </span>
            <span className="pregunta__etiqueta">{opcion.etiqueta}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}
