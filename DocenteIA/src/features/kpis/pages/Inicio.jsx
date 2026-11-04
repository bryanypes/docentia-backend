import { ArrowRight, Award, ChartColumnBig, ClipboardList, RefreshCw, RotateCcw, SlidersHorizontal, UserCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCallback, useMemo, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { Portada } from '../../../shared/components/ui/Portada/Portada.jsx'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { ETAPAS, FUENTE_DEL_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearEntero, formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarSeguimientoDocentes } from '../../docentes/services/docentesService.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarProgramas } from '../../programas/services/programasService.js'
import { PanelAtencion, PanelAvance, PanelHonor, PanelPorGrupo } from '../components/PanelesSeguimiento.jsx'
import { AsistenteResumen } from '../components/AsistenteResumen.jsx'
import { TarjetaKpi, TarjetaPromedio } from '../components/TarjetaKpi.jsx'
import { LineaTiempo } from '../../periodos/components/LineaTiempo.jsx'
import { analizarSeguimiento } from '../seguimiento.js'
import { obtenerConfiguracionEvaluacion } from '../services/configuracionService.js'
import { obtenerKpis } from '../services/kpisService.js'
import './Inicio.css'

const ACTIVO = ''
const TODAS = ''
const TODOS = ''

export default function Inicio() {
  const { usuario } = useSesion()
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR

  const [periodoId, setPeriodoId] = useState(ACTIVO)
  const [facultadId, setFacultadId] = useState(TODAS)
  const [programaId, setProgramaId] = useState(TODOS)

  const periodos = useFetch(listarPeriodos)
  const facultades = useFetch(listarFacultades)
  // Las carreras se ofrecen según la facultad: la elegida por la vicerrectoría o la del coordinador.
  const facultadPrograma = esVicerrector ? facultadId || null : usuario.idFacultad
  const cargarProgramas = useCallback(() => listarProgramas(facultadPrograma), [facultadPrograma])
  const programas = useFetch(cargarProgramas, [cargarProgramas])

  const filtros = useMemo(
    () => ({
      periodoId: periodoId || null,
      facultadId: esVicerrector ? facultadId || null : null,
      programaId: programaId || null,
    }),
    [periodoId, facultadId, programaId, esVicerrector],
  )
  const cargarKpis = useCallback(() => obtenerKpis(filtros), [filtros])
  const kpis = useFetch(cargarKpis, [cargarKpis])
  const cargarSeguimiento = useCallback(
    () =>
      listarSeguimientoDocentes(filtros.facultadId, { periodoId: filtros.periodoId, idPrograma: filtros.programaId }),
    [filtros],
  )
  const seguimiento = useFetch(cargarSeguimiento, [cargarSeguimiento])
  const configuracion = useFetch(obtenerConfiguracionEvaluacion)

  const listaFacultades = facultades.datos ?? []
  const listaProgramas = programas.datos ?? []
  const nombreFacultad = esVicerrector
    ? (listaFacultades.find((f) => String(f.idFacultad) === facultadId)?.nombre ?? 'Toda la universidad')
    : (listaFacultades.find((f) => f.idFacultad === usuario.idFacultad)?.nombre ?? 'Tu facultad')
  const nombrePrograma = listaProgramas.find((p) => String(p.idPrograma) === programaId)?.nombre
  const alcance = nombrePrograma ? `${nombrePrograma} · ${nombreFacultad}` : nombreFacultad
  const periodoActivo = (periodos.datos ?? []).find((p) => p.esActivo)
  const viendoActivo = periodoId === ACTIVO || String(periodoActivo?.idPeriodo) === periodoId
  const nombrePeriodo = seguimiento.datos?.periodo?.nombre ?? kpis.datos?.periodo.nombre ?? periodoActivo?.nombre
  const hayFiltros = Boolean(periodoId || facultadId || programaId)

  const miFuente = FUENTE_DEL_ROL[usuario.rol]
  const agrupacion = esVicerrector && !facultadId ? 'facultad' : 'programa'
  const umbral = configuracion.datos ? Number(configuracion.datos.umbralCompromisos) : null
  const umbralDestacado = kpis.datos?.umbralDestacado ?? null
  const analisis = useMemo(
    () =>
      seguimiento.datos?.periodo && umbral !== null
        ? analizarSeguimiento(
            seguimiento.datos.docentes,
            miFuente,
            agrupacion === 'facultad' ? 'nombreFacultad' : 'nombrePrograma',
            umbral,
            umbralDestacado,
          )
        : null,
    [seguimiento.datos, miFuente, agrupacion, umbral, umbralDestacado],
  )
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)

  function limpiar() {
    setPeriodoId(ACTIVO)
    setFacultadId(TODAS)
    setProgramaId(TODOS)
  }

  return (
    <>
      <Encabezado
        miga={`${esVicerrector ? 'Vicerrectoría académica' : 'Coordinación'} · Inicio`}
        titulo="Resumen de la evaluación"
        subtitulo={
          esVicerrector
            ? 'Cómo va la evaluación docente en toda la universidad, una facultad o una carrera.'
            : 'Cómo va la evaluación docente en tu facultad o en una de sus carreras, y qué falta por hacer.'
        }
      />

      <div className="pagina inicio">
        <MensajeNavegacion />

        {/* Portada: dónde se está mirando y los filtros que cambian todo el tablero. */}
        <Portada
          id="inicio-alcance"
          icono={SlidersHorizontal}
          rotulo="Estás viendo"
          titulo={alcance}
          detalle={
            <>
              Periodo {nombrePeriodo ?? '—'}
              {viendoActivo ? <span className="inicio__vigente">Activo</span> : <span className="inicio__historico">Histórico</span>}
            </>
          }
        >

          <div className="inicio__filtros" role="search" aria-label="Filtrar el tablero">
            <Filtro id="filtro-periodo" etiqueta="Periodo" valor={periodoId} onCambiar={setPeriodoId}>
              <option value={ACTIVO}>{periodoActivo ? `${periodoActivo.nombre} (activo)` : 'Periodo activo'}</option>
              {(periodos.datos ?? [])
                .filter((p) => !p.esActivo)
                .map((p) => (
                  <option key={p.idPeriodo} value={p.idPeriodo}>
                    {p.nombre}
                  </option>
                ))}
            </Filtro>

            {esVicerrector ? (
              <Filtro
                id="filtro-facultad"
                etiqueta="Facultad"
                valor={facultadId}
                onCambiar={(valor) => {
                  setFacultadId(valor)
                  setProgramaId(TODOS)
                }}
              >
                <option value={TODAS}>Todas las facultades</option>
                {listaFacultades.map((f) => (
                  <option key={f.idFacultad} value={f.idFacultad}>
                    {f.nombre}
                  </option>
                ))}
              </Filtro>
            ) : (
              <div className="inicio__filtro">
                <span className="inicio__filtro-etiqueta">Facultad</span>
                <span className="inicio__filtro-fijo">{nombreFacultad}</span>
              </div>
            )}

            <Filtro id="filtro-programa" etiqueta="Carrera" valor={programaId} onCambiar={setProgramaId}>
              <option value={TODOS}>Todas las carreras</option>
              {listaProgramas.map((p) => (
                <option key={p.idPrograma} value={p.idPrograma}>
                  {p.nombre}
                </option>
              ))}
            </Filtro>

            {hayFiltros && (
              <button type="button" className="inicio__limpiar" onClick={limpiar}>
                <RotateCcw size={15} aria-hidden="true" />
                Limpiar
              </button>
            )}
          </div>
        </Portada>

        {viendoActivo && analisis && analisis.total > 0 && (
          <AsistenteResumen
            nombre={usuario.nombreCompleto}
            analisis={analisis}
            miFuente={miFuente}
            recoleccion={recoleccion}
          />
        )}

        <h2 className="inicio__seccion">
          Indicadores de {kpis.datos?.periodo.nombre ?? 'el periodo'}
          <span> · {alcance}</span>
        </h2>
        <Indicadores kpis={kpis} esVicerrector={esVicerrector} mostrarCompletos={!viendoActivo} />

        {viendoActivo && <LineaTiempo />}

        <h2 className="inicio__seccion">
          Docentes del periodo{nombrePeriodo ? ` ${nombrePeriodo}` : ''}
          <span> · {alcance}</span>
        </h2>
        <Seguimiento
          seguimiento={seguimiento}
          configuracion={configuracion}
          analisis={analisis}
          agrupacion={agrupacion}
          miFuente={miFuente}
        />
      </div>
    </>
  )
}

