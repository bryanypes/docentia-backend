import { ArrowDownRight, ArrowRight, ArrowUpRight, CircleCheck, Minus, ShieldAlert, Sparkles, TriangleAlert } from 'lucide-react'
import { useCallback, useEffect } from 'react'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { obtenerAnalisisDocente } from '../services/analisisService.js'
import { ESTADO_ANALISIS } from '../estados.js'
import { MensajeIA } from './DocenteIA.jsx'
import './ResultadoComentarios.css'

const SONDEO_MS = 4000
const EN_CURSO = ['EN_COLA', 'EN_PROCESO']

const CAMBIO = {
  NUEVO: { texto: 'Nuevo', Icono: Sparkles, tono: 'violeta' },
  SE_MANTIENE: { texto: 'Se mantiene', Icono: Minus, tono: 'cielo' },
  EN_AUMENTO: { texto: 'En aumento', Icono: ArrowUpRight, tono: 'turquesa' },
  EN_DISMINUCION: { texto: 'En disminución', Icono: ArrowDownRight, tono: 'coral' },
  SIN_REFERENCIA: { texto: 'Sin periodo anterior', Icono: Minus, tono: 'neutro' },
}

const pct = (n) => `${String(Math.round(n * 10) / 10).replace('.', ',')} %`

/** Un tema con sus citas copiadas tal cual; tocarlo filtra los comentarios por su criterio. */
function Tema({ tema, onFiltrar, activo }) {
  return (
    <li className={`resultado-ia__tema resultado-ia__tema--${tema.polaridad.toLowerCase()}`}>
      <button
        type="button"
        className="resultado-ia__tema-boton"
        aria-pressed={activo}
        onClick={() => onFiltrar(activo ? null : tema.criterio)}
        title={activo ? 'Quitar el filtro' : `Ver los comentarios de ${tema.criterio}`}
      >
        <span className="resultado-ia__tema-nombre">{tema.nombre}</span>
        <span className="resultado-ia__tema-dato">
          {tema.criterio} · {pct(tema.porcentaje)} · {tema.frecuencia} {tema.frecuencia === 1 ? 'comentario' : 'comentarios'}
        </span>
      </button>
      {tema.citas?.length > 0 && (
        <ul className="resultado-ia__citas">
          {tema.citas.slice(0, 2).map((c) => (
            <li key={c.comentarioId}>
              <blockquote>«{c.fragmento}»</blockquote>
            </li>
          ))}
        </ul>
      )}
    </li>
  )
}

/**
 * Resultado del análisis de comentarios de un docente (GET /analisis-comentarios/docentes/{id}).
 * Mientras está en cola o en proceso se vuelve a consultar sola. Las advertencias sensibles solo las ve
 * coordinación y nunca muestran texto.
 */
