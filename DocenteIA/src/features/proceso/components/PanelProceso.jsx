import { RefreshCw, Route } from 'lucide-react'
import { useCallback } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, listarFuentes } from '../../../shared/utils/formatters.js'
import { formatearHora, hoyEnColombia } from '../../agenda/franjas.js'
import { useSesion } from '../../auth/sesionContext.js'
import {
  ESTADOS_PROCESO,
  ETIQUETA_ASISTENCIA,
  ETIQUETA_CONFIRMACION,
  ETIQUETA_TIPO_ACTA,
  describirCobertura,
  puedeRegistrarAsistencia,
} from '../estados.js'
import { obtenerProceso } from '../services/procesoService.js'
import { ChipProceso } from './ChipProceso.jsx'
import { ControlAsistencia } from './ControlAsistencia.jsx'
import { HitosProceso, PasosProceso } from './LineaProceso.jsx'
import './Proceso.css'

function describirCita(cita) {
  if (!cita) return 'Sin cita'
  const horario = `${formatearFecha(cita.fecha)}, ${formatearHora(cita.horaInicio)}–${formatearHora(cita.horaFin)}`
  if (cita.asistencia) return `${horario} · ${ETIQUETA_ASISTENCIA[cita.asistencia]}`
  if (!cita.activa) return `${horario} · liberada`
  return `${horario} · ${cita.confirmacion ? ETIQUETA_CONFIRMACION[cita.confirmacion] : 'Sin confirmar'}`
}

function describirActa(acta) {
  if (!acta) return 'Sin generar'
  const firmas = [acta.firmadaPorDocente && 'docente', acta.firmadaPorCoordinacion && 'coordinación'].filter(Boolean)
  const estado = acta.estado === 'FIRMADA' ? 'firmada' : firmas.length ? `firmó ${firmas.join(' y ')}` : 'sin firmas'
  return `${ETIQUETA_TIPO_ACTA[acta.tipo]} · ${estado}`
}

/**
 * Estado general del proceso de un docente para coordinación y vicerrectoría: evaluaciones, cita,
 * asistencia y acta, con la trazabilidad de cada paso. La coordinación registra aquí la asistencia.
 */
export function PanelProceso({ idDocente, onCambio }) {
  const { usuario } = useSesion()
  const cargar = useCallback(() => obtenerProceso(idDocente), [idDocente])
  const proceso = useFetch(cargar, [cargar])

  if (proceso.cargando && !proceso.datos) {
    return (
      <Panel titulo="Proceso de evaluación" icono={Route} tono="turquesa">
        <Cargando texto="Cargando el proceso" bloques={1} />
      </Panel>
    )
  }
  if (proceso.error) {
    return (
      <Panel titulo="Proceso de evaluación" icono={Route} tono="turquesa">
        <EstadoVista tipo="error" titulo="No fue posible cargar el proceso" mensaje={proceso.error.message}>
          <Boton icono={RefreshCw} onClick={proceso.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      </Panel>
    )
  }

  const datos = proceso.datos
  const { cita, coberturaEstudiantes: cobertura } = datos
  const registrar = usuario.rol === ROLES.COORDINADOR && puedeRegistrarAsistencia(cita, hoyEnColombia())

  return (
    <Panel
      titulo="Proceso de evaluación"
      descripcion={ESTADOS_PROCESO[datos.estado]?.ayuda}
      icono={Route}
      tono="turquesa"
      acciones={<ChipProceso estado={datos.estado} />}
    >
      <PasosProceso proceso={datos} />

      <dl className="proceso__datos">
        <div>
          <dt>Evaluaciones</dt>
          <dd>
            {datos.fuentesPendientes.length === 0 ? 'Las tres completas' : `Falta ${listarFuentes(datos.fuentesPendientes)}`}
          </dd>
        </div>
        {cobertura && (
          <div>
            <dt>Formularios de estudiantes</dt>
            <dd>
              {describirCobertura(cobertura).titulo}
              {describirCobertura(cobertura).detalle && (
                <span className="proceso__dato-detalle">{describirCobertura(cobertura).detalle}</span>
              )}
            </dd>
          </div>
        )}
        <div>
          <dt>Cita</dt>
          <dd>{describirCita(cita)}</dd>
        </div>
        <div>
          <dt>Acta</dt>
          <dd>{describirActa(datos.acta)}</dd>
        </div>
        <div>
          <dt>Resultado para el docente</dt>
          <dd>
            <span className={`pildora pildora--${datos.resultadoHabilitado ? 'exito' : 'neutro'}`}>
              {datos.resultadoHabilitado ? 'Habilitado' : 'Oculto hasta la reunión'}
            </span>
          </dd>
        </div>
      </dl>

      {registrar && (
        <ControlAsistencia
          cita={cita}
          nombreDocente={datos.docente.nombreCompleto}
          onRegistrada={() => {
            proceso.recargar()
            onCambio?.()
          }}
        />
      )}

      <h3 className="proceso__subtitulo">Trazabilidad</h3>
      <HitosProceso hitos={datos.hitos} />
    </Panel>
  )
}
