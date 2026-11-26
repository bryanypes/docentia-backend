import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { obtenerConfiguracionEvaluacion } from '../../kpis/services/configuracionService.js'
import { corregirActa, crearActa, firmarActa, listarActas } from '../services/actasService.js'
import { PanelActa } from './PanelActa.jsx'

vi.mock('../services/actasService.js')
vi.mock('../../kpis/services/configuracionService.js')

const ACTA = {
  idActa: 7,
  periodo: { idPeriodo: 1, nombre: '2026-2' },
  version: 1,
  estado: 'BORRADOR',
  promedio: 2.85,
  categoria: { codigo: 'BAJO', etiqueta: 'Bajo', accionInstitucional: 'Plan de mejoramiento' },
  umbralCompromisos: 3.8,
  requiereCompromisos: true,
  compromisos: [],
  firmas: [],
}

// jsdom no dibuja: un contexto 2D mínimo y un PNG fijo como resultado del lienzo.
const TRAZO = 'data:image/png;base64,iVBORw0KGgo='
const CONTEXTO = new Proxy({}, { get: (_, prop) => (prop === 'measureText' ? () => ({ width: 100 }) : () => {}) })

async function firmarEscribiendo(dialogo) {
  await userEvent.click(within(dialogo).getByRole('button', { name: 'Escribir' }))
  await userEvent.type(within(dialogo).getByLabelText('Tu nombre como firma'), 'Ana')
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(CONTEXTO)
  vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue(TRAZO)
  obtenerConfiguracionEvaluacion.mockResolvedValue({ umbralCompromisos: 3.8, rolCorrectorActas: 'VICERRECTOR' })
})