export function ResultadoComentarios({ idDocente, periodoId, esCoordinador, criterioActivo, onFiltrarCriterio, onVersion, compacto = false }) {
  const cargar = useCallback(() => obtenerAnalisisDocente(idDocente, periodoId), [idDocente, periodoId])
  const analisis = useFetch(cargar, [cargar])
  const datos = analisis.datos
  const { recargar } = analisis

  useEffect(() => {
    if (!datos || !EN_CURSO.includes(datos.estado)) return undefined
    const temporizador = setTimeout(recargar, SONDEO_MS)
    return () => clearTimeout(temporizador)
  }, [datos, recargar])

  // Avisa qué versión del resultado hay, para que la lista de comentarios se recargue al terminar.
  const version = datos?.resultado ? `${datos.version}-${datos.generadoEn}` : null
  useEffect(() => {
    onVersion?.(version)
  }, [version, onVersion])

  if (analisis.cargando && !datos) return <p className="resultado-ia__cargando">Consultando el análisis…</p>
  if (analisis.error) {
    return (
      <MensajeIA robot="error" tono="coral" titulo="No fue posible consultar el análisis" role="alert">
        <p>{analisis.error.message}</p>
      </MensajeIA>
    )
  }
  if (!datos) return null

  const estado = ESTADO_ANALISIS[datos.estado]
  const r = datos.resultado
  const rotuloModelo = r?.tipoModelo === 'LLM' ? 'Propuesto por un modelo de lenguaje' : 'Análisis automático por reglas'

  return (
    <section
      className={`resultado-ia${compacto ? ' resultado-ia--compacto' : ''}`}
      {...(compacto ? { 'aria-label': 'Resumen del análisis' } : { 'aria-labelledby': 'resultado-ia-titulo' })}
    >
      {/* En la vista con pestañas el estado ya lo dice la franja de arriba: aquí solo van los rótulos. */}
      <div className="resultado-ia__cabecera">
        {!compacto && (
        <div>
          <h2 id="resultado-ia-titulo">
            <Sparkles size={18} aria-hidden="true" />
            Análisis de los comentarios
          </h2>
          <p>
            {datos.nombreDocente} · {datos.nombrePeriodo}
            {datos.fuente ? ` · ${datos.fuente === 'FORMULARIO' ? 'formulario de estudiantes' : 'carga CSV'}` : ''}
            {datos.version ? ` · versión ${datos.version}` : ''}
            {datos.generadoEn ? ` · ${formatearFechaHora(datos.generadoEn)}` : ''}
          </p>
        </div>
        )}
        {r && (
          <div className="resultado-ia__rotulos">
            <span className="resultado-ia__rotulo">{rotuloModelo}</span>
            {datos.estadoHallazgos === 'PENDIENTE_VALIDACION' ? (
              <span className="resultado-ia__rotulo resultado-ia__rotulo--pendiente">Pendiente de validación por coordinación</span>
            ) : datos.estadoHallazgos === 'VALIDADO' ? (
              <span className="resultado-ia__rotulo resultado-ia__rotulo--validado">
                <CircleCheck size={13} aria-hidden="true" /> Validado
              </span>
            ) : null}
          </div>
        )}
      </div>

      {compacto && !r && <p className="resultado-ia__vacio">Todavía no hay resultados para mostrar.</p>}

      {!compacto && datos.estado !== 'COMPLETADO' && estado && (
        <MensajeIA robot={estado.robot} tono={estado.tono} titulo={estado.titulo} role="status">
          <p>
            {datos.mensaje}
            {EN_CURSO.includes(datos.estado) ? ' La pantalla se actualiza sola.' : ''}
            {r && EN_CURSO.includes(datos.estado) ? ' Mientras tanto ves la versión anterior.' : ''}
          </p>
        </MensajeIA>
      )}

      {r && (
        <>
          <div className="resultado-ia__cifras">
            <div>
              <span>Comentarios analizados</span>
              <strong>{r.nComentarios}</strong>
            </div>
            <div>
              <span>Índice de sentimiento</span>
              <strong className={r.sentimientoGlobal.indice >= 0 ? 'resultado-ia__positivo' : 'resultado-ia__negativo'}>
                {r.sentimientoGlobal.indice > 0 ? '+' : ''}
                {Math.round(r.sentimientoGlobal.indice * 100)}
              </strong>
              <small>
                {r.sentimientoGlobal.positivos} positivos · {r.sentimientoGlobal.negativos} negativos
              </small>
            </div>
            <div>
              <span>No analizados</span>
              <strong>{Object.values(r.nExcluidos ?? {}).reduce((a, b) => a + b, 0)}</strong>
              <small>Ofensivos, repetidos u órdenes al modelo</small>
            </div>
          </div>

          <div className="resultado-ia__aspectos">
            <Panel titulo="Fortalezas" descripcion="Temas positivos que se repiten." icono={CircleCheck} tono="turquesa">
              {r.aspectos.fortalezas.length ? (
                <ul className="resultado-ia__temas">
                  {r.aspectos.fortalezas.map((t) => (
                    <Tema key={`${t.criterio}-${t.descriptor}`} tema={t} onFiltrar={onFiltrarCriterio} activo={criterioActivo === t.criterio} />
                  ))}
                </ul>
              ) : (
                <p className="resultado-ia__vacio">No hay fortalezas recurrentes.</p>
              )}
            </Panel>
            <Panel titulo="Oportunidades de mejora" descripcion="Temas negativos que se repiten." icono={TriangleAlert} tono="coral">
              {r.aspectos.oportunidades.length ? (
                <ul className="resultado-ia__temas">
                  {r.aspectos.oportunidades.map((t) => (
                    <Tema key={`${t.criterio}-${t.descriptor}`} tema={t} onFiltrar={onFiltrarCriterio} activo={criterioActivo === t.criterio} />
                  ))}
                </ul>
              ) : (
                <p className="resultado-ia__vacio">No hay oportunidades recurrentes.</p>
              )}
            </Panel>
          </div>

          {/* Sin periodo anterior no hay con qué comparar: en vez de una lista de "Sin periodo anterior"
              se dice una sola vez. */}
          {!r.tendencias.some((t) => t.cambio !== 'SIN_REFERENCIA') ? (
            <MensajeIA robot="esperando" tono="violeta" tamano="compacto" titulo="Todavía no puedo mostrar tendencias">
              <p>No tengo información del periodo anterior de este docente, así que no puedo comparar cómo cambió cada tema.</p>
            </MensajeIA>
          ) : (
            <Panel titulo="Tendencias" descripcion="Cómo cambió cada tema frente al periodo anterior." icono={ArrowRight} tono="violeta">
              <ul className="resultado-ia__tendencias">
                {r.tendencias.map((t) => {
                  const cambio = CAMBIO[t.cambio] ?? CAMBIO.SIN_REFERENCIA
                  const tema = r.temas.find((x) => x.criterio === t.criterio && x.descriptor === t.descriptor)
                  return (
                    <li key={`${t.criterio}-${t.descriptor}-${t.polaridad}`}>
                      <span>{tema?.nombre ?? `${t.criterio} · ${t.descriptor}`}</span>
                      <span className="resultado-ia__porcentajes">
                        {t.porcentajeAnterior === null ? '—' : pct(t.porcentajeAnterior)} → {pct(t.porcentajeActual)}
                      </span>
                      <span className={`resultado-ia__cambio resultado-ia__cambio--${cambio.tono}`}>
                        <cambio.Icono size={13} aria-hidden="true" />
                        {cambio.texto}
                      </span>
                    </li>
                  )
                })}
              </ul>
            </Panel>
          )}

          {esCoordinador && r.advertencias.some((a) => a.tipo.startsWith('SENSIBLE_')) && (
            <div className="resultado-ia__sensible" role="note">
              <ShieldAlert size={18} aria-hidden="true" />
              <span>
                <strong>Requiere atención de coordinación: </strong>
                {r.advertencias
                  .filter((a) => a.tipo.startsWith('SENSIBLE_'))
                  .map((a) => a.mensaje)
                  .join(' ')}
              </span>
            </div>
          )}
        </>
      )}
    </section>
  )
}
