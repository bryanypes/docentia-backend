import { CalendarCheck, CalendarDays, GraduationCap, Landmark, RefreshCw, Search } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl } from '../../../shared/hooks/usePaginacion.js'
import { formatearFecha, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarCitasProgramadas } from '../services/agendaService.js'
import './CitasProgramadas.css'

const TIPOS = {
  COORDINACION: { etiqueta: 'Coordinación ↔ docentes', Icono: GraduationCap },
  VICERRECTORIA: { etiqueta: 'Vicerrectoría ↔ coordinadores', Icono: Landmark },
}

const ESTADOS = {
  POR_CONFIRMAR: { etiqueta: 'Por confirmar', tono: 'alerta' },
  CONFIRMADA: { etiqueta: 'Confirmada', tono: 'info' },
  ASISTIO: { etiqueta: 'Asistió', tono: 'exito' },
}

const tipoDe = (c) => (c.rolAnfitrion === 'VICERRECTOR' ? 'VICERRECTORIA' : 'COORDINACION')
const estadoDe = (c) => (c.asistencia === 'ASISTIO' ? 'ASISTIO' : c.confirmacion === 'CONFIRMADA' ? 'CONFIRMADA' : 'POR_CONFIRMAR')
const hora = (t) => (t ?? '').slice(0, 5)

/**
 * Todas las citas agendadas del periodo para la administración: coordinación con sus docentes y
 * vicerrectoría con los coordinadores. Solo lectura; no muestra notas.
 */
export default function CitasProgramadas() {
  const { usuario } = useSesion()
  const [filtros, actualizarFiltros] = useFiltrosUrl({ periodo: '', tipo: '', estado: '', busqueda: '' })
  const { periodo, tipo, estado, busqueda } = filtros
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => listarCitasProgramadas(periodo), [periodo])
  const citas = useFetch(cargar, [cargar])
  const todas = useMemo(() => citas.datos ?? [], [citas.datos])

  const visibles = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return todas.filter(
      (c) =>
        (!tipo || tipoDe(c) === tipo) &&
        (!estado || estadoDe(c) === estado) &&
        (!termino || normalizarTexto(`${c.nombreDocente} ${c.nombreAnfitrion} ${c.unidad ?? ''}`).includes(termino)),
    )
  }, [todas, tipo, estado, busqueda])

  // Agrupadas por día, en el orden en que llegan (fecha y hora).
  const porDia = useMemo(() => {
    const grupos = new Map()
    visibles.forEach((c) => grupos.set(c.fecha, [...(grupos.get(c.fecha) ?? []), c]))
    return [...grupos.entries()]
  }, [visibles])

  const cuenta = (clave) => todas.filter((c) => tipoDe(c) === clave).length
  const hayFiltros = Boolean(tipo || estado || busqueda)

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Citas programadas`}
        titulo="Citas programadas"
        subtitulo="Todas las reuniones de retroalimentación agendadas: coordinación con sus docentes y vicerrectoría con los coordinadores."
      />
      <div className="pagina">
        <div className="citas-programadas__resumen">
          <div className="citas-programadas__cifra">
            <CalendarDays size={18} aria-hidden="true" />
            <span>Citas agendadas</span>
            <strong>{todas.length}</strong>
          </div>
          {Object.entries(TIPOS).map(([clave, { etiqueta, Icono }]) => (
            <button
              key={clave}
              type="button"
              className={`citas-programadas__cifra citas-programadas__cifra--boton${tipo === clave ? ' citas-programadas__cifra--activa' : ''}`}
              aria-pressed={tipo === clave}
              onClick={() => actualizarFiltros({ tipo: tipo === clave ? '' : clave })}
            >
              <Icono size={18} aria-hidden="true" />
              <span>{etiqueta}</span>
              <strong>{cuenta(clave)}</strong>
            </button>
          ))}
        </div>

        <Panel titulo="Filtros" className="citas-programadas__filtros-panel">
          <div className="citas-programadas__filtros">
            <label className="citas-programadas__buscador">
              <Search size={16} aria-hidden="true" />
              <span className="solo-lectores">Buscar por docente o por quien lo recibe</span>
              <input
                type="search"
                className="campo__control"
                placeholder="Buscar por docente, coordinador o programa"
                value={busqueda}
                onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
              />
            </label>
            <Campo id="periodoCitas" etiqueta="Periodo">
              <select
                id="periodoCitas"
                className="campo__control"
                value={periodo}
                onChange={(e) => actualizarFiltros({ periodo: e.target.value })}
              >
                <option value="">Periodo activo</option>
                {(periodos.datos ?? []).map((p) => (
                  <option key={p.idPeriodo} value={p.idPeriodo}>
                    {p.nombre}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="estadoCitas" etiqueta="Estado">
              <select
                id="estadoCitas"
                className="campo__control"
                value={estado}
                onChange={(e) => actualizarFiltros({ estado: e.target.value })}
              >
                <option value="">Todas</option>
                {Object.entries(ESTADOS).map(([clave, { etiqueta }]) => (
                  <option key={clave} value={clave}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          {hayFiltros && (
            <Boton variante="enlace" onClick={() => actualizarFiltros({ tipo: '', estado: '', busqueda: '' })}>
              Quitar filtros
            </Boton>
          )}
        </Panel>

        {citas.cargando && !citas.datos && <Cargando texto="Cargando las citas" bloques={2} />}
        {citas.error && (
          <EstadoVista
            tipo={citas.error.status === 409 ? 'vacio' : 'error'}
            titulo={citas.error.status === 409 ? 'No hay un periodo activo' : 'No fue posible cargar las citas'}
            mensaje={citas.error.message}
          >
            <Boton icono={RefreshCw} onClick={citas.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {citas.datos && visibles.length === 0 && (
          <EstadoVista
            titulo={todas.length === 0 ? 'No hay citas agendadas en este periodo' : 'Ninguna cita coincide con los filtros'}
          />
        )}

        {porDia.map(([fecha, delDia]) => (
          <Panel
            key={fecha}
            titulo={formatearFecha(fecha)}
            descripcion={`${delDia.length} ${delDia.length === 1 ? 'cita' : 'citas'}`}
            icono={CalendarCheck}
          >
            <ul className="citas-programadas__lista">
              {delDia.map((c) => {
                const e = ESTADOS[estadoDe(c)]
                return (
                  <li key={c.idCita} className="citas-programadas__cita">
                    <span className="citas-programadas__hora">
                      {hora(c.horaInicio)}–{hora(c.horaFin)}
                    </span>
                    <span className="citas-programadas__personas">
                      <strong>{c.nombreDocente}</strong>
                      <span>
                        {c.rolDocente === 'COORDINADOR' ? 'Coordinador(a)' : 'Docente'}
                        {c.unidad ? ` · ${c.unidad}` : ''}
                      </span>
                      <span className="citas-programadas__con">
                        con {c.nombreAnfitrion} ({c.rolAnfitrion === 'VICERRECTOR' ? 'vicerrectoría' : 'coordinación'})
                      </span>
                    </span>
                    <span className={`pildora pildora--${e.tono}`}>{e.etiqueta}</span>
                  </li>
                )
              })}
            </ul>
          </Panel>
        ))}
      </div>
    </>
  )
}
