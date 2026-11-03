import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import * as procesoService from '../services/procesoService.js'
import { PanelProceso } from './PanelProceso.jsx'
import { PanelProcesoPropio } from './PanelProcesoPropio.jsx'

vi.mock('../services/procesoService.js')

const PROCESO = {
  docente: { idDocente: 4, nombreCompleto: 'Nora Muñoz' },
  estado: 'ASISTENCIA_CONFIRMADA',
  paso: 5,
  fuentesPendientes: [],
  coberturaEstudiantes: { respondidas: 18, esperadas: 25 },
  cita: {
    idCita: 3,
    activa: true,
    fecha: '2026-11-09',
    horaInicio: '08:00:00',
    horaFin: '08:30:00',
    nombreCoordinador: 'Marta Ruiz',
    confirmacion: 'CONFIRMADA',
    asistencia: null,
  },
  acta: null,
  resultadoHabilitado: false,
  hitos: [
    { codigo: 'RESERVA', titulo: 'Escogió horario', detalle: '09/11/2026 08:00–08:30', fecha: '2026-11-02T10:00:00' },
    { codigo: 'CONFIRMACION', titulo: 'Confirmó asistencia', detalle: null, fecha: '2026-11-08T09:00:00' },
  ],
}

describe('PanelProceso', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-09T17:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra el estado, la cobertura de estudiantes y la trazabilidad', async () => {
    procesoService.obtenerProceso.mockResolvedValue(PROCESO)
    renderizarConSesion(<PanelProceso idDocente={4} />, { rol: 'VICERRECTOR' })

    expect(await screen.findByText('Asistencia confirmada')).toBeInTheDocument()
    expect(screen.getByText('18 de 25')).toBeInTheDocument()
    expect(screen.getByText('Oculto hasta la reunión')).toBeInTheDocument()
    expect(screen.getByText('Escogió horario')).toBeInTheDocument()
    expect(screen.getByText('Confirmó asistencia')).toBeInTheDocument()
    // La vicerrectoría observa: no registra asistencia.
    expect(screen.queryByRole('button', { name: /Asistió/ })).not.toBeInTheDocument()
  })

  it('el día de la cita la coordinación habilita el resultado', async () => {
    procesoService.obtenerProceso.mockResolvedValue(PROCESO)
    procesoService.registrarAsistencia.mockResolvedValue({ ...PROCESO.cita, asistencia: 'ASISTIO' })
    renderizarConSesion(<PanelProceso idDocente={4} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Asistió · habilitar resultado' }))
    expect(procesoService.registrarAsistencia).toHaveBeenCalledWith(3, true)
    await waitFor(() => expect(procesoService.obtenerProceso).toHaveBeenCalledTimes(2))
  })

  it('el docente no ve detalle mientras las evaluaciones siguen en curso', async () => {
    procesoService.obtenerProcesoPropio.mockResolvedValue({
      ...PROCESO,
      estado: 'EVALUACIONES_EN_CURSO',
      paso: 1,
      cita: null,
      hitos: [],
    })
    renderizarConSesion(<PanelProcesoPropio />, { rol: 'DOCENTE' })

    expect(await screen.findByText(/Tu evaluación está en curso/)).toBeInTheDocument()
    expect(screen.queryByText('Trazabilidad')).not.toBeInTheDocument()
  })

  it('el panel del docente vuelve a pedir el proceso cuando cambia la versión', async () => {
    procesoService.obtenerProcesoPropio.mockResolvedValue({ ...PROCESO, estado: 'EVALUACION_COMPLETA', paso: 3 })
    const { rerender } = renderizarConSesion(<PanelProcesoPropio version={0} />, { rol: 'DOCENTE' })
    expect(await screen.findByText('Evaluación completa')).toBeInTheDocument()

    procesoService.obtenerProcesoPropio.mockResolvedValue(PROCESO)
    rerender(<PanelProcesoPropio version={1} />)
    expect(await screen.findByText('Asistencia confirmada')).toBeInTheDocument()
  })
})