/** Lista desplegable de la barra de filtros, con su rótulo encima. */
function Filtro({ id, etiqueta, valor, onCambiar, children }) {
  return (
    <div className="inicio__filtro">
      <label htmlFor={id} className="inicio__filtro-etiqueta">
        {etiqueta}
      </label>
      <select id={id} className="inicio__filtro-control" value={valor} onChange={(e) => onCambiar(e.target.value)}>
        {children}
      </select>
    </div>
  )
}

/**
 * En el periodo activo, "docentes con evaluación completa" ya lo cuenta el anillo de avance; solo se
 * muestra como tarjeta al consultar periodos anteriores, donde no hay seguimiento.
 */
function Indicadores({ kpis, esVicerrector, mostrarCompletos }) {
  if (kpis.cargando) {
    return (
      <div className="inicio__tarjetas" role="status" aria-live="polite">
        <span className="solo-lectores">Cargando indicadores</span>
        {[1, 2, 3, 4].map((n) => (
          <div key={n} className="inicio__esqueleto" />
        ))}
      </div>
    )
  }

  if (kpis.error) {
    if (kpis.error.status === 409) {
      return (
        <EstadoVista
          titulo="No hay un periodo académico activo"
          mensaje="Elige un periodo en el filtro para consultar sus indicadores, o pide a la vicerrectoría que active uno."
        />
      )
    }
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar los indicadores" mensaje={kpis.error.message}>
        <Boton icono={RefreshCw} onClick={kpis.recargar}>
          Intentar de nuevo
        </Boton>
      </EstadoVista>
    )
  }

  const datos = kpis.datos
  if (!datos) return null


  if (Number(datos.evaluacionesRealizadas.valor) === 0) {
    return (
      <EstadoVista
        titulo="Aún no hay calificaciones en este periodo"
        mensaje="Cuando coordinación, vicerrectoría o la carga del CSV de estudiantes registren calificaciones, los indicadores aparecerán aquí."
      />
    )
  }

  const { periodo, periodoAnterior, umbralDestacado } = datos
  const unidadDocentes = (indicador) => (Number(indicador.valor) === 1 ? 'docente' : 'docentes')

  return (
    <div className="inicio__kpis" style={{ '--tarjetas': mostrarCompletos ? 3 : 2 }}>
      <TarjetaPromedio
        indicador={datos.promedioGeneral}
        periodo={periodo}
        periodoAnterior={periodoAnterior}
        ambito={esVicerrector ? 'de los docentes' : 'de los docentes de la facultad'}
      />
      {mostrarCompletos && (
      <TarjetaKpi
        icono={UserCheck}
        rotulo="Docentes con evaluación completa"
        valor={formatearEntero(datos.docentesEvaluados.valor)}
        unidad={unidadDocentes(datos.docentesEvaluados)}
        indicador={datos.docentesEvaluados}
        periodoAnterior={periodoAnterior}
        explicacion="Tienen las tres calificaciones: estudiantes, coordinación y vicerrectoría."
      />
      )}
      <TarjetaKpi
        icono={ClipboardList}
        rotulo="Calificaciones registradas"
        valor={formatearEntero(datos.evaluacionesRealizadas.valor)}
        unidad="en total"
        indicador={datos.evaluacionesRealizadas}
        periodoAnterior={periodoAnterior}
        explicacion="Cada docente recibe hasta tres: una por fuente."
      />
      <TarjetaKpi
        icono={Award}
        rotulo="Docentes destacados"
        valor={formatearEntero(datos.docentesDestacados.valor)}
        unidad={unidadDocentes(datos.docentesDestacados)}
        indicador={datos.docentesDestacados}
        periodoAnterior={periodoAnterior}
        explicacion={`Con promedio de ${formatearNota(umbralDestacado)} o más.`}
      />
    </div>
  )
}

