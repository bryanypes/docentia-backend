import { MessagesSquare, RefreshCw, ShieldAlert, Sparkles, Tags } from 'lucide-react'
import { useCallback, useEffect } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { obtenerResumenComentarios } from '../../analisis/services/analisisService.js'
import './ComentariosResumen.css'

const SENTIMIENTOS = [
  { clave: 'positivos', etiqueta: 'Positivos', tono: 'turquesa' },
  { clave: 'negativos', etiqueta: 'Negativos', tono: 'coral' },
  { clave: 'mixtos', etiqueta: 'Mixtos', tono: 'violeta' },
  { clave: 'neutros', etiqueta: 'Neutros', tono: 'cielo' },
]

const COLOR = {
  turquesa: 'var(--color-turquesa)',
  coral: 'var(--color-coral)',
  violeta: 'var(--color-violeta)',
  cielo: 'var(--color-cielo)',
}

const MOTIVO_EXCLUSION = {
  INSTRUCCION_SOSPECHOSA: 'órdenes al modelo',
  VACIO: 'vacíos',
  CORTO: 'muy cortos',
  DUPLICADO: 'repetidos',
  OFENSIVO: 'ofensivos',
  LENGUAJE_OFENSIVO: 'ofensivos',
}

const POLARIDAD = { POSITIVO: 'Fortaleza', NEGATIVO: 'Por mejorar', NEUTRO: 'Neutro' }

