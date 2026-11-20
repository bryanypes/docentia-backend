import { AlertTriangle, Brain, MessageSquareOff, MessagesSquare, RefreshCw, Search, Sparkles, TrendingUp, Wand2 } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { normalizarTexto } from '../../../shared/utils/formatters.js'
import { MINIMO_COMENTARIOS } from '../estados.js'
import { obtenerPanorama } from '../services/analisisService.js'
import { MensajeIA } from './DocenteIA.jsx'
import './PanoramaAnalisis.css'

/** En qué punto está cada docente, con su tono y lo que significa. */
const ETAPAS = {
  LISTO: {
    texto: 'Listo para analizar',
    tono: 'alerta',
    explicacion: `Tienen ${MINIMO_COMENTARIOS} o más comentarios y todavía no se ha pedido su análisis.`,
  },
  EN_CURSO: { texto: 'En curso', tono: 'cielo', explicacion: 'DocenteIA está leyendo sus comentarios.' },
  REVISION: {
    texto: 'Por revisar',
    tono: 'violeta',
    explicacion: 'El análisis está listo: falta incluir o descartar cada hallazgo y publicarlo.',
  },
  PUBLICADO: { texto: 'Publicado', tono: 'exito', explicacion: 'La revisión ya se publicó y el docente puede verla.' },
  POCOS: {
    texto: 'Evidencia insuficiente',
    tono: 'neutro',
    explicacion: `Tienen menos de ${MINIMO_COMENTARIOS} comentarios: no alcanzan para un análisis confiable.`,
  },
  SIN_COMENTARIOS: {
    texto: 'Sin comentarios',
    tono: 'neutro',
    explicacion: 'Ningún estudiante ha escrito comentarios sobre ellos en este periodo.',
  },
  ERROR: { texto: 'Con error', tono: 'coral', explicacion: 'El análisis falló; puedes volver a pedirlo.' },
}
const ORDEN_ETAPAS = ['LISTO', 'EN_CURSO', 'REVISION', 'PUBLICADO', 'POCOS', 'SIN_COMENTARIOS', 'ERROR']

function etapaDe(fila) {
  if (fila.estado === 'EN_COLA' || fila.estado === 'EN_PROCESO') return 'EN_CURSO'
  if (fila.estado === 'ERROR') return 'ERROR'
  if (fila.estado === 'COMPLETADO') return fila.publicado ? 'PUBLICADO' : 'REVISION'
  if (fila.comentarios === 0) return 'SIN_COMENTARIOS'
  if (fila.comentarios < MINIMO_COMENTARIOS || fila.estado === 'INSUFICIENTE') return 'POCOS'
  return 'LISTO'
}

/** Cuántos comentarios tiene y, si no alcanzan, cuántos le faltan. */
function textoComentarios(fila) {
  if (fila.comentarios === 0) return 'Sin comentarios'
  const base = `${fila.comentarios} ${fila.comentarios === 1 ? 'comentario' : 'comentarios'}`
  const faltan = MINIMO_COMENTARIOS - fila.comentarios
  return faltan > 0 ? `${base} · faltan ${faltan} para analizar` : base
}

/**
 * Panorama del análisis de comentarios del alcance: cuántos comentarios hay, qué docentes se pueden
 * analizar y en qué va cada uno. Las cifras y los filtros llevan a la lista de docentes.
 */
