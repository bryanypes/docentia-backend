import { Award, ClipboardList, RefreshCw, UserCheck } from 'lucide-react'
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
import {
  PanelAtencion,
  PanelAvance,
  PanelDistribucion,
  PanelPorGrupo,
} from '../components/PanelesSeguimiento.jsx'
import { AsistenteResumen } from '../components/AsistenteResumen.jsx'
import { TarjetaKpi, TarjetaPromedio } from '../components/TarjetaKpi.jsx'
import { analizarSeguimiento } from '../seguimiento.js'
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

  const listaFacultades = facultades.datos ?? []
  const nombreFacultad = esVicerrector
    ? (listaFacultades.find((f) => String(f.idFacultad) === facultadId)?.nombre ?? 'Toda la universidad')
    : (listaFacultades.find((f) => f.idFacultad === usuario.idFacultad)?.nombre ?? 'Tu facultad')
  const periodoActivo = (periodos.datos ?? []).find((p) => p.esActivo)
  const viendoActivo = periodoId === ACTIVO || String(periodoActivo?.idPeriodo) === periodoId

  // El análisis del periodo activo lo comparten el asistente y los paneles de seguimiento.
  const miFuente = FUENTE_DEL_ROL[usuario.rol]
  const agrupacion = esVicerrector ? 'facultad' : 'programa'
  const analisis = useMemo(
    () =>
      seguimiento.datos?.periodo
        ? analizarSeguimiento(
            seguimiento.datos.docentes,
            miFuente,
            agrupacion === 'facultad' ? 'nombreFacultad' : 'nombrePrograma',
          )
        : null,
    [seguimiento.datos, miFuente, agrupacion],
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

      <div className="pagina">
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

        <h2 className="inicio__seccion">
          Indicadores de {kpis.datos?.periodo.nombre ?? 'el periodo'}
          <span> · {nombreFacultad}</span>
        </h2>
        <Indicadores kpis={kpis} esVicerrector={esVicerrector} mostrarCompletos={!viendoActivo} />

        <h2 className="inicio__seccion">
          Seguimiento del periodo activo{seguimiento.datos?.periodo ? ` (${seguimiento.datos.periodo.nombre})` : ''}
        </h2>
        {viendoActivo ? (
          <Seguimiento seguimiento={seguimiento} analisis={analisis} agrupacion={agrupacion} miFuente={miFuente} />
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

function Seguimiento({ seguimiento, analisis, agrupacion, miFuente }) {

  if (seguimiento.cargando) return <Cargando texto="Cargando el seguimiento" bloques={2} />
  if (seguimiento.error) {
    return (
      <EstadoVista tipo="error" titulo="No fue posible cargar el seguimiento" mensaje={seguimiento.error.message}>
        <Boton icono={RefreshCw} onClick={seguimiento.recargar}>
          Intentar de nuevo
        </Boton>
      </EstadoVista>
    )
  }
  if (!seguimiento.datos?.periodo) {
    return <EstadoVista titulo="No hay un periodo académico activo" mensaje="Cuando la vicerrectoría active uno, verás aquí el avance." />
  }
  if (analisis.total === 0) {
    return <EstadoVista titulo="No hay docentes en este alcance" mensaje="Cuando se registren docentes activos aparecerán aquí." />
  }

  return (
    <>
      <div className="inicio__rejilla">
        <PanelAvance analisis={analisis} miFuente={miFuente} periodo={seguimiento.datos.periodo} />
        <PanelDistribucion analisis={analisis} />
      </div>
      {/* Con un solo grupo, su promedio es el mismo promedio general: no se repite */}
      <div className="inicio__rejilla">
        <PanelAtencion analisis={analisis} />
        {analisis.porGrupo.length > 1 && <PanelPorGrupo analisis={analisis} agrupacion={agrupacion} />}
      </div>
    </>
  )
}
