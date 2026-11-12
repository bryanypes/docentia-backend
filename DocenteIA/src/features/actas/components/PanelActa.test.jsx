import { screen, waitFor } from '@testing-library/react'
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

beforeEach(() => {
  vi.clearAllMocks()
  obtenerConfiguracionEvaluacion.mockResolvedValue({ umbralCompromisos: 3.8, rolCorrectorActas: 'VICERRECTOR' })
})

describe('PanelActa', () => {
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

  it('firmar exige la confirmación explícita', async () => {
    listarActas.mockResolvedValue([ACTA])
    firmarActa.mockResolvedValue(ACTA)
    renderizarConSesion(<PanelActa idDocente={3} />)
    const boton = await screen.findByRole('button', { name: 'Firmar acta' })
    expect(boton).toBeDisabled()
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(boton)
    expect(firmarActa).toHaveBeenCalledWith(7)
  })

  it('un doble clic en firmar envía una sola vez', async () => {
    listarActas.mockResolvedValue([ACTA])
    let terminar
    firmarActa.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('checkbox'))
    const boton = screen.getByRole('button', { name: 'Firmar acta' })
    await userEvent.dblClick(boton)
    expect(firmarActa).toHaveBeenCalledTimes(1)
    terminar(ACTA)
  })

  it('muestra el 409 de firma sin cita', async () => {
    listarActas.mockResolvedValue([ACTA])
    firmarActa.mockRejectedValue(new ApiError(409, 'El docente no tiene una cita agendada'))
    renderizarConSesion(<PanelActa idDocente={3} />)
    await userEvent.click(await screen.findByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Firmar acta' }))
    expect(await screen.findByText('El docente no tiene una cita agendada')).toBeInTheDocument()
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