export function PanoramaAnalisis({ periodoId, onElegirDocente }) {
  const cargar = useCallback(() => obtenerPanorama(periodoId), [periodoId])
  const panorama = useFetch(cargar, [cargar])
  const [filtros, actualizarFiltros] = useFiltrosUrl({ etapa: '', busqueda: '' })
  const { etapa: filtroEtapa, busqueda } = filtros

  const filas = useMemo(
    () => (panorama.datos?.docentes ?? []).map((f) => ({ ...f, etapa: etapaDe(f) })),
    [panorama.datos],
  )
  const visibles = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return filas.filter(
      (f) =>
        (!filtroEtapa || f.etapa === filtroEtapa) &&
        (!termino || normalizarTexto(f.docente.nombreCompleto).includes(termino)),
    )
  }, [filas, filtroEtapa, busqueda])
  const paginacion = usePaginacion(visibles)

  if (panorama.cargando && !panorama.datos) return <Cargando texto="Calculando el panorama del análisis" bloques={2} />
  if (panorama.error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar el panorama" mensaje={panorama.error.message}>
        <Boton icono={RefreshCw} onClick={panorama.recargar}>Intentar de nuevo</Boton>
      </EstadoVista>
    )
  }
  const t = panorama.datos.totales
  const hallazgos = t.fortalezas + t.oportunidades
  const cuantos = (clave) => filas.filter((f) => f.etapa === clave).length
  const filtrar = (clave) => actualizarFiltros({ etapa: filtroEtapa === clave ? '' : clave })

  const cifras = [
    {
      clave: 'LISTO',
      Icono: Wand2,
      titulo: 'Listos para analizar',
      detalle: `Con ${MINIMO_COMENTARIOS} o más comentarios, sin analizar`,
      tono: 'violeta',
    },
    {
      clave: 'POCOS',
      Icono: AlertTriangle,
      titulo: 'Evidencia insuficiente',
      detalle: `Menos de ${MINIMO_COMENTARIOS} comentarios`,
      tono: 'neutro',
    },
    {
      clave: 'SIN_COMENTARIOS',
      Icono: MessageSquareOff,
      titulo: 'Sin comentarios',
      detalle: 'Ningún estudiante ha escrito',
      tono: 'neutro',
    },
    { clave: 'ERROR', Icono: RefreshCw, titulo: 'Con error', detalle: 'Se puede volver a pedir', tono: 'coral' },
  ]

  return (
    <div className="panorama">
      <MensajeIA robot={hallazgos ? 'aprobada' : 'pensando'} tono="turquesa" tamano="compacto">
        <p>
          {`Los estudiantes han escrito ${t.comentarios} ${t.comentarios === 1 ? 'comentario' : 'comentarios'} sobre ${t.conComentarios} de ${t.docentes} docentes. `}
          {hallazgos
            ? `En los análisis hechos los agrupé en ${t.fortalezas} fortalezas y ${t.oportunidades} oportunidades de mejora.`
            : `Puedo analizar a cualquier docente con al menos ${MINIMO_COMENTARIOS} comentarios, cuando lo necesites.`}
        </p>
      </MensajeIA>

      <div className="panorama__cifras">
        <div className="panorama__cifra">
          <MessagesSquare size={18} aria-hidden="true" />
          <span>Comentarios recibidos</span>
          <strong>{t.comentarios}</strong>
          <small>
            {t.analizados} docentes analizados · {t.pendientesRevision} por revisar · {t.publicados} publicados
          </small>
        </div>
        {cifras.map(({ clave, Icono, titulo, detalle, tono }) => (
          <button
            key={clave}
            type="button"
            className={`panorama__cifra panorama__cifra--${tono} panorama__cifra--boton${filtroEtapa === clave ? ' panorama__cifra--activa' : ''}`}
            aria-pressed={filtroEtapa === clave}
            onClick={() => filtrar(clave)}
          >
            <Icono size={18} aria-hidden="true" />
            <span>{titulo}</span>
            <strong>{cuantos(clave)}</strong>
            <small>
              {cuantos(clave) === 1 ? 'docente' : 'docentes'} · {detalle}
            </small>
          </button>
        ))}
      </div>

      {hallazgos > 0 && (
        <Panel titulo="Lo que dicen los estudiantes" descripcion="Hallazgos de los análisis completados." icono={Sparkles} tono="turquesa">
          <div className="panorama__balance" aria-hidden="true">
            <span className="panorama__tono--exito" style={{ flexGrow: t.fortalezas }} />
            <span className="panorama__tono--coral" style={{ flexGrow: t.oportunidades }} />
          </div>
          <dl className="panorama__leyenda">
            <div>
              <dt>
                <span className="panorama__punto panorama__tono--exito" aria-hidden="true" />
                Fortalezas
              </dt>
              <dd>{t.fortalezas}</dd>
            </div>
            <div>
              <dt>
                <span className="panorama__punto panorama__tono--coral" aria-hidden="true" />
                Oportunidades de mejora
              </dt>
              <dd>{t.oportunidades}</dd>
            </div>
            <div>
              <dt>
                <span className="panorama__punto panorama__tono--neutro" aria-hidden="true" />
                Por decidir
              </dt>
              <dd>{t.porDecidir}</dd>
            </div>
            <div>
              <dt>
                <span className="panorama__punto panorama__tono--rosa" aria-hidden="true" />
                Descartados
              </dt>
              <dd>{t.descartados}</dd>
            </div>
          </dl>
          <p className="panorama__nota">
            <TrendingUp size={15} aria-hidden="true" />
            {t.comentariosFiltrados > 0
              ? `${t.comentariosFiltrados} comentarios ofensivos o sobre rasgos personales se excluyeron y no llegaron al análisis.`
              : 'Ningún comentario se excluyó por ofensivo o por hablar de rasgos personales.'}
          </p>
        </Panel>
      )}

      <Panel titulo="Docentes" descripcion="Elige uno para ver o pedir su análisis." icono={Brain}>
        <div className="panorama__filtros">
          <label className="panorama__buscador">
            <Search size={16} aria-hidden="true" />
            <span className="solo-lectores">Buscar docente por nombre</span>
            <input
              type="search"
              className="campo__control"
              placeholder="Buscar docente por nombre"
              value={busqueda}
              onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
            />
          </label>
          <div className="panorama__chips" role="group" aria-label="Filtrar por estado del análisis">
            <button
              type="button"
              className={`panorama__chip${!filtroEtapa ? ' panorama__chip--activo' : ''}`}
              aria-pressed={!filtroEtapa}
              onClick={() => actualizarFiltros({ etapa: '' })}
            >
              Todos <span>{filas.length}</span>
            </button>
            {ORDEN_ETAPAS.map((clave) => (
              <button
                key={clave}
                type="button"
                className={`panorama__chip${filtroEtapa === clave ? ' panorama__chip--activo' : ''}`}
                aria-pressed={filtroEtapa === clave}
                title={ETAPAS[clave].explicacion}
                onClick={() => filtrar(clave)}
              >
                {ETAPAS[clave].texto} <span>{cuantos(clave)}</span>
              </button>
            ))}
          </div>
          {filtroEtapa && (
            <p className="panorama__explicacion" role="status">
              <strong>{ETAPAS[filtroEtapa].texto}:</strong> {ETAPAS[filtroEtapa].explicacion}
            </p>
          )}
        </div>
        <ul className="panorama__docentes">
          {paginacion.visibles.map((f) => (
            <li key={f.docente.idDocente}>
              <div className="panorama__docente">
                <strong>{f.docente.nombreCompleto}</strong>
                <span>
                  {f.docente.nombrePrograma ?? 'Coordinación'} · {textoComentarios(f)}
                </span>
              </div>
              <span className={`pildora pildora--${ETAPAS[f.etapa].tono}`}>{ETAPAS[f.etapa].texto}</span>
              <span className="panorama__mini">
                {f.fortalezas + f.oportunidades > 0 ? (
                  <>
                    <span title="Fortalezas">+{f.fortalezas}</span>
                    <span title="Oportunidades de mejora">△{f.oportunidades}</span>
                  </>
                ) : (
                  '—'
                )}
              </span>
              <BotonAccion
                tipo="ver"
                texto="Abrir análisis"
                nombre={f.docente.nombreCompleto}
                onClick={() => onElegirDocente(String(f.docente.idDocente))}
              />
            </li>
          ))}
          {visibles.length === 0 && (
            <li className="campo__ayuda">
              {filtroEtapa ? `Ningún docente está en «${ETAPAS[filtroEtapa].texto}».` : 'Ningún docente coincide.'}
            </li>
          )}
        </ul>
        <Paginacion {...paginacion} etiqueta="análisis por docente" />
      </Panel>
    </div>
  )
}
