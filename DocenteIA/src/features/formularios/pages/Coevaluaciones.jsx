import { RefreshCw, Search, SlidersHorizontal } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { iniciales, normalizarTexto } from '../../../shared/utils/formatters.js'
import { obtenerPendientesCoevaluacion } from '../services/formulariosService.js'
import '../components/FormularioInstrumento.css'
import './Coevaluaciones.css'

const ESTADO = {
  SIN_INICIAR: { etiqueta: 'Sin iniciar', tono: 'neutro', accion: 'editar', texto: 'Responder' },
  BORRADOR: { etiqueta: 'En borrador', tono: 'violeta', accion: 'editar', texto: 'Continuar' },
  ENVIADA: { etiqueta: 'Enviada', tono: 'turquesa', accion: 'ver', texto: 'Ver coevaluación' },
}
const TODOS = ''

/**
 * Coevaluaciones de la coordinación: chips de estado con su conteo, una barra de filtros pegada a la
 * lista y una tarjeta por docente con su estado, avance y acción.
 */
export default function Coevaluaciones() {
  const { datos, error, cargando, recargar } = useFetch(obtenerPendientesCoevaluacion)
  const [estado, setEstado] = useState(TODOS)
  const [programa, setPrograma] = useState(TODOS)
  const [busqueda, setBusqueda] = useState('')

  const docentes = useMemo(() => datos?.docentes ?? [], [datos])
  const conteo = useMemo(
    () => Object.fromEntries(Object.keys(ESTADO).map((clave) => [clave, docentes.filter((d) => d.estado === clave).length])),
    [docentes],
  )
  const programas = useMemo(() => [...new Set(docentes.map((d) => d.programa))].sort(), [docentes])
  const visibles = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return docentes.filter(
      (d) =>
        (!estado || d.estado === estado) &&
        (!programa || d.programa === programa) &&
        (!termino || normalizarTexto(d.nombreDocente).includes(termino)),
    )
  }, [docentes, estado, programa, busqueda])
  const hayFiltros = Boolean(estado || programa || busqueda)

  return (
    <>
      <Encabezado
        miga="Coordinación · Coevaluaciones"
        titulo="Coevaluaciones"
        subtitulo={datos ? `Periodo ${datos.periodo.nombre}. ${datos.pendientes} pendientes.` : 'Docentes de tu facultad.'}
      />
      <div className="pagina">
        {cargando && !datos && <Cargando texto="Cargando coevaluaciones" bloques={1} />}
        {error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar las coevaluaciones" mensaje={error.message}>
            <Boton icono={RefreshCw} onClick={recargar}>
              Intentar de nuevo
            </Boton>
          </EstadoVista>
        )}
        {!cargando && !error && !datos && (
          <EstadoVista titulo="No hay un periodo activo" mensaje="La vicerrectoría debe activar uno." />
        )}
        {datos?.docentes.length === 0 && <EstadoVista titulo="Sin docentes" mensaje="Tu facultad no tiene docentes activos." />}

        {docentes.length > 0 && (
          <section className="coevaluaciones" aria-labelledby="coevaluaciones-titulo">
            <div className="coevaluaciones__cabecera">
              <h2 id="coevaluaciones-titulo">Docentes de la facultad</h2>
              <div className="coevaluaciones__chips" role="group" aria-label="Filtrar por estado">
                <button
                  type="button"
                  className={`coevaluaciones__chip${!estado ? ' coevaluaciones__chip--activo' : ''}`}
                  aria-pressed={!estado}
                  onClick={() => setEstado(TODOS)}
                >
                  Todos <strong>{docentes.length}</strong>
                </button>
                {Object.entries(ESTADO).map(([clave, { etiqueta, tono }]) => (
                  <button
                    key={clave}
                    type="button"
                    className={`coevaluaciones__chip coevaluaciones__chip--${tono}${estado === clave ? ' coevaluaciones__chip--activo' : ''}`}
                    aria-pressed={estado === clave}
                    onClick={() => setEstado(estado === clave ? TODOS : clave)}
                  >
                    {etiqueta} <strong>{conteo[clave]}</strong>
                  </button>
                ))}
              </div>
            </div>

            <div className="coevaluaciones__filtros" role="search" aria-label="Filtrar coevaluaciones">
              <span className="coevaluaciones__filtros-rotulo">
                <SlidersHorizontal size={15} aria-hidden="true" />
                Filtrar
              </span>
              <label className="coevaluaciones__buscador">
                <Search size={16} aria-hidden="true" />
                <span className="solo-lectores">Buscar docente</span>
                <input type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar docente" />
              </label>
              <label className="coevaluaciones__selector">
                <span>Programa</span>
                <select className="campo__control" value={programa} onChange={(e) => setPrograma(e.target.value)}>
                  <option value={TODOS}>Todos los programas</option>
                  {programas.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </label>
              <label className="coevaluaciones__selector">
                <span>Estado</span>
                <select className="campo__control" value={estado} onChange={(e) => setEstado(e.target.value)}>
                  <option value={TODOS}>Todos</option>
                  {Object.entries(ESTADO).map(([clave, { etiqueta }]) => (
                    <option key={clave} value={clave}>
                      {etiqueta}
                    </option>
                  ))}
                </select>
              </label>
              <span className="coevaluaciones__conteo" aria-live="polite">
                {visibles.length} de {docentes.length}
              </span>
              {hayFiltros && (
                <Boton
                  variante="enlace"
                  onClick={() => {
                    setEstado(TODOS)
                    setPrograma(TODOS)
                    setBusqueda('')
                  }}
                >
                  Limpiar
                </Boton>
              )}
            </div>

            {visibles.length === 0 ? (
              <EstadoVista titulo="Ningún docente coincide" mensaje="Prueba con otro estado, programa o nombre." />
            ) : (
              <ul className="coevaluaciones__tarjetas">
                {visibles.map((d) => {
                  const info = ESTADO[d.estado]
                  const avance = datos.itemsCoevaluacion ? (d.respondidos / datos.itemsCoevaluacion) * 100 : 0
                  return (
                    <li key={d.idDocente} className={`coevaluacion coevaluacion--${info.tono}`}>
                      <div className="coevaluacion__persona">
                        <span className="coevaluacion__avatar" aria-hidden="true">
                          {iniciales(d.nombreDocente)}
                        </span>
                        <div>
                          <strong>{d.nombreDocente}</strong>
                          <span>{d.programa}</span>
                        </div>
                      </div>
                      <span className={`coevaluacion__estado coevaluacion__estado--${info.tono}`}>{info.etiqueta}</span>
                      <div className="coevaluacion__avance">
                        <span className="coevaluacion__riel" aria-hidden="true">
                          <span style={{ width: `${avance}%` }} />
                        </span>
                        <span>
                          {d.respondidos} de {datos.itemsCoevaluacion}
                        </span>
                      </div>
                      <div className="acciones-fila coevaluacion__acciones">
                        <BotonAccion tipo={info.accion} texto={info.texto} nombre={d.nombreDocente} a={`/coevaluaciones/${d.idDocente}`} />
                        <BotonAccion tipo="ver" texto="Ver expediente" nombre={d.nombreDocente} a={`/docentes/${d.idDocente}`} />
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>
        )}
      </div>
    </>
  )
}
