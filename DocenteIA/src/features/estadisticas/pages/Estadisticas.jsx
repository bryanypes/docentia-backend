import {
  CalendarCheck,
  ChartColumnBig,
  Download,
  Eye,
  FileSignature,
  Filter,
  GraduationCap,
  Layers,
  RefreshCw,
  Route,
  Search,
  Sigma,
  Users,
  X,
} from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { EtiquetaNivel } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_EVALUADOR, NIVELES, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarSeguimientoDocentes } from '../../docentes/services/docentesService.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { obtenerConfiguracionEvaluacion } from '../../kpis/services/configuracionService.js'
import { ChipProceso } from '../../proceso/components/ChipProceso.jsx'
import { ESTADOS_PROCESO } from '../../proceso/estados.js'
import { listarProcesos } from '../../proceso/services/procesoService.js'
import { descargarXlsx } from '../../../shared/utils/xlsx.js'
import { calcular, filtrar, unirDatos } from '../estadisticas.js'
import { hojasDelReporte, nombreDelReporte } from '../reporteExcel.js'
import './Estadisticas.css'

const SIN_FILTROS = { facultad: '', programa: '', nivel: '', estado: '', busqueda: '' }
const TONO_NIVEL = { MUY_ALTO: 'muy-alto', ALTO: 'alto', ACEPTABLE: 'aceptable', BAJO: 'bajo', MUY_BAJO: 'muy-bajo' }
// Docentes que se muestran de entrada y en cada «Mostrar más».
const TANDA_FILAS = 10

