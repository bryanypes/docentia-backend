import { ArrowRight, Award, ChartColumnBig, ClipboardList, RefreshCw, UserCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCallback, useMemo, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { ETAPAS, FUENTE_DEL_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearEntero, formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarSeguimientoDocentes } from '../../docentes/services/docentesService.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { PanelEtapas } from '../../periodos/components/PanelEtapas.jsx'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { PanelAtencion, PanelAvance } from '../components/PanelesSeguimiento.jsx'
import { AsistenteResumen } from '../components/AsistenteResumen.jsx'
import { TarjetaKpi, TarjetaPromedio } from '../components/TarjetaKpi.jsx'
import { LineaTiempo } from '../../periodos/components/LineaTiempo.jsx'
import { analizarSeguimiento } from '../seguimiento.js'
import { obtenerConfiguracionEvaluacion } from '../services/configuracionService.js'
import { obtenerKpis } from '../services/kpisService.js'
import './Inicio.css'

const ACTIVO = ''
const TODAS = ''

export default function Inicio() {
  const { usuario } = useSesion()
  const esVicerrector = usuario.rol === ROLES.VICERRECTOR

  const [periodoId, setPeriodoId] = useState(ACTIVO)
  const [facultadId, setFacultadId] = useState(TODAS)

  const periodos = useFetch(listarPeriodos)
  const facultades = useFetch(listarFacultades)
  const cargarKpis = useCallback(
    () => obtenerKpis({ periodoId, facultadId: esVicerrector ? facultadId : null }),
    [periodoId, facultadId, esVicerrector],
  )
  const kpis = useFetch(cargarKpis, [cargarKpis])
  const cargarSeguimiento = useCallback(
    () => listarSeguimientoDocentes(esVicerrector ? facultadId || null : null),
    [facultadId, esVicerrector],
  )
  const seguimiento = useFetch(cargarSeguimiento, [cargarSeguimiento])
  const configuracion = useFetch(obtenerConfiguracionEvaluacion)

  const listaFacultades = facultades.datos ?? []
  const nombreFacultad = esVicerrector
    ? (listaFacultades.find((f) => String(f.idFacultad) === facultadId)?.nombre ?? 'Toda la universidad')
    : (listaFacultades.find((f) => f.idFacultad === usuario.idFacultad)?.nombre ?? 'Tu facultad')
  const periodoActivo = (periodos.datos ?? []).find((p) => p.esActivo)
  const viendoActivo = periodoId === ACTIVO || String(periodoActivo?.idPeriodo) === periodoId

  // El análisis del periodo activo lo comparten el asistente y los paneles de seguimiento.
  const miFuente = FUENTE_DEL_ROL[usuario.rol]
  // Los paneles hablan de programas para todos los roles; el filtro de facultad solo acota.
  const agrupacion = 'programa'
  const analisis = useMemo(
    () =>
      seguimiento.datos?.periodo && configuracion.datos
        ? analizarSeguimiento(
            seguimiento.datos.docentes,
            miFuente,
            agrupacion === 'facultad' ? 'nombreFacultad' : 'nombrePrograma',
            Number(configuracion.datos.umbralCompromisos),
          )
        : null,
    [seguimiento.datos, miFuente, agrupacion, configuracion.datos],
  )
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)

  return (
    <>
      <Encabezado
        miga={`${esVicerrector ? 'Vicerrectoría académica' : 'Coordinación'} · Inicio`}
        titulo="Resumen de la evaluación"
        subtitulo={
          esVicerrector
            ? 'Cómo va la evaluación docente en toda la universidad o en la facultad que elijas.'
            : 'Cómo va la evaluación docente en tu facultad y qué falta por hacer.'
        }
      />

      <div className="pagina inicio">
        <MensajeNavegacion />
        <section className="inicio__filtros" aria-label="Filtros del resumen">
          <div className="inicio__campos">
            <div className="campo">
              <label htmlFor="filtro-periodo" className="rotulo">
                Periodo académico
              </label>
              <select
                id="filtro-periodo"
                className="campo__control"
                value={periodoId}
                onChange={(e) => setPeriodoId(e.target.value)}
              >
                <option value={ACTIVO}>{periodoActivo ? `${periodoActivo.nombre} (activo)` : 'Periodo activo'}</option>
                {(periodos.datos ?? [])
                  .filter((p) => !p.esActivo)
                  .map((p) => (
                    <option key={p.idPeriodo} value={p.idPeriodo}>
                      {p.nombre}
                    </option>
                  ))}
              </select>
            </div>

            {esVicerrector ? (
              <div className="campo">
                <label htmlFor="filtro-facultad" className="rotulo">
                  Facultad
                </label>
                <select
                  id="filtro-facultad"
                  className="campo__control"
                  value={facultadId}
                  onChange={(e) => setFacultadId(e.target.value)}
                >
                  <option value={TODAS}>Todas las facultades</option>
                  {listaFacultades.map((f) => (
                    <option key={f.idFacultad} value={f.idFacultad}>
                      {f.nombre}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="campo">
                <span className="rotulo rotulo--campo">Facultad</span>
                <div className="inicio__fijo">
                  <span>{nombreFacultad}</span>
                  <span className="inicio__fijo-etiqueta">Tu facultad</span>
                </div>
              </div>
            )}
          </div>
        </section>

        {analisis && analisis.total > 0 && (
          <AsistenteResumen
            nombre={usuario.nombreCompleto}
            analisis={analisis}
            miFuente={miFuente}
            recoleccion={recoleccion}
          />
        )}

        <LineaTiempo />

        <h2 className="inicio__seccion">
          Indicadores de {kpis.datos?.periodo.nombre ?? 'el periodo'}
          <span> · {nombreFacultad}</span>
        </h2>
        <Indicadores kpis={kpis} esVicerrector={esVicerrector} mostrarCompletos={!viendoActivo} />
        {esVicerrector && viendoActivo && (
          <ListaDestacados seguimiento={seguimiento} umbral={kpis.datos?.umbralDestacado} />
        )}

        <h2 className="inicio__seccion">
          Seguimiento del periodo activo{seguimiento.datos?.periodo ? ` (${seguimiento.datos.periodo.nombre})` : ''}
        </h2>
        {viendoActivo ? (
          <Seguimiento
            seguimiento={seguimiento}
            configuracion={configuracion}
            analisis={analisis}
            agrupacion={agrupacion}
            miFuente={miFuente}
          />
        ) : (
          <Aviso tipo="info" titulo="El seguimiento se calcula solo para el periodo activo">
            El avance, los niveles y los docentes que requieren seguimiento corresponden a{' '}
            {periodoActivo?.nombre ?? 'el periodo activo'}. Los indicadores de arriba sí son del periodo que
            elegiste.
          </Aviso>
        )}

        <PanelEtapas />
      </div>
    </>
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
        mensaje="Cuando se envíen los formularios de evaluación, los indicadores aparecerán aquí."
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
        explicacion="Cada docente recibe hasta tres: una por evaluador."
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
    return <EstadoVista titulo="No hay un periodo académico activo" mensaje="Cuando la vicerrectoría active uno, verás aquí el avance." />
  }
  // El seguimiento puede llegar antes que la configuración (el umbral); mientras tanto, cargando.
  if (!analisis) return <Cargando texto="Cargando el seguimiento" bloques={2} />
  if (analisis.total === 0) {
    return <EstadoVista titulo="No hay docentes en este alcance" mensaje="Cuando se registren docentes activos aparecerán aquí." />
  }

  return (
    <>
      <div className="inicio__rejilla">
        <PanelAvance analisis={analisis} miFuente={miFuente} periodo={seguimiento.datos.periodo} />
        <PanelAtencion analisis={analisis} />
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

/** Docentes con promedio completo igual o mayor al umbral de destacado, de mayor a menor. */
export function ListaDestacados({ seguimiento, umbral }) {
  if (umbral == null || !seguimiento.datos) return null
  const destacados = seguimiento.datos.docentes
    .filter((d) => d.promedio?.estado === 'COMPLETO' && Number(d.promedio.valor) >= Number(umbral))
    .sort((a, b) => Number(b.promedio.valor) - Number(a.promedio.valor))
  return (
    <section className="inicio__destacados panel" aria-labelledby="titulo-destacados">
      <h3 id="titulo-destacados" className="inicio__destacados-titulo">
        Docentes destacados <span>· promedio de {formatearNota(umbral)} o más</span>
      </h3>
      {destacados.length === 0 ? (
        <p className="campo__ayuda">Ningún docente con promedio completo alcanza el umbral todavía.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th scope="col">Docente</th>
              <th scope="col">Programa</th>
              <th scope="col">Puntaje</th>
              <th scope="col">Clasificación</th>
            </tr>
          </thead>
          <tbody>
            {destacados.map((d) => (
              <tr key={d.docente.idDocente}>
                <td className="tabla__fuerte">
                  <Link to={`/docentes/${d.docente.idDocente}`}>{d.docente.nombreCompleto}</Link>
                </td>
                <td data-etiqueta="Programa">{d.docente.nombrePrograma}</td>
                <td data-etiqueta="Puntaje">{formatearNota(d.promedio.valor)}</td>
                <td data-etiqueta="Clasificación">{d.promedio.categoria?.etiqueta ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