function Seguimiento({ seguimiento, configuracion, analisis, agrupacion, miFuente }) {
  if (seguimiento.cargando) return <Cargando texto="Cargando el seguimiento" bloques={2} />
  const error = seguimiento.error ?? configuracion.error
  if (error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar el seguimiento" mensaje={error.message}>
        <Boton
          icono={RefreshCw}
          onClick={() => {
            seguimiento.recargar()
            configuracion.recargar()
          }}
        >
          Intentar de nuevo
        </Boton>
      </EstadoVista>
    )
  }
  if (!seguimiento.datos?.periodo) {
    return (
      <EstadoVista
        titulo="No hay un periodo académico activo"
        mensaje="Elige un periodo en los filtros, o pide a la vicerrectoría que active uno."
      />
    )
  }
  // El seguimiento puede llegar antes que la configuración (el umbral); mientras tanto, cargando.
  if (!analisis) return <Cargando texto="Cargando el seguimiento" bloques={2} />
  if (analisis.total === 0) {
    return <EstadoVista titulo="No hay docentes en esta selección" mensaje="Prueba con otra facultad o carrera." />
  }

  return (
    <>
      <div className="inicio__rejilla inicio__rejilla--extremos">
        <PanelAtencion analisis={analisis} />
        <PanelHonor analisis={analisis} />
      </div>
      <div className="inicio__rejilla">
        <PanelAvance analisis={analisis} miFuente={miFuente} periodo={seguimiento.datos.periodo} />
        <PanelPorGrupo analisis={analisis} agrupacion={agrupacion} />
      </div>
      <Link to="/estadisticas" className="inicio__estadisticas">
        <span className="inicio__estadisticas-icono" aria-hidden="true">
          <ChartColumnBig size={22} />
        </span>
        <span>
          <strong>Ver todas las estadísticas</strong>
          <span>
            Niveles de desempeño, avance del proceso, promedios por {agrupacion}, respuesta de estudiantes y actas,
            con filtros que actualizan todo a la vez.
          </span>
        </span>
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
    </>
  )
}