describe('PanelActa', () => {
  it('muestra la identificación congelada del acta', async () => {
    listarActas.mockResolvedValue([
      {
        ...ACTA,
        firmas: [{ rol: 'COORDINADOR', nombre: 'Marta Ruiz', fecha: '2026-11-20T10:00:00', conTrazo: false }],
        identificacion: {
          docente: 'Nora Ficcion',
          documentoRotulo: 'Código',
          documento: '1061234567',
          cargo: 'Docente del programa Software',
          programa: 'Software',
          escuela: 'Escuela de Ingeniería',
          periodo: '2026-2',
          periodoInicio: '2026-07-01',
          periodoFin: '2026-12-15',
          evaluador: 'Coordinación de la Escuela de Ingeniería',
          reunionFecha: '2026-11-20',
          reunionHoraInicio: '09:00:00',
          reunionHoraFin: '09:30:00',
          reunionAsistencia: 'ASISTIO',
          pesoEstudiantes: 50,
          pesoCoordinacion: 30,
          pesoAutoevaluacion: 20,
          respuestasEstudiantes: 12,
          fortalezas: ['Explica con claridad'],
          aspectosPorMejorar: [],
        },
      },
    ])
    renderizarConSesion(<PanelActa idDocente={3} />)
    const bloque = await screen.findByRole('region', { name: 'Identificación del acta' })
    expect(within(bloque).getByText('1061234567')).toBeInTheDocument()
    expect(within(bloque).getByText('Código')).toBeInTheDocument()
    expect(within(bloque).getByText('Escuela de Ingeniería')).toBeInTheDocument()
    expect(within(bloque).getByText('Estudiantes 50 %, coordinación 30 %, autoevaluación 20 %')).toBeInTheDocument()
    expect(within(bloque).getByText('12')).toBeInTheDocument()
    expect(within(bloque).getByText('Explica con claridad')).toBeInTheDocument()
    expect(within(bloque).getByText(/quedaron fijos con la primera firma/)).toBeInTheDocument()
  })

  it('en la vista del docente no hay conteo de respuestas', async () => {
    listarActas.mockResolvedValue([
      { ...ACTA, identificacion: { docente: 'Nora Ficcion', cargo: 'Docente', periodo: '2026-2', evaluador: 'Coordinación' } },
    ])
    renderizarConSesion(<PanelActa />)
    const bloque = await screen.findByRole('region', { name: 'Identificación del acta' })
    expect(within(bloque).queryByText('Respuestas de estudiantes')).not.toBeInTheDocument()
  })

  it('toma el umbral del acta y exige compromiso', async () => {
    listarActas.mockResolvedValue([ACTA])
    renderizarConSesion(<PanelActa idDocente={3} />)
    expect(await screen.findByText('Nota menor a 3,80')).toBeInTheDocument()
    expect(screen.getByText('Plan de Mejoramiento Profesoral')).toBeInTheDocument()
  })

  it('la coordinación genera el acta', async () => {
    listarActas.mockResolvedValue([])
    crearActa.mockResolvedValue(ACTA)
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Generar acta' }))
    expect(crearActa).toHaveBeenCalledWith(3)
  })

  it('firmar abre el lienzo y exige el trazo y la confirmación', async () => {
    listarActas.mockResolvedValue([ACTA])
    firmarActa.mockResolvedValue(ACTA)
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Firmar acta' }))

    const dialogo = screen.getByRole('dialog', { name: 'Firmar el acta' })
    const enviar = within(dialogo).getByRole('button', { name: 'Firmar acta' })
    expect(enviar).toBeDisabled()
    await firmarEscribiendo(dialogo)
    expect(enviar).toBeDisabled()
    await userEvent.click(within(dialogo).getByRole('checkbox'))
    await userEvent.click(enviar)
    expect(firmarActa).toHaveBeenCalledWith(7, TRAZO)
  })

  it('un doble clic en firmar envía una sola vez', async () => {
    listarActas.mockResolvedValue([ACTA])
    let terminar
    firmarActa.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Firmar acta' }))
    const dialogo = screen.getByRole('dialog', { name: 'Firmar el acta' })
    await firmarEscribiendo(dialogo)
    await userEvent.click(within(dialogo).getByRole('checkbox'))
    await userEvent.dblClick(within(dialogo).getByRole('button', { name: 'Firmar acta' }))
    expect(firmarActa).toHaveBeenCalledTimes(1)
    terminar(ACTA)
  })

  it('el error de la firma se muestra dentro del modal', async () => {
    listarActas.mockResolvedValue([ACTA])
    firmarActa.mockRejectedValue(new ApiError(409, 'El docente no tiene una cita agendada'))
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Firmar acta' }))
    const dialogo = screen.getByRole('dialog', { name: 'Firmar el acta' })
    await firmarEscribiendo(dialogo)
    await userEvent.click(within(dialogo).getByRole('checkbox'))
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Firmar acta' }))
    expect(await within(dialogo).findByText('El docente no tiene una cita agendada')).toBeInTheDocument()
  })

  it('solo el rol corrector corrige un acta firmada', async () => {
    const firmada = { ...ACTA, estado: 'FIRMADA', codigoVerificacion: 'ABC', hashContenido: 'ff', firmas: [] }
    listarActas.mockResolvedValue([firmada])
    corregirActa.mockResolvedValue(firmada)
    const { unmount } = renderizarConSesion(<PanelActa idDocente={3} />)
    await screen.findByText(/Código ABC/)
    await waitFor(() => expect(obtenerConfiguracionEvaluacion).toHaveBeenCalled())
    expect(screen.queryByRole('button', { name: /Corregir/ })).not.toBeInTheDocument()
    unmount()

    renderizarConSesion(<PanelActa idDocente={3} />, { rol: 'VICERRECTOR' })
    await userEvent.type(await screen.findByLabelText('Motivo de la corrección'), 'Error de digitación')
    await userEvent.click(screen.getByRole('button', { name: /Corregir/ }))
    const confirmacion = await screen.findByRole('dialog', { name: '¿Corregir el acta firmada?' })
    await userEvent.click(within(confirmacion).getByRole('button', { name: 'Corregir acta' }))
    expect(corregirActa).toHaveBeenCalledWith(7, 'Error de digitación')
  })

  it('muestra el historial de versiones', async () => {
    listarActas.mockResolvedValue([
      { ...ACTA, idActa: 8, version: 2 },
      { ...ACTA, estado: 'ANULADA' },
    ])
    renderizarConSesion(<PanelActa idDocente={3} />)
    expect(await screen.findByText('Historial de versiones (1)')).toBeInTheDocument()
  })

  it('informa si no puede cargar', async () => {
    listarActas.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<PanelActa idDocente={3} />)
    expect(await screen.findByText('No fue posible cargar el acta')).toBeInTheDocument()
  })
})