const legible = (codigo) => MOTIVO_EXCLUSION[codigo] ?? codigo.toLowerCase().replaceAll('_', ' ')
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`

/** Anillo del sentimiento global: un sector por tipo, con el índice de -100 a 100 en el centro. */
function AnilloSentimiento({ sentimiento, total }) {
  let acumulado = 0
  const sectores = SENTIMIENTOS.map(({ clave, tono }) => {
    const desde = acumulado
    acumulado += total ? (sentimiento[clave] / total) * 360 : 0
    return `${COLOR[tono]} ${desde}deg ${acumulado}deg`
  })
  const indice = total ? Math.round(((sentimiento.positivos - sentimiento.negativos) / total) * 100) : 0
  return (
    <div className="comentarios-resumen__sentimiento">
      <div
        className="comentarios-resumen__anillo"
        style={{ background: total ? `conic-gradient(${sectores.join(', ')})` : 'var(--color-borde-suave)' }}
        role="img"
        aria-label={`Índice de sentimiento ${indice > 0 ? '+' : ''}${indice} sobre 100`}
      >
        <span>
          <strong>
            {indice > 0 ? '+' : ''}
            {indice}
          </strong>
          índice
        </span>
      </div>
      <ul className="comentarios-resumen__leyenda">
        {SENTIMIENTOS.map(({ clave, etiqueta, tono }) => (
          <li key={clave}>
            <span className={`comentarios-resumen__punto comentarios-resumen__punto--${tono}`} aria-hidden="true" />
            {etiqueta}
            <strong>{sentimiento[clave]}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Balance por criterio: negativos hacia la izquierda y positivos hacia la derecha. */
function BalanceCriterios({ criterios }) {
  const maximo = Math.max(1, ...criterios.map((c) => Math.max(c.positivos, c.negativos)))
  return (
    <ul className="comentarios-resumen__criterios">
      {criterios.map((c) => (
        <li key={c.codigo}>
          <span className="comentarios-resumen__criterio-nombre">
            <strong>{c.codigo}</strong> {c.nombre}
          </span>
          <span className="comentarios-resumen__divergente" aria-hidden="true">
            <span className="comentarios-resumen__lado comentarios-resumen__lado--neg">
              <span style={{ width: `${(c.negativos / maximo) * 100}%` }} />
            </span>
            <span className="comentarios-resumen__lado comentarios-resumen__lado--pos">
              <span style={{ width: `${(c.positivos / maximo) * 100}%` }} />
            </span>
          </span>
          <span className="comentarios-resumen__conteo">
            <span className="comentarios-resumen__neg">{c.negativos}</span>
            <span className="comentarios-resumen__pos">{c.positivos}</span>
            <span className="solo-lectores">
              {c.positivos} positivos, {c.negativos} negativos, {c.neutros} neutros
            </span>
          </span>
        </li>
      ))}
    </ul>
  )
}

/**
 * «Lo que dicen los comentarios»: agregado de los análisis vigentes del periodo en el alcance. No se
 * muestra si el backend dice que no está disponible (pocos docentes) y nunca se recalcula por docente.
 */
export function ComentariosResumen({ periodoId, programaId, alcance, esCoordinador, avisoFacultad, onDatos }) {
  const cargar = useCallback(
    () => (periodoId ? obtenerResumenComentarios({ periodo: periodoId, programa: programaId }) : Promise.resolve(null)),
    [periodoId, programaId],
  )
  const resumen = useFetch(cargar, [cargar])
  const datos = resumen.datos
  // La página lo usa para el Excel.
  useEffect(() => {
    onDatos?.(datos?.disponible ? datos : null)
  }, [datos, onDatos])

  return (
    <section className="comentarios-resumen" aria-labelledby="comentarios-resumen-titulo">
      <div className="comentarios-resumen__cabecera">
        <div>
          <h2 id="comentarios-resumen-titulo">
            <Sparkles size={18} aria-hidden="true" />
            Lo que dicen los comentarios
          </h2>
          <p>
            Análisis automático de los comentarios de estudiantes y de los formularios · {alcance}
            {datos?.disponible ? ` · ${plural(datos.docentesConAnalisis, 'docente analizado', 'docentes analizados')}` : ''}
          </p>
        </div>
        {datos?.disponible && (
          <span className="comentarios-resumen__total">
            <strong>{datos.nComentarios}</strong> comentarios
          </span>
        )}
      </div>
      {avisoFacultad && <p className="comentarios-resumen__aviso">{avisoFacultad}</p>}

      {resumen.cargando && !datos && <Cargando texto="Cargando el resumen de comentarios" bloques={2} />}
      {resumen.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar el resumen de comentarios" mensaje={resumen.error.message}>
          <Boton icono={RefreshCw} onClick={resumen.recargar}>
            Intentar de nuevo
          </Boton>
        </EstadoVista>
      )}
      {datos && !datos.disponible && (
        <EstadoVista
          titulo="Todavía no hay suficientes análisis"
          mensaje={`Para proteger el anonimato, el resumen aparece con al menos ${datos.minimoDocentes} docentes analizados en esta selección; hay ${datos.docentesConAnalisis}.`}
        />
      )}
      {datos?.disponible && datos.nComentarios === 0 && (
        <EstadoVista titulo="Sin comentarios útiles" mensaje="Los análisis de esta selección no tuvieron comentarios para resumir." />
      )}

      {datos?.disponible && datos.nComentarios > 0 && (
        <div className="comentarios-resumen__mosaico">
          <Panel titulo="Sentimiento" descripcion="Cómo se sienten los comentarios en conjunto." icono={MessagesSquare} tono="turquesa">
            <AnilloSentimiento sentimiento={datos.sentimientoGlobal} total={datos.nComentarios} />
          </Panel>

          <Panel titulo="Por criterio" descripcion="Comentarios negativos y positivos en cada criterio del Anexo 1." icono={Tags} tono="violeta">
            {datos.criterios.length ? (
              <BalanceCriterios criterios={datos.criterios} />
            ) : (
              <p className="comentarios-resumen__vacio">Ningún comentario se asoció a un criterio.</p>
            )}
          </Panel>

          <Panel
            className="comentarios-resumen__ancho"
            titulo="Temas más mencionados"
            descripcion="Porcentaje sobre los comentarios útiles y en cuántos docentes aparece cada tema."
            icono={Sparkles}
            tono="cielo"
          >
            <ol className="comentarios-resumen__temas">
              {datos.temas.map((t) => (
                <li key={`${t.criterio}-${t.descriptor}-${t.polaridad}`}>
                  <span className={`comentarios-resumen__polaridad comentarios-resumen__polaridad--${t.polaridad.toLowerCase()}`}>
                    {POLARIDAD[t.polaridad] ?? t.polaridad}
                  </span>
                  <span className="comentarios-resumen__tema">{t.nombre}</span>
                  <span className="comentarios-resumen__riel" aria-hidden="true">
                    <span style={{ width: `${Math.min(100, t.porcentaje)}%` }} />
                  </span>
                  <span className="comentarios-resumen__cifra">
                    <strong>{String(t.porcentaje).replace('.', ',')} %</strong>
                    {plural(t.docentes, 'docente', 'docentes')}
                  </span>
                </li>
              ))}
            </ol>
          </Panel>

          <p className="comentarios-resumen__pie">
            {Object.keys(datos.nExcluidos ?? {}).length
              ? `No se analizaron: ${Object.entries(datos.nExcluidos)
                  .map(([motivo, n]) => `${n} ${legible(motivo)}`)
                  .join(', ')}.`
              : 'Ningún comentario se excluyó.'}{' '}
            Las cifras suman los análisis vigentes de cada docente; los textos nunca se muestran aquí.
          </p>

          {esCoordinador && Object.keys(datos.advertencias ?? {}).length > 0 && (
            <div className="comentarios-resumen__advertencias" role="note">
              <ShieldAlert size={18} aria-hidden="true" />
              <span>
                <strong>Advertencias para revisar: </strong>
                {Object.entries(datos.advertencias)
                  .map(([tipo, n]) => `${n} ${legible(tipo.replace('SENSIBLE_', 'sensible: '))}`)
                  .join(' · ')}
                . Revísalas en el análisis de cada docente.
              </span>
            </div>
          )}
        </div>
      )}
    </section>
  )
}
