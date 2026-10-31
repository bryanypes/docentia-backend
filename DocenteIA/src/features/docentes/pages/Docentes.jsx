import { ChevronRight, RefreshCw, Search } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { ETIQUETA_FUENTE, FUENTE_DEL_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota, iniciales, listarFuentes, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { listarSeguimientoDocentes } from '../services/docentesService.js'
import './Docentes.css'

const TODOS = ''

/** Filtros por estado de la evaluación en el periodo activo. */
const ESTADOS = {
  FALTA_MI_NOTA: 'FALTA_MI_NOTA',
  INCOMPLETOS: 'INCOMPLETOS',
  COMPLETOS: 'COMPLETOS',
}

function cumpleEstado(promedio, estado, miFuente) {
  if (!estado) return true
  if (!promedio) return false
  if (estado === ESTADOS.COMPLETOS) return promedio.estado === 'COMPLETO'
  if (estado === ESTADOS.INCOMPLETOS) return promedio.estado === 'INCOMPLETO'
  return promedio.fuentesPendientes.includes(miFuente)
}

/**
 * Una sola etiqueta por fila: el nivel si está completa; si no, cuántas faltan (o que falta la tuya,
 * que es lo accionable). Qué fuentes faltan se ve al pasar el mouse y lo leen los lectores de pantalla.
 */
function EstadoEvaluacion({ promedio, faltaMia }) {
  if (!promedio || promedio.estado === 'COMPLETO') return <EtiquetaNivel promedio={promedio} />
  const faltan = promedio.fuentesPendientes.length
  const detalle = `Falta: ${listarFuentes(promedio.fuentesPendientes)}`
  return (
    <span className={`pildora ${faltaMia ? 'pildora--info' : 'pildora--alerta'}`} title={detalle}>
      {faltaMia ? 'Falta tu calificación' : `Pendiente · faltan ${faltan}`}
      <span className="solo-lectores">. {detalle}</span>
    </span>
  )
}

export default function Docentes() {
  const { usuario } = useSesion()
  const navegar = useNavigate()
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR
  const miFuente = FUENTE_DEL_ROL[usuario.rol]

  const [idFacultad, setIdFacultad] = useState(TODOS)
  const [programa, setPrograma] = useState(TODOS)
  // El tablero enlaza aquí con ?estado=FALTA_MI_NOTA para abrir la lista ya filtrada.
  const [parametros] = useSearchParams()
  const [estado, setEstado] = useState(() =>
    Object.values(ESTADOS).includes(parametros.get('estado')) ? parametros.get('estado') : TODOS,
  )
  const [busqueda, setBusqueda] = useState('')

  const cargarDocentes = useCallback(() => listarSeguimientoDocentes(idFacultad || null), [idFacultad])
  const seguimiento = useFetch(cargarDocentes, [cargarDocentes])
  const facultades = useFetch(() => (esVicerrector ? listarFacultades() : Promise.resolve([])), [esVicerrector])

  const periodo = seguimiento.datos?.periodo ?? null
  const lista = useMemo(() => seguimiento.datos?.docentes ?? [], [seguimiento.datos])
  const programas = useMemo(() => [...new Set(lista.map(({ docente }) => docente.nombrePrograma))].sort(), [lista])
  const faltanMias = useMemo(
    () => lista.filter(({ promedio }) => cumpleEstado(promedio, ESTADOS.FALTA_MI_NOTA, miFuente)).length,
    [lista, miFuente],
  )

  const filas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return lista.filter(
      ({ docente, promedio }) =>
        (!programa || docente.nombrePrograma === programa) &&
        cumpleEstado(promedio, estado, miFuente) &&
        (!termino ||
          normalizarTexto(`${docente.nombreCompleto} ${docente.correo} ${docente.nombrePrograma}`).includes(termino)),
    )
  }, [lista, programa, estado, miFuente, busqueda])

  function limpiarFiltros() {
    setBusqueda('')
    setPrograma(TODOS)
    setEstado(TODOS)
    setIdFacultad(TODOS)
  }

  const alcance = esVicerrector ? 'toda la universidad' : (lista[0]?.docente.nombreFacultad ?? 'tu facultad')
  const hayFiltros = Boolean(busqueda || programa || estado || idFacultad)

  return (
    <>
      <Encabezado
        miga={`${esVicerrector ? 'Vicerrectoría' : 'Coordinación'} · Docentes`}
        titulo={esVicerrector ? 'Docentes de la universidad' : 'Docentes de la facultad'}
        subtitulo="Filtra y abre el expediente de cada docente. La calificación final siempre la firma un humano."
      />

      <div className="pagina">
        {periodo && faltanMias > 0 && estado !== ESTADOS.FALTA_MI_NOTA && (
          <div className="docentes__pendientes" role="status">
            <img src={robotSenala} alt="" className="docentes__pendientes-robot" />
            <span className="docentes__pendientes-texto">
              <strong>
                {faltanMias === 1 ? '1 docente espera' : `${faltanMias} docentes esperan`} tu calificación
              </strong>{' '}
              de {ETIQUETA_FUENTE[miFuente].toLowerCase()} en {periodo.nombre}.
            </span>
            <Boton variante="contorno" onClick={() => setEstado(ESTADOS.FALTA_MI_NOTA)}>
              Ver solo esos
            </Boton>
          </div>
        )}

        <div className="docentes__filtros">
          <label className="docentes__buscador">
            <Search size={16} aria-hidden="true" />
            <span className="solo-lectores">Buscar docente</span>
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar docente, correo o programa"
            />
          </label>

          {esVicerrector && (
            <label className="docentes__selector">
              <span className="docentes__etiqueta">Facultad</span>
              <select
                value={idFacultad}
                onChange={(e) => {
                  setIdFacultad(e.target.value)
                  setPrograma(TODOS)
                }}
              >
                <option value={TODOS}>Todas</option>
                {(facultades.datos ?? []).map((f) => (
                  <option key={f.idFacultad} value={f.idFacultad}>
                    {f.nombre}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="docentes__selector">
            <span className="docentes__etiqueta">Programa</span>
            <select value={programa} onChange={(e) => setPrograma(e.target.value)}>
              <option value={TODOS}>Todos</option>
              {programas.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </label>

          <label className="docentes__selector">
            <span className="docentes__etiqueta">Evaluación</span>
            <select value={estado} onChange={(e) => setEstado(e.target.value)} disabled={!periodo}>
              <option value={TODOS}>Todas</option>
              <option value={ESTADOS.FALTA_MI_NOTA}>Falta mi calificación</option>
              <option value={ESTADOS.INCOMPLETOS}>Incompletas</option>
              <option value={ESTADOS.COMPLETOS}>Completas</option>
            </select>
          </label>

          <span className="docentes__conteo" aria-live="polite">
            {filas.length} de {lista.length}
          </span>
        </div>

        <span className="docentes__alcance">
          Alcance: {alcance}
          {periodo ? ` · Periodo ${periodo.nombre}` : ' · Sin periodo activo'}
        </span>

        {seguimiento.cargando && <Cargando texto="Cargando docentes" bloques={1} />}

        {seguimiento.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar los docentes" mensaje={seguimiento.error.message}>
            <Boton icono={RefreshCw} onClick={seguimiento.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}

        {!seguimiento.cargando && !seguimiento.error && lista.length === 0 && (
          <EstadoVista
            titulo="No hay docentes en tu alcance"
            mensaje="Cuando se registren docentes activos en los programas de la facultad aparecerán aquí."
          />
        )}

        {!seguimiento.cargando && !seguimiento.error && lista.length > 0 && (
          <div className="docentes__tabla-contenedor">
            {filas.length > 0 ? (
              <div className="docentes__desplazable">
                <table className="docentes__tabla">
                  <caption className="solo-lectores">Docentes</caption>
                  <thead>
                    <tr>
                      <th scope="col">Docente</th>
                      <th scope="col">Programa</th>
                      <th scope="col">Evaluación {periodo?.nombre ?? ''}</th>
                      <th scope="col" className="tabla__numero">
                        Promedio
                      </th>
                      <th scope="col">
                        <span className="solo-lectores">Abrir expediente</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {filas.map(({ docente: d, promedio }) => {
                      const faltaMia = cumpleEstado(promedio, ESTADOS.FALTA_MI_NOTA, miFuente)
                      return (
                        <tr key={d.idDocente} onClick={() => navegar(`/docentes/${d.idDocente}`)}>
                          <td>
                            <div className="docentes__persona">
                              <span className="docentes__avatar" aria-hidden="true">
                                {iniciales(d.nombreCompleto)}
                              </span>
                              <span>
                                <Link
                                  to={`/docentes/${d.idDocente}`}
                                  className="docentes__nombre"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {d.nombreCompleto}
                                </Link>
                                <span className="docentes__correo">{d.correo}</span>
                              </span>
                            </div>
                          </td>
                          <td>
                            {d.nombrePrograma}
                            {esVicerrector && <span className="docentes__correo">{d.nombreFacultad}</span>}
                          </td>
                          <td>
                            <EstadoEvaluacion promedio={promedio} faltaMia={faltaMia} />
                          </td>
                          <td className="tabla__numero docentes__promedio">
                            {promedio?.estado === 'COMPLETO' ? formatearNota(promedio.valor) : '—'}
                          </td>
                          <td className="docentes__flecha" aria-hidden="true">
                            <ChevronRight size={18} />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <EstadoVista
                titulo={
                  estado === ESTADOS.FALTA_MI_NOTA ? 'No te falta calificar a nadie' : 'Ningún docente coincide con el filtro'
                }
                mensaje={
                  estado === ESTADOS.FALTA_MI_NOTA
                    ? 'Todos los docentes de este filtro ya tienen tu calificación.'
                    : 'Prueba con otro filtro o limpia la búsqueda.'
                }
              >
                {hayFiltros && (
                  <Boton variante="contorno" onClick={limpiarFiltros}>
                    Limpiar filtros
                  </Boton>
                )}
              </EstadoVista>
            )}
          </div>
        )}
      </div>
    </>
  )
}
