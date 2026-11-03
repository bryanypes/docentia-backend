import { BadgeCheck, Brain, Filter, MessagesSquare, RefreshCw, ShieldAlert, Sparkles, TrendingUp } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { normalizarTexto } from '../../../shared/utils/formatters.js'
import { obtenerPanorama } from '../services/analisisService.js'
import './PanoramaAnalisis.css'

/** Etapa del análisis de cada docente, con su tono. */
function etapa(fila) {
  if (fila.comentarios === 0 && !fila.estado) return { clave: 'SIN_COMENTARIOS', texto: 'Sin comentarios', tono: 'neutro' }
  if (!fila.estado) return { clave: 'SIN_SOLICITAR', texto: 'Sin solicitar', tono: 'alerta' }
  if (fila.estado === 'EN_COLA' || fila.estado === 'EN_PROCESO') return { clave: 'EN_CURSO', texto: 'En curso', tono: 'cielo' }
  if (fila.estado === 'ERROR') return { clave: 'ERROR', texto: 'Con error', tono: 'coral' }
  if (fila.estado === 'INSUFICIENTE') return { clave: 'INSUFICIENTE', texto: 'Evidencia insuficiente', tono: 'neutro' }
  if (fila.publicado) return { clave: 'PUBLICADO', texto: 'Publicado', tono: 'exito' }
  return { clave: 'REVISION', texto: 'Por revisar', tono: 'violeta' }
}

const ETAPAS = [
  ['SIN_SOLICITAR', 'Sin solicitar'],
  ['EN_CURSO', 'En curso'],
  ['REVISION', 'Por revisar'],
  ['PUBLICADO', 'Publicado'],
  ['INSUFICIENTE', 'Evidencia insuficiente'],
  ['ERROR', 'Con error'],
  ['SIN_COMENTARIOS', 'Sin comentarios'],
]

const TONO_ETAPA = {
  SIN_SOLICITAR: 'alerta',
  EN_CURSO: 'cielo',
  REVISION: 'violeta',
  PUBLICADO: 'exito',
  INSUFICIENTE: 'neutro',
  ERROR: 'coral',
  SIN_COMENTARIOS: 'neutro',
}

const pct = (parte, total) => (total ? Math.round((parte / total) * 100) : 0)

/**
 * Panorama del análisis de comentarios del alcance: avance, balance de fortalezas y oportunidades,
 * calidad de las citas y comentarios filtrados. Pulsar una etapa filtra la lista de docentes.
 */