const porcentaje = (valor) => (valor === null || valor === undefined ? '—' : `${valor} %`)
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`

/** Tarjeta de cifra principal. */
function Cifra({ icono: Icono, rotulo, valor, detalle, tono = 'azul' }) {
  return (
    <div className={`estadisticas__cifra estadisticas__cifra--${tono}`}>
      <span className="estadisticas__cifra-icono" aria-hidden="true">
        <Icono size={18} />
      </span>
      <span className="estadisticas__cifra-rotulo">{rotulo}</span>
      <strong className="estadisticas__cifra-valor">{valor}</strong>
      {detalle && <span className="estadisticas__cifra-detalle">{detalle}</span>}
    </div>
  )
}

/**
 * Barra horizontal que también filtra: al pulsarla, toda la vista se recalcula con ese valor.
 * Pulsarla otra vez quita el filtro.
 */
function BarraFiltro({ etiqueta, valor, maximo, texto, tono, activa, onClick, detalle }) {
  const ancho = maximo ? Math.max(valor > 0 ? 3 : 0, (valor / maximo) * 100) : 0
  return (
    <li>
      <button
        type="button"
        className={`estadisticas__barra${activa ? ' estadisticas__barra--activa' : ''}`}
        aria-pressed={activa}
        title={detalle}
        onClick={onClick}
      >
        <span className="estadisticas__barra-etiqueta">{etiqueta}</span>
        <span className="estadisticas__barra-riel" aria-hidden="true">
          <span className={`estadisticas__barra-relleno estadisticas__tono--${tono}`} style={{ width: `${ancho}%` }} />
        </span>
        <span className="estadisticas__barra-valor">{texto}</span>
      </button>
    </li>
  )
}

function FiltroActivo({ texto, onQuitar }) {
  return (
    <li>
      <button type="button" className="estadisticas__chip" onClick={onQuitar}>
        {texto}
        <X size={14} aria-hidden="true" />
        <span className="solo-lectores">Quitar filtro</span>
      </button>
    </li>
  )
}

/**
 * Estadísticas de coordinación y vicerrectoría. Una sola fuente de filas filtradas alimenta todas
 * las gráficas: cambiar un filtro, o pulsar una barra, actualiza la vista completa.
 */
export default function Estadisticas() {
  const { usuario } = useSesion()
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR
  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [mostrar, setMostrar] = useState(TANDA_FILAS)
  const agruparPor = esVicerrector && !filtros.facultad ? 'nombreFacultad' : 'nombrePrograma'

  const cargarSeguimiento = useCallback(() => listarSeguimientoDocentes(null), [])
  const seguimiento = useFetch(cargarSeguimiento, [cargarSeguimiento])
  const cargarProcesos = useCallback(() => listarProcesos(null), [])
  const procesos = useFetch(cargarProcesos, [cargarProcesos])
  const facultades = useFetch(() => (esVicerrector ? listarFacultades() : Promise.resolve([])), [esVicerrector])
  const configuracion = useFetch(obtenerConfiguracionEvaluacion)
  const umbral = Number(configuracion.datos?.umbralCompromisos ?? 3.8)

  const filas = useMemo(
    () => unirDatos(seguimiento.datos?.docentes ?? [], procesos.datos ?? []),
    [seguimiento.datos, procesos.datos],
  )
  // Los programas se ofrecen según la facultad elegida, para no listar combinaciones vacías.
  const programas = useMemo(
    () =>
      [
        ...new Set(
          filas
            .filter((f) => !filtros.facultad || String(f.docente.idFacultad) === filtros.facultad)
            .map((f) => f.docente.nombrePrograma),
        ),
      ].sort(),
    [filas, filtros.facultad],
  )
  const visibles = useMemo(() => filtrar(filas, filtros), [filas, filtros])
  const r = useMemo(() => calcular(visibles, { umbral, agruparPor }), [visibles, umbral, agruparPor])

  const cambiar = (campo, valor) =>
    setFiltros((actual) => ({
      ...actual,
      [campo]: actual[campo] === valor ? '' : valor,
      ...(campo === 'facultad' ? { programa: '' } : {}),
    }))
  const fijar = (campo, valor) => setFiltros((actual) => ({ ...actual, [campo]: valor, ...(campo === 'facultad' ? { programa: '' } : {}) }))
  const hayFiltros = Object.values(filtros).some(Boolean)
  const nombreFacultad = (id) => (facultades.datos ?? []).find((f) => String(f.idFacultad) === String(id))?.nombre ?? id

  const encabezado = (
    <Encabezado
      miga={`${esVicerrector ? 'Vicerrectoría' : 'Coordinación'} · Estadísticas`}
      titulo="Estadísticas de la evaluación"
      subtitulo="Filtra o pulsa cualquier barra: todas las gráficas se recalculan con la misma selección."
    />
  )

  if (seguimiento.cargando || procesos.cargando) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <Cargando texto="Calculando las estadísticas" bloques={3} />
        </div>
      </>
    )
  }
  const error = seguimiento.error ?? procesos.error
  if (error) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <EstadoVista tipo="error" titulo="No fue posible cargar las estadísticas" mensaje={error.message}>
            <Boton
              icono={RefreshCw}
              onClick={() => {
                seguimiento.recargar()
                procesos.recargar()
              }}
            >
              Intentar de nuevo
            </Boton>
          </EstadoVista>
        </div>
      </>
    )
  }
  if (!seguimiento.datos?.periodo) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <EstadoVista titulo="No hay un periodo académico activo" mensaje="Las estadísticas se calculan sobre el periodo activo." />
        </div>
      </>
    )
  }

  const maxEstado = Math.max(1, ...r.porEstado.map((e) => e.cantidad))
  const maxHistograma = Math.max(1, ...r.histograma.map((h) => h.cantidad))
  const maxPendiente = Math.max(1, r.total)
  const nombreGrupo = agruparPor === 'nombreFacultad' ? 'facultad' : 'programa'

  function descargar() {
    const periodo = seguimiento.datos.periodo.nombre
    const descripcionFiltros = [
      filtros.facultad && `Facultad: ${nombreFacultad(filtros.facultad)}`,
      filtros.programa && `Programa: ${filtros.programa}`,
      filtros.nivel && `Desempeño: ${NIVELES.find((n) => n.codigo === filtros.nivel)?.etiqueta}`,
      filtros.estado && `Proceso: ${ESTADOS_PROCESO[filtros.estado].etiqueta}`,
      filtros.busqueda && `Búsqueda: “${filtros.busqueda}”`,
    ]
      .filter(Boolean)
      .join(' · ')
    descargarXlsx(nombreDelReporte(periodo), hojasDelReporte({ filas: visibles, r, periodo, descripcionFiltros, nombreGrupo }))
  }

  return (
    <>
      {encabezado}
      <div className="pagina estadisticas">
        <section className="estadisticas__filtros" aria-label="Filtros de las estadísticas">
          <div className="estadisticas__campos">
            <label className="estadisticas__buscador">
              <Search size={16} aria-hidden="true" />
              <span className="solo-lectores">Buscar docente</span>
              <input
                type="search"
                placeholder="Buscar docente"
                value={filtros.busqueda}
                onChange={(e) => fijar('busqueda', e.target.value)}
              />
            </label>
            {esVicerrector && (
              <label className="estadisticas__campo">
                <span>Facultad</span>
                <select className="campo__control" value={filtros.facultad} onChange={(e) => fijar('facultad', e.target.value)}>
                  <option value="">Todas</option>
                  {(facultades.datos ?? []).map((f) => (
                    <option key={f.idFacultad} value={f.idFacultad}>
                      {f.nombre}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label className="estadisticas__campo">
              <span>Programa</span>
              <select className="campo__control" value={filtros.programa} onChange={(e) => fijar('programa', e.target.value)}>
                <option value="">Todos</option>
                {programas.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </label>
            <label className="estadisticas__campo">
              <span>Desempeño</span>
              <select className="campo__control" value={filtros.nivel} onChange={(e) => fijar('nivel', e.target.value)}>
                <option value="">Todos</option>
                {NIVELES.map((n) => (
                  <option key={n.codigo} value={n.codigo}>
                    {n.etiqueta}
                  </option>
                ))}
              </select>
            </label>
            <label className="estadisticas__campo">
              <span>Proceso</span>
              <select className="campo__control" value={filtros.estado} onChange={(e) => fijar('estado', e.target.value)}>
                <option value="">Todos</option>
                {Object.entries(ESTADOS_PROCESO).map(([clave, info]) => (
                  <option key={clave} value={clave}>
                    {info.etiqueta}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="estadisticas__resumen-filtros">
            <span aria-live="polite">
              <Filter size={14} aria-hidden="true" /> {plural(r.total, 'docente', 'docentes')} en la selección
            </span>
            <Boton
              variante="contorno"
              icono={Download}
              className="estadisticas__descargar"
              disabled={r.total === 0}
              onClick={descargar}
              title="Libro con el resumen, los docentes y cada gráfica; las cifras se recalculan al filtrar en Excel"
            >
              Descargar Excel
            </Boton>
            {hayFiltros && (
              <ul className="estadisticas__chips" aria-label="Filtros activos">
                {filtros.facultad && <FiltroActivo texto={nombreFacultad(filtros.facultad)} onQuitar={() => fijar('facultad', '')} />}
                {filtros.programa && <FiltroActivo texto={filtros.programa} onQuitar={() => fijar('programa', '')} />}
                {filtros.nivel && (
                  <FiltroActivo texto={NIVELES.find((n) => n.codigo === filtros.nivel)?.etiqueta} onQuitar={() => fijar('nivel', '')} />
                )}
                {filtros.estado && <FiltroActivo texto={ESTADOS_PROCESO[filtros.estado].etiqueta} onQuitar={() => fijar('estado', '')} />}
                {filtros.busqueda && <FiltroActivo texto={`“${filtros.busqueda}”`} onQuitar={() => fijar('busqueda', '')} />}
                <li>
                  <Boton variante="enlace" onClick={() => setFiltros(SIN_FILTROS)}>
                    Limpiar todo
                  </Boton>
                </li>
              </ul>
            )}
          </div>
        </section>

        <div className="estadisticas__cifras">
          <Cifra icono={Users} rotulo="Docentes" valor={r.total} detalle={`${r.completos} con evaluación completa`} />
          <Cifra icono={Layers} rotulo="Avance" valor={porcentaje(r.avance)} detalle="Con las tres evaluaciones" tono="turquesa" />
          <Cifra
            icono={Sigma}
            rotulo="Promedio"
            valor={r.promedioGeneral === null ? '—' : formatearNota(r.promedioGeneral)}
            detalle={r.mediana === null ? 'Sin evaluaciones completas' : `Mediana ${formatearNota(r.mediana)}`}
            tono="violeta"
          />
          <Cifra
            icono={GraduationCap}
            rotulo="Estudiantes que respondieron"
            valor={porcentaje(r.cobertura.porcentaje)}
            detalle={`${r.cobertura.respondidas} de ${r.cobertura.esperadas} matrículas`}
            tono="cielo"
          />
          <Cifra
            icono={CalendarCheck}
            rotulo="Reuniones realizadas"
            valor={r.citas.asistieron}
            detalle={`${r.citas.agendadas} citas activas · ${r.citas.confirmadas} confirmadas`}
            tono="coral"
          />
          <Cifra
            icono={FileSignature}
            rotulo="Actas firmadas"
            valor={r.actas.firmadas}
            detalle={`${r.actas.compromiso} de compromiso · ${r.actas.retroalimentacion} de retroalimentación`}
            tono="rosa"
          />
        </div>

        {r.total === 0 ? (
          <EstadoVista titulo="Ningún docente coincide" mensaje="Quita algún filtro para ver resultados.">
            <Boton variante="contorno" onClick={() => setFiltros(SIN_FILTROS)}>
              Limpiar filtros
            </Boton>
          </EstadoVista>
        ) : (
          <>
          <div className="estadisticas__mosaico">
            <Panel titulo="Nivel de desempeño" descripcion="Docentes con evaluación completa. Pulsa un nivel para filtrar." icono={ChartColumnBig} tono="violeta">
              <div className="estadisticas__apilada" aria-hidden="true">
                {r.porNivel
                  .filter((n) => n.cantidad > 0)
                  .map((n) => (
                    <span key={n.codigo} className={`estadisticas__tono--${TONO_NIVEL[n.codigo]}`} style={{ flexGrow: n.cantidad }} />
                  ))}
              </div>
              <ul className="estadisticas__barras">
                {r.porNivel.map((n) => (
                  <BarraFiltro
                    key={n.codigo}
                    etiqueta={n.etiqueta}
                    valor={n.cantidad}
                    maximo={Math.max(1, r.completos)}
                    texto={`${n.cantidad} · ${n.porcentaje} %`}
                    tono={TONO_NIVEL[n.codigo]}
                    activa={filtros.nivel === n.codigo}
                    onClick={() => cambiar('nivel', n.codigo)}
                  />
                ))}
              </ul>
              <p className="estadisticas__nota">
                {plural(r.bajoUmbral, 'docente', 'docentes')} bajo {formatearNota(r.umbral)}: su acta es de compromiso.
              </p>
            </Panel>

            <Panel titulo="Avance del proceso" descripcion="Dónde está cada docente, de las evaluaciones al acta firmada." icono={Route} tono="turquesa">
              <ul className="estadisticas__barras">
                {r.porEstado.map((e) => (
                  <BarraFiltro
                    key={e.estado}
                    etiqueta={ESTADOS_PROCESO[e.estado].etiqueta}
                    valor={e.cantidad}
                    maximo={maxEstado}
                    texto={e.cantidad}
                    tono={ESTADOS_PROCESO[e.estado].tono}
                    activa={filtros.estado === e.estado}
                    detalle={ESTADOS_PROCESO[e.estado].ayuda}
                    onClick={() => cambiar('estado', e.estado)}
                  />
                ))}
              </ul>
            </Panel>

            <Panel titulo="Distribución de promedios" descripcion="Cuántos docentes hay en cada tramo de medio punto." icono={Sigma} tono="cielo">
              <div className="estadisticas__histograma" role="list" aria-label="Docentes por tramo de promedio">
                {r.histograma.map((h) => (
                  <div key={h.etiqueta} className="estadisticas__columna" role="listitem" aria-label={`${h.etiqueta}: ${plural(h.cantidad, 'docente', 'docentes')}`}>
                    <span className="estadisticas__columna-valor">{h.cantidad || ''}</span>
                    <span className="estadisticas__columna-riel" aria-hidden="true">
                      <span
                        className={`estadisticas__columna-relleno${h.hasta <= r.umbral ? ' estadisticas__columna-relleno--bajo' : ''}`}
                        style={{ height: `${(h.cantidad / maxHistograma) * 100}%` }}
                      />
                    </span>
                    <span className="estadisticas__columna-etiqueta">{h.etiqueta}</span>
                  </div>
                ))}
              </div>
            </Panel>

            <Panel titulo="Calificaciones que faltan" descripcion="Docentes a los que les falta cada evaluador." icono={Layers} tono="coral">
              <ul className="estadisticas__barras">
                {r.pendientesPorEvaluador.map((p) => (
                  <BarraFiltro
                    key={p.evaluador}
                    etiqueta={ETIQUETA_EVALUADOR[p.evaluador]}
                    valor={p.cantidad}
                    maximo={maxPendiente}
                    texto={`${p.cantidad} de ${r.total}`}
                    tono="coral"
                    activa={false}
                    onClick={() => fijar('estado', p.evaluador === 'AUTOEVALUACION' ? 'PENDIENTE_AUTOEVALUACION' : 'EVALUACIONES_EN_CURSO')}
                  />
                ))}
              </ul>
              <p className="estadisticas__nota">Pulsa una fuente para ver los docentes en esa etapa.</p>
            </Panel>
          </div>
          <div className="estadisticas__rejilla">

            <Panel
              className="estadisticas__ancho"
              titulo={`Comparación por ${nombreGrupo}`}
              descripcion={`Promedio, avance y respuesta de estudiantes en cada ${nombreGrupo}. Pulsa una fila para filtrar.`}
              icono={ChartColumnBig}
              tono="rosa"
            >
              <div className="tabla-contenedor">
                <table className="tabla tabla--apilable estadisticas__tabla">
                  <thead>
                    <tr>
                      <th scope="col">{nombreGrupo === 'facultad' ? 'Facultad' : 'Programa'}</th>
                      <th scope="col">Promedio</th>
                      <th scope="col">Avance</th>
                      <th scope="col">Estudiantes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {r.porGrupo.map((g) => {
                      const filtro = nombreGrupo === 'facultad'
                        ? () => fijar('facultad', String(visibles.find((f) => f.docente.nombreFacultad === g.nombre)?.docente.idFacultad ?? ''))
                        : () => cambiar('programa', g.nombre)
                      return (
                        <tr key={g.nombre}>
                          <th scope="row">
                            <button type="button" className="estadisticas__grupo" onClick={filtro} aria-pressed={filtros.programa === g.nombre}>
                              {g.nombre}
                              <span>{plural(g.docentes, 'docente', 'docentes')}</span>
                            </button>
                          </th>
                          <td data-etiqueta="Promedio">
                            <Medidor valor={g.promedio} maximo={5} texto={g.promedio === null ? '—' : formatearNota(g.promedio)} tono="violeta" />
                          </td>
                          <td data-etiqueta="Avance">
                            <Medidor valor={g.avance} maximo={100} texto={`${g.avance} %`} tono="turquesa" />
                          </td>
                          <td data-etiqueta="Estudiantes">
                            <Medidor valor={g.cobertura} maximo={100} texto={porcentaje(g.cobertura)} tono="cielo" />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </Panel>

            <Panel className="estadisticas__ancho" titulo="Docentes de la selección" descripcion="La lista cambia con los filtros." icono={Users}>
              <div className="tabla-contenedor">
                <table className="tabla tabla--apilable estadisticas__tabla estadisticas__docentes">
                  <thead>
                    <tr>
                      <th scope="col">Docente</th>
                      <th scope="col">Programa</th>
                      <th scope="col">Desempeño</th>
                      <th scope="col">Proceso</th>
                      <th scope="col">
                        <span className="solo-lectores">Acciones</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibles.slice(0, mostrar).map(({ docente, promedio, proceso }) => (
                      <tr key={docente.idDocente}>
                        <td className="tabla__fuerte">{docente.nombreCompleto}</td>
                        <td data-etiqueta="Programa">{docente.nombrePrograma}</td>
                        <td data-etiqueta="Desempeño">
                          <EtiquetaNivel promedio={promedio} />
                        </td>
                        <td data-etiqueta="Proceso">{proceso ? <ChipProceso estado={proceso.estado} /> : '—'}</td>
                        <td>
                          <Link
                            to={`/docentes/${docente.idDocente}`}
                            className="boton boton--contorno estadisticas__ver"
                            title={`Ver información de ${docente.nombreCompleto}`}
                          >
                            <Eye size={16} aria-hidden="true" />
                            <span className="estadisticas__ver-texto">Ver información</span>
                            <span className="solo-lectores"> de {docente.nombreCompleto}</span>
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {visibles.length > TANDA_FILAS && (
                <div className="estadisticas__mas">
                  <span>
                    Mostrando {Math.min(mostrar, visibles.length)} de {visibles.length}
                  </span>
                  {mostrar < visibles.length ? (
                    <Boton variante="contorno" onClick={() => setMostrar((n) => n + TANDA_FILAS)}>
                      Mostrar {Math.min(TANDA_FILAS, visibles.length - mostrar)} más
                    </Boton>
                  ) : (
                    <Boton variante="enlace" onClick={() => setMostrar(TANDA_FILAS)}>
                      Mostrar menos
                    </Boton>
                  )}
                </div>
              )}
            </Panel>
          </div>
          </>
        )}
      </div>
    </>
  )
}

function Medidor({ valor, maximo, texto, tono }) {
  const ancho = valor === null || valor === undefined ? 0 : Math.min(100, (valor / maximo) * 100)
  return (
    <span className="estadisticas__medidor">
      <span className="estadisticas__medidor-riel" aria-hidden="true">
        <span className={`estadisticas__barra-relleno estadisticas__tono--${tono}`} style={{ width: `${ancho}%` }} />
      </span>
      <span className="estadisticas__medidor-texto">{texto}</span>
    </span>
  )
}