export function PanoramaAnalisis({ periodoId, onElegirDocente }) {
  const cargar = useCallback(() => obtenerPanorama(periodoId), [periodoId])
  const panorama = useFetch(cargar, [cargar])
  const [filtroEtapa, setFiltroEtapa] = useState('')
  const [busqueda, setBusqueda] = useState('')

  const filas = useMemo(() => (panorama.datos?.docentes ?? []).map((f) => ({ ...f, etapa: etapa(f) })), [panorama.datos])
  const visibles = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return filas.filter(
      (f) =>
        (!filtroEtapa || f.etapa.clave === filtroEtapa) &&
        (!termino || normalizarTexto(f.docente.nombreCompleto).includes(termino)),
    )
  }, [filas, filtroEtapa, busqueda])

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
  const conteoEtapas = ETAPAS.map(([clave, texto]) => ({ clave, texto, cantidad: filas.filter((f) => f.etapa.clave === clave).length }))
  const maxEtapa = Math.max(1, ...conteoEtapas.map((e) => e.cantidad))

  return (
    <div className="panorama">
      <div className="panorama__cifras">
        <div className="panorama__cifra">
          <MessagesSquare size={18} aria-hidden="true" />
          <span>Comentarios recibidos</span>
          <strong>{t.comentarios}</strong>
          <small>{t.conComentarios} de {t.docentes} docentes tienen comentarios</small>
        </div>
        <div className="panorama__cifra panorama__cifra--violeta">
          <Brain size={18} aria-hidden="true" />
          <span>Docentes analizados</span>
          <strong>{t.analizados}</strong>
          <small>{t.pendientesRevision} por revisar · {t.publicados} publicados</small>
        </div>
        <div className="panorama__cifra panorama__cifra--turquesa">
          <BadgeCheck size={18} aria-hidden="true" />
          <span>Citas verificadas</span>
          <strong>{pct(t.citasVerificadas, t.citas)} %</strong>
          <small>{t.citasVerificadas} de {t.citas} citas aparecen tal cual en los comentarios</small>
        </div>
        <div className="panorama__cifra panorama__cifra--coral">
          <ShieldAlert size={18} aria-hidden="true" />
          <span>Comentarios filtrados</span>
          <strong>{t.comentariosFiltrados}</strong>
          <small>Ofensivos o sobre rasgos personales; no llegan al modelo</small>
        </div>
      </div>

      <div className="panorama__rejilla">
        <Panel titulo="Avance del análisis" descripcion="Pulsa una etapa para ver esos docentes." icono={Filter} tono="violeta">
          <ul className="panorama__etapas">
            {conteoEtapas.map((e) => (
              <li key={e.clave}>
                <button
                  type="button"
                  className={`panorama__etapa${filtroEtapa === e.clave ? ' panorama__etapa--activa' : ''}`}
                  aria-pressed={filtroEtapa === e.clave}
                  onClick={() => setFiltroEtapa((actual) => (actual === e.clave ? '' : e.clave))}
                >
                  <span>{e.texto}</span>
                  <span className="panorama__riel" aria-hidden="true">
                    <span className={`panorama__relleno panorama__tono--${TONO_ETAPA[e.clave]}`} style={{ width: `${(e.cantidad / maxEtapa) * 100}%` }} />
                  </span>
                  <strong>{e.cantidad}</strong>
                </button>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel titulo="Lo que dicen los estudiantes" descripcion="Hallazgos vigentes de los análisis completados." icono={Sparkles} tono="turquesa">
          {hallazgos === 0 ? (
            <p className="campo__ayuda">Aún no hay hallazgos. Aparecen al completar el primer análisis.</p>
          ) : (
            <>
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
                  <dd>
                    {t.fortalezas} · {pct(t.fortalezas, hallazgos)} %
                  </dd>
                </div>
                <div>
                  <dt>
                    <span className="panorama__punto panorama__tono--coral" aria-hidden="true" />
                    Oportunidades de mejora
                  </dt>
                  <dd>
                    {t.oportunidades} · {pct(t.oportunidades, hallazgos)} %
                  </dd>
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
                    Descartados por coordinación
                  </dt>
                  <dd>{t.descartados}</dd>
                </div>
              </dl>
              <p className="panorama__nota">
                <TrendingUp size={15} aria-hidden="true" />
                {t.comentariosEnviados} comentarios anonimizados se enviaron al modelo; cada hallazgo cita fragmentos
                que se verifican contra ellos.
              </p>
            </>
          )}
        </Panel>
      </div>

      <Panel
        titulo="Docentes"
        descripcion={filtroEtapa ? `Filtrado: ${ETAPAS.find(([c]) => c === filtroEtapa)[1]}` : 'Elige uno para ver o solicitar su análisis.'}
        icono={Brain}
        acciones={
          filtroEtapa ? (
            <Boton variante="enlace" onClick={() => setFiltroEtapa('')}>
              Quitar filtro
            </Boton>
          ) : null
        }
      >
        <label className="panorama__buscador">
          <span className="solo-lectores">Buscar docente</span>
          <input type="search" className="campo__control" placeholder="Buscar docente" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        </label>
        <ul className="panorama__docentes">
          {visibles.map((f) => (
            <li key={f.docente.idDocente}>
              <div className="panorama__docente">
                <strong>{f.docente.nombreCompleto}</strong>
                <span>
                  {f.docente.nombrePrograma} · {f.comentarios} comentarios
                </span>
              </div>
              <span className={`pildora pildora--${f.etapa.tono}`}>{f.etapa.texto}</span>
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
          {visibles.length === 0 && <li className="campo__ayuda">Ningún docente coincide.</li>}
        </ul>
      </Panel>
    </div>
  )
}
